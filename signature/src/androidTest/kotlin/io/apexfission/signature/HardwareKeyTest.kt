package io.apexfission.signature

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.apexfission.signature.protocol.Suite
import org.junit.*
import org.junit.runner.RunWith
import java.security.KeyFactory
import java.security.KeyStore
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties

/** Physical-device tests. Skips are explicit prerequisites, never proof of hardware enforcement. */
@RunWith(AndroidJUnit4::class)
class HardwareKeyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val keys = HardwareKeys(context)
    private val alias = "instrumented-${System.nanoTime()}"
    @After fun cleanup() { keys.delete(alias) }

    @Test fun selectedTeeHasPerUseBiometricPolicyAndChain() {
        assumeBiometric()
        val key = keys.generate(alias, Suite.ES256_TEE, ByteArray(32) { it.toByte() })
        Assert.assertEquals(Suite.ES256_TEE, key.suite)
        Assert.assertEquals(64, key.keyId.length)
        Assert.assertTrue(keys.certificateChain(key).isNotEmpty())
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = store.getKey(HardwareKeys.storageAlias(alias), null)
        val info = KeyFactory.getInstance("EC", "AndroidKeyStore").getKeySpec(privateKey, KeyInfo::class.java)
        Assert.assertEquals(KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT, info.securityLevel)
        Assert.assertTrue(info.isUserAuthenticationRequired)
        Assert.assertTrue(isPerUseAuthenticationDuration(info.userAuthenticationValidityDurationSeconds))
        Assert.assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, info.userAuthenticationType)
        Assert.assertTrue(info.isInvalidatedByBiometricEnrollment)
        try { keys.generate(alias, Suite.ES256_TEE, ByteArray(32)); Assert.fail("must not overwrite") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun strongBoxNeverFallsBack() {
        assumeBiometric()
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
            try { keys.generate(alias, Suite.ES256_STRONGBOX, ByteArray(32)); Assert.fail("must reject") }
            catch (_: HardwareUnavailableException) { }
        } else {
            // Resource exhaustion is a genuine failed test; do not retry with TEE.
            val key = keys.generate(alias, Suite.ES256_STRONGBOX, ByteArray(32))
            Assert.assertEquals(Suite.ES256_STRONGBOX, key.suite)
        }
    }
    private fun assumeBiometric() {
        Assume.assumeTrue("Physical enrolled BIOMETRIC_STRONG required", BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
    }
}
