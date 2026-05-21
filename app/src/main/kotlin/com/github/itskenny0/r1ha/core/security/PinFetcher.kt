package com.github.itskenny0.r1ha.core.security

import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

/**
 * Probe a remote HTTPS endpoint and extract the SHA-256 SPKI hash of every cert
 * in the chain. The user can then pin one (or multiple, for cert rotation
 * resilience) without grinding through `openssl s_client | openssl x509 ...`
 * pipelines by hand.
 *
 * Implementation: makes one HEAD request against the server URL using an
 * OkHttpClient WITHOUT pinning (a fresh ephemeral client), then on success
 * walks `response.handshake().peerCertificates`. The server's certificate is
 * the first entry; any intermediate CAs follow. We surface each one with a
 * label so the user can pick the leaf or pin the intermediate (more durable
 * across cert rotations).
 */
object PinFetcher {

    /** One certificate's pin info. */
    data class CertPin(
        /** Subject CN or first SAN — to help the user tell the leaf from the CA. */
        val label: String,
        /** Bare base64 of SHA-256(subjectPublicKeyInfo). Append "sha256/" before
         *  handing to OkHttp's CertificatePinner. */
        val sha256Base64: String,
    )

    /**
     * Probe [serverUrl] and return the SHA-256 SPKI pins for every certificate
     * the server presents. Returns failure for network / TLS errors.
     */
    suspend fun probe(serverUrl: String): Result<List<CertPin>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = OkHttpClient.Builder().build()
            val cleaned = if (serverUrl.startsWith("http")) serverUrl else "https://$serverUrl"
            val req = Request.Builder().url(cleaned).head().build()
            client.newCall(req).execute().use { response ->
                val handshake = response.handshake
                    ?: error("no TLS handshake (server is plain HTTP?)")
                handshake.peerCertificates.mapIndexed { idx, cert ->
                    val x509 = cert as? java.security.cert.X509Certificate
                        ?: return@mapIndexed CertPin("cert[$idx]", "")
                    val label = x509.subjectX500Principal.name.let { dn ->
                        dn.split(',').firstOrNull { it.trim().startsWith("CN=") }
                            ?.removePrefix("CN=")?.removePrefix("cn=")
                            ?: dn.take(40)
                    }
                    val spki = x509.publicKey.encoded
                    val digest = MessageDigest.getInstance("SHA-256").digest(spki)
                    val base64 = android.util.Base64.encodeToString(
                        digest,
                        android.util.Base64.NO_WRAP,
                    )
                    CertPin(label = label, sha256Base64 = base64)
                }.filter { it.sha256Base64.isNotBlank() }
            }
        }.onFailure { t ->
            R1Log.w("PinFetcher", "probe '$serverUrl' failed: ${t.message}")
        }
    }
}
