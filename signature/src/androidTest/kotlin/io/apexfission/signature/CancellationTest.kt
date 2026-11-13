package io.apexfission.signature

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.apexfission.signature.protocol.*
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

public class PromptTestActivity : FragmentActivity()

/** Does not simulate successful authentication. A real enrolled device is required. */
@RunWith(AndroidJUnit4::class)
class CancellationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val keys = HardwareKeys(context)
    private val alias = "cancel-${System.nanoTime()}"
    @After fun cleanup() { keys.delete(alias) }

    @Test fun explicitCancelCompletesOnceAndIgnoresLateCallbacks() { runCancellation(background = false) }
    @Test fun stoppedActivityCancelsPendingAuthentication() { runCancellation(background = true) }

    private fun runCancellation(background: Boolean) {
        Assume.assumeTrue("Enrolled strong biometric required", BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
        val key = keys.generate(alias, Suite.ES256_TEE, ByteArray(32))
        val statement = Statement(Operation.REGISTER, key.suite, "device-cancel", ByteArray(32), "https://issuer.example", "device-test", key.keyId)
        val callbacks = AtomicInteger()
        val cancelled = AtomicInteger()
        val intent = Intent(InstrumentationRegistry.getInstrumentation().context, PromptTestActivity::class.java)
        ActivityScenario.launch<PromptTestActivity>(intent).use { scenario ->
            lateinit var signer: BiometricSigner
            scenario.onActivity { activity ->
                signer = BiometricSigner(activity, keys)
                signer.sign(key, statement, PromptText("Device cancellation test")) { result ->
                    callbacks.incrementAndGet()
                    if (result.exceptionOrNull() is CancellationException) cancelled.incrementAndGet()
                    Assert.assertTrue("Cancellation must not deliver a signature", result.isFailure)
                }
                if (!background) { signer.cancel(); signer.cancel() }
            }
            if (background) scenario.moveToState(Lifecycle.State.CREATED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Assert.assertEquals(1, callbacks.get())
            Assert.assertEquals(1, cancelled.get())
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Assert.assertEquals(1, callbacks.get())
    }
}
