package com.github.itskenny0.r1ha.core.security

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurityPolicyStoreTest {

    @Test fun defaultIsAllOff() {
        val store = SecurityPolicyStore(ApplicationProvider.getApplicationContext())
        val policy = store.current()
        assertThat(policy.tlsPinningEnabled).isFalse()
        assertThat(policy.sha256Pins).isEmpty()
        assertThat(policy.mtlsEnabled).isFalse()
        assertThat(policy.mtlsKeystorePath).isNull()
        assertThat(policy.mtlsKeystorePassword).isEqualTo("")
    }

    @Test fun updateRoundTrip() {
        val store = SecurityPolicyStore(ApplicationProvider.getApplicationContext())
        store.update {
            it.copy(
                tlsPinningEnabled = true,
                sha256Pins = listOf("AAAA1111", "BBBB2222"),
                mtlsEnabled = true,
                mtlsKeystorePath = "/data/data/x/files/mtls/client.p12",
                mtlsKeystorePassword = "hunter2",
            )
        }
        val current = store.current()
        assertThat(current.tlsPinningEnabled).isTrue()
        assertThat(current.sha256Pins).containsExactly("AAAA1111", "BBBB2222").inOrder()
        assertThat(current.mtlsEnabled).isTrue()
        assertThat(current.mtlsKeystorePath).isEqualTo("/data/data/x/files/mtls/client.p12")
        assertThat(current.mtlsKeystorePassword).isEqualTo("hunter2")
    }

    @Test fun pinNormalise_acceptsCanonicalForm() {
        // Random valid 32-byte SHA-256 in base64 — generated once for the test.
        val valid = "L9f0fI4LUFv0H7+gM7tQ7VhJ4dEh5JkD5UQzlqkLqYU="
        assertThat(SecurityPolicyStore.normalisePin(valid)).isEqualTo(valid)
    }

    @Test fun pinNormalise_stripsSha256Prefix() {
        val valid = "L9f0fI4LUFv0H7+gM7tQ7VhJ4dEh5JkD5UQzlqkLqYU="
        assertThat(SecurityPolicyStore.normalisePin("sha256/$valid")).isEqualTo(valid)
    }

    @Test fun pinNormalise_rejectsShortDigest() {
        // Only 16 bytes base64-encoded — clearly not a SHA-256.
        val tooShort = "AAECAwQFBgcICQoLDA0ODw=="
        assertThat(SecurityPolicyStore.normalisePin(tooShort)).isNull()
    }

    @Test fun pinNormalise_rejectsGarbage() {
        assertThat(SecurityPolicyStore.normalisePin("not-base64-at-all!!!")).isNull()
        assertThat(SecurityPolicyStore.normalisePin("")).isNull()
        assertThat(SecurityPolicyStore.normalisePin("   ")).isNull()
    }
}
