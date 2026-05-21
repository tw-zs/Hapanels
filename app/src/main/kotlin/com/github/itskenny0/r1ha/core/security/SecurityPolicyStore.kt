package com.github.itskenny0.r1ha.core.security

import android.content.Context
import android.content.SharedPreferences
import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Snapshot of the user-configured network security policy.
 *
 * Kept separate from [com.github.itskenny0.r1ha.core.prefs.AppSettings] because the
 * OkHttpClient lazily built in [com.github.itskenny0.r1ha.AppGraph] needs to read the
 * pin list synchronously at process startup; threading that through the DataStore-backed
 * settings flow would force a suspending init or a runBlocking on the main thread.
 * SharedPreferences is synchronous on first read, has no schema migration story to
 * complicate, and is the right tool for this one config blob.
 */
data class SecurityPolicy(
    /** When true and [sha256Pins] is non-empty, OkHttp enforces SPKI pinning against
     *  the server URL host. When false, pins are stored but not applied: users can
     *  pre-stage pins before flipping the toggle. */
    val tlsPinningEnabled: Boolean = false,
    /** SHA-256 SPKI hashes of public keys the server is allowed to present. Stored
     *  as the bare base64 (`sha256/...` is appended by the consumer). One pin per
     *  list entry; the user adds the backup pin too so a normal cert rotation
     *  doesn't strand them. */
    val sha256Pins: List<String> = emptyList(),
    /** When true and [mtlsKeystorePath] points to a readable PKCS12, OkHttp presents
     *  the client cert during TLS handshake. mTLS is opt-in because misconfiguring
     *  it (wrong cert, wrong password, expired cert) breaks every request to the
     *  server until corrected; the toggle lets users stage a known-good cert
     *  before flipping. */
    val mtlsEnabled: Boolean = false,
    /** Absolute path inside the app's private filesDir to the imported PKCS12
     *  blob. Stored as a path (not the bytes inline) because SharedPreferences is
     *  a key-value store and embedding ~2 KB binary as base64 every commit is
     *  wasteful. Null = no cert imported yet. */
    val mtlsKeystorePath: String? = null,
    /** Password for [mtlsKeystorePath]. Stored in plain SharedPreferences for
     *  simplicity — the app sandbox already isolates this file from other apps
     *  on non-rooted devices, and pairing it with the .p12 file (which holds the
     *  actual key material) means each in isolation is useless. A strong .p12
     *  password is still the right move because export-from-this-device-then-
     *  uninstall is the practical threat. */
    val mtlsKeystorePassword: String = "",
)

/**
 * Synchronous SharedPreferences-backed store for [SecurityPolicy].
 *
 * Reads ([current]) never block on disk past the first SharedPreferences load, which
 * happens at app start. Writes commit synchronously via `commit()` so a settings
 * change followed by an immediate `okHttp` rebuild can't see stale pins.
 *
 * Exposes a [flow] for UI observation; the flow re-emits whenever [update] commits.
 * The flow is not multicast: each collector gets its own; cheap enough since the
 * settings screen has one observer at a time.
 */
class SecurityPolicyStore private constructor(
    private val prefs: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    private val changes = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).also { it.tryEmit(Unit) }

    /** Sync snapshot of the current policy. Safe to call from any thread. */
    fun current(): SecurityPolicy {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val raw = prefs.getString(KEY_PINS, null).orEmpty()
        val pins = raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        return SecurityPolicy(
            tlsPinningEnabled = enabled,
            sha256Pins = pins,
            mtlsEnabled = prefs.getBoolean(KEY_MTLS_ENABLED, false),
            mtlsKeystorePath = prefs.getString(KEY_MTLS_PATH, null)?.takeIf { it.isNotBlank() },
            mtlsKeystorePassword = prefs.getString(KEY_MTLS_PASS, "").orEmpty(),
        )
    }

    /** Reactive view of the policy. Emits on every successful commit; replays the
     *  current value to new collectors so a settings screen entering recomposition
     *  doesn't have to wait for the next write to render. */
    val flow: Flow<SecurityPolicy> = changes
        .onStart { /* MutableSharedFlow.replay = 1 seeds the initial value */ }
        .map { current() }

    /** Apply [transform] to the current policy and persist the result synchronously.
     *  Returns true on a successful commit; on failure we log and return false so
     *  the UI can surface a toast without forcing every caller to wrap. */
    fun update(transform: (SecurityPolicy) -> SecurityPolicy): Boolean {
        val next = transform(current())
        val ok = prefs.edit()
            .putBoolean(KEY_ENABLED, next.tlsPinningEnabled)
            .putString(KEY_PINS, next.sha256Pins.joinToString("\n"))
            .putBoolean(KEY_MTLS_ENABLED, next.mtlsEnabled)
            .putString(KEY_MTLS_PATH, next.mtlsKeystorePath.orEmpty())
            .putString(KEY_MTLS_PASS, next.mtlsKeystorePassword)
            .commit()
        if (!ok) {
            R1Log.w("SecurityPolicyStore", "commit failed for next=$next")
        }
        changes.tryEmit(Unit)
        return ok
    }

    companion object {
        private const val PREFS_NAME = "r1ha_security"
        private const val KEY_ENABLED = "tls.pinning.enabled"
        private const val KEY_PINS = "tls.pinning.pins"
        private const val KEY_MTLS_ENABLED = "tls.mtls.enabled"
        private const val KEY_MTLS_PATH = "tls.mtls.path"
        private const val KEY_MTLS_PASS = "tls.mtls.password"

        /**
         * Validate a candidate pin string. A pin is acceptable when it is the
         * base64 of a 32-byte SHA-256 digest (44 chars after padding, or 43
         * without). The user can paste a value with or without the `sha256/`
         * prefix; we strip it. Returns the canonical bare-base64 form, or null
         * when the input doesn't parse to 32 bytes.
         */
        fun normalisePin(raw: String): String? {
            val stripped = raw.trim().removePrefix("sha256/")
            if (stripped.isBlank()) return null
            return runCatching {
                // Standard base64 with padding. Pin is the SHA-256 of the SPKI
                // (a fixed-size 32-byte digest), so the decoded length must be
                // exactly 32 — anything else is a malformed paste.
                val decoded = android.util.Base64.decode(stripped, android.util.Base64.DEFAULT)
                if (decoded.size != 32) null else stripped
            }.getOrNull()
        }
    }
}
