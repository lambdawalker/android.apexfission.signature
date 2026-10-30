package io.apexfission.signature

import android.os.Looper
import androidx.annotation.MainThread
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.apexfission.signature.protocol.Statement
import java.security.Signature
import java.util.concurrent.CancellationException

public data class PromptText(public val title: String, public val subtitle: String? = null, public val cancel: String = "Cancel") {
    init { require(title.isNotBlank() && cancel.isNotBlank()) { "Prompt title and cancel label must not be blank" } }
}

/** Main-thread activity-scoped signer. Backgrounding/rotation cancels; nothing auto-resumes. */
public class BiometricSigner(
    private val activity: FragmentActivity,
    private val keys: HardwareKeys,
) : DefaultLifecycleObserver, AutoCloseable {
    private class Pending(val signature: Signature, val bytes: ByteArray, val callback: (Result<ByteArray>) -> Unit) {
        var prompt: BiometricPrompt? = null
    }
    private var pending: Pending? = null
    private var closed = false
    init { mainThread(); activity.lifecycle.addObserver(this) }

    @MainThread
    public fun sign(key: HardwareKey, statement: Statement, prompt: PromptText, callback: (Result<ByteArray>) -> Unit) {
        mainThread()
        check(!closed) { "Signer is closed" }
        check(pending == null) { "A biometric operation is already pending" }
        check(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { "Activity must be resumed" }
        val operation = try {
            require(statement.keyId == key.keyId && statement.suite == key.suite) { "Statement does not bind selected key and suite" }
            Pending(keys.signingOperation(key), statement.encode(), callback)
        } catch (failure: Exception) { callback(Result.failure(failure)); return }
        pending = operation
        try {
            val biometric = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish(operation, Result.failure(AuthenticationException(errorCode, errString.toString())))
                }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (pending !== operation) return
                    val signed = runCatching {
                        check(result.cryptoObject?.signature === operation.signature) { "Authenticated CryptoObject missing or substituted" }
                        operation.signature.update(operation.bytes)
                        operation.signature.sign()
                    }
                    finish(operation, signed)
                }
                // Nonmatching biometric attempts are nonterminal; only the OS decides lockout.
            })
            operation.prompt = biometric
            val info = BiometricPrompt.PromptInfo.Builder().setTitle(prompt.title)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(prompt.cancel).apply { prompt.subtitle?.let { setSubtitle(it) } }.build()
            biometric.authenticate(info, BiometricPrompt.CryptoObject(operation.signature))
        } catch (failure: Exception) { finish(operation, Result.failure(failure)) }
    }

    private fun finish(operation: Pending, result: Result<ByteArray>) {
        if (pending !== operation) return
        pending = null // Clear before callback: reentrant host callbacks cannot complete this operation twice.
        operation.callback(result)
    }

    @MainThread
    public fun cancel() {
        mainThread()
        val operation = pending ?: return
        pending = null // Ignore late OS callbacks, including success after explicit cancellation.
        try { operation.prompt?.cancelAuthentication() }
        finally { operation.callback(Result.failure(CancellationException("Biometric signing cancelled"))) }
    }

    override fun onStop(owner: LifecycleOwner) { cancel() }
    override fun onDestroy(owner: LifecycleOwner) { close() }

    @MainThread
    override fun close() {
        mainThread()
        if (closed) return
        closed = true
        activity.lifecycle.removeObserver(this)
        cancel()
    }
    private fun mainThread() { check(Looper.myLooper() == Looper.getMainLooper()) { "BiometricSigner requires the main thread" } }
}
