package com.example.rodapp

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityManager {

    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD = "user_password"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun saveCredentials(context: Context, email: String, password: String?) {
        val editor = getEncryptedPrefs(context).edit()
            .putString(KEY_USER_EMAIL, email)
        
        if (password != null) {
            editor.putString(KEY_USER_PASSWORD, password)
        } else {
            editor.remove(KEY_USER_PASSWORD)
        }
        editor.apply()
    }

    fun getCredentials(context: Context): Pair<String?, String?> {
        val prefs = getEncryptedPrefs(context)
        return Pair(prefs.getString(KEY_USER_EMAIL, null), prefs.getString(KEY_USER_PASSWORD, null))
    }

    fun clearCredentials(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASSWORD)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación biométrica",
        subtitle: String = "Ingresa con tu huella digital",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Fallo silencioso, el usuario puede reintentar
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
