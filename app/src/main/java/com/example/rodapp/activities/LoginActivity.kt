package com.example.rodapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rodapp.R
import com.example.rodapp.SecurityManager
import com.example.rodapp.SupabaseClient
import com.example.rodapp.main.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var imageButtonBiometric: ImageButton
    private lateinit var textViewRegisterLink: TextView

    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("SUPABASE_AUTH", "onCreate Intent Data: ${intent?.dataString}")

        // 1. Observar cambios de sesión de forma reactiva
        observeSessionStatus()

        // 2. Manejar deep link inicial
        handleAuthRedirect(intent)

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Inicializar vistas
        editTextEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        imageButtonBiometric = findViewById(R.id.imageButtonBiometric)
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar biometría
        if (SecurityManager.isBiometricEnabled(this)) {
            imageButtonBiometric.visibility = android.view.View.VISIBLE
            if (intent?.data == null) {
                showBiometricLogin()
            }
        } else {
            imageButtonBiometric.visibility = android.view.View.GONE
        }

        imageButtonBiometric.setOnClickListener {
            showBiometricLogin()
        }

        buttonLogin.setOnClickListener {
            performLogin()
        }

        val buttonGoogleSignIn = findViewById<Button>(R.id.buttonGoogleSignIn)
        buttonGoogleSignIn.setOnClickListener {
            performGoogleLogin()
        }

        textViewRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeSessionStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SupabaseClient.client.auth.sessionStatus.collect { status ->
                    Log.d("SUPABASE_AUTH", "Estado de sesión: $status")
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            if (!isNavigating) {
                                // isNew es true si el usuario acaba de iniciar sesión (Email, Google, etc.)
                                // isNew es false si la sesión se restauró automáticamente al abrir la app
                                if (status.isNew) {
                                    isNavigating = true
                                    Log.d("SUPABASE_AUTH", "Login exitoso (${status.source}). Navegando...")
                                    
                                    // Si es login exitoso y no tiene huella, sugerimos activarla
                                    if (!SecurityManager.isBiometricEnabled(this@LoginActivity)) {
                                        val user = SupabaseClient.client.auth.currentUserOrNull()
                                        showEnableBiometricDialog(user?.email ?: "", null)
                                    } else {
                                        navigateToMain()
                                    }
                                } else {
                                    // Sesión antigua: Solo entramos si la huella NO está habilitada
                                    if (!SecurityManager.isBiometricEnabled(this@LoginActivity)) {
                                        isNavigating = true
                                        navigateToMain()
                                    } else {
                                        Log.d("SUPABASE_AUTH", "Sesión antigua detectada, esperando huella.")
                                    }
                                }
                            }
                        }
                        is SessionStatus.RefreshFailure -> {
                            Toast.makeText(this@LoginActivity, "Error de red", Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) 
        Log.d("SUPABASE_AUTH", "onNewIntent Data: ${intent.dataString}")
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.dataString
        if (data != null && (data.contains("#access_token=") || data.contains("error_description="))) {
            Log.d("SUPABASE_AUTH", "Detectado retorno de autenticación. Procesando...")
            try {
                SupabaseClient.client.handleDeeplinks(intent)
            } catch (e: Exception) {
                Log.e("SUPABASE_AUTH", "Error al procesar deep link", e)
                Toast.makeText(this, "Error de retorno: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performGoogleLogin() {
        lifecycleScope.launch {
            try {
                Log.d("SUPABASE_AUTH", "Iniciando flujo de Google...")
                // Forzamos el selector de cuentas con queryParams
                SupabaseClient.client.auth.signInWith(Google, redirectUrl = "rodapp://login") {
                    queryParams["prompt"] = "select_account"
                }
            } catch (e: Exception) {
                Log.e("SUPABASE_AUTH", "Error en signInWith Google", e)
                Toast.makeText(this@LoginActivity, "Error al abrir Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                buttonLogin.isEnabled = false
                buttonLogin.text = "Iniciando sesión..."

                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                Toast.makeText(this@LoginActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                
                if (SecurityManager.isBiometricAvailable(this@LoginActivity) && 
                    !SecurityManager.isBiometricEnabled(this@LoginActivity)) {
                    // Si el login fue exitoso, guardamos el correo para la huella
                    showEnableBiometricDialog(email, password)
                } else {
                    navigateToMain()
                }

            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> 
                        "Correo o contraseña incorrectos"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
            } finally {
                buttonLogin.isEnabled = true
                buttonLogin.text = "Ingresar"
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showBiometricLogin() {
        val (email, password) = SecurityManager.getCredentials(this)
        if (email != null) {
            SecurityManager.showBiometricPrompt(
                activity = this,
                onSuccess = {
                    if (password != null && password.isNotEmpty()) {
                        // Flujo de Email: Tenemos contraseña
                        editTextEmail.setText(email)
                        editTextPassword.setText(password)
                        performLogin()
                    } else {
                        // Flujo de Google: No hay contraseña guardada.
                        Log.d("SUPABASE_AUTH", "Acceso concedido por huella (Google)")
                        if (SupabaseClient.client.auth.currentUserOrNull() != null) {
                            isNavigating = true
                            navigateToMain()
                        } else {
                            // La sesión expiró — pedir que vuelva a autenticarse con Google
                            Toast.makeText(
                                this,
                                "Tu sesión ha expirado. Por favor inicia sesión con Google nuevamente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onError = { error ->
                    Toast.makeText(this, "Error de huella: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showEnableBiometricDialog(email: String, password: String?) {
        AlertDialog.Builder(this)
            .setTitle("Activar Huella Digital")
            .setMessage("¿Deseas usar tu huella digital para acceder más rápido la próxima vez?")
            .setPositiveButton("Sí, activar") { _, _ ->
                SecurityManager.showBiometricPrompt(
                    activity = this,
                    onSuccess = {
                        SecurityManager.saveCredentials(this, email, password)
                        SecurityManager.setBiometricEnabled(this, true)
                        Toast.makeText(this, "Huella activada correctamente", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    },
                    onError = { error ->
                        navigateToMain()
                    }
                )
            }
            .setNegativeButton("Ahora no") { _, _ ->
                navigateToMain()
            }
            .setCancelable(false)
            .show()
    }
}
