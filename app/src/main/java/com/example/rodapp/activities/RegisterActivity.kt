package com.example.rodapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.main.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val lastname: String
)

class RegisterActivity : AppCompatActivity() {

    private lateinit var textViewLoginLink: TextView
    private lateinit var editTextFullName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonGoogleSignIn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aunque el redirect apunta a LoginActivity, es buena práctica manejarlo si RegisterActivity pudiera ser destino
        SupabaseClient.client.handleDeeplinks(intent) {
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
            finish()
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Inicialización de vistas
        textViewLoginLink = findViewById(R.id.textViewLoginLink)
        editTextFullName = findViewById(R.id.editTextFullName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)

        // Configuración de insets para manejar el diseño de borde a borde
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonRegister.setOnClickListener {
            performRegistration()
        }

        buttonGoogleSignIn.setOnClickListener {
            performGoogleLogin()
        }

        textViewLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClient.client.handleDeeplinks(intent) {
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun performGoogleLogin() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Google, redirectUrl = "rodapp://login")
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Error al iniciar con Google: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun performRegistration() {
        val fullName = editTextFullName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        // Validaciones básicas locales
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor llena los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email no válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // Dividir nombre
        val nameParts = fullName.split(" ", limit = 2)
        val firstName = nameParts.getOrElse(0) { "" }
        val lastName = nameParts.getOrElse(1) { "" }

        lifecycleScope.launch {
            try {
                buttonRegister.isEnabled = false
                buttonRegister.text = "Registrando..."

                // 1. Registro en Supabase Auth
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id

                if (userId != null) {
                    // 2. Guardar en la tabla 'users'
                    val userProfile = UserProfile(id = userId, name = firstName, lastname = lastName)
                    SupabaseClient.client.postgrest.from("users").insert(userProfile)

                    Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Cuenta creada, por favor inicia sesión.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }

            } catch (e: Exception) {
                // Mapeo de errores para el registro
                val errorMsg = when {
                    e.message?.contains("User already registered", ignoreCase = true) == true -> 
                        "Este correo ya está registrado con otra cuenta"
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Error de conexión. Revisa tu internet"
                    else -> "No se pudo completar el registro. Intenta de nuevo."
                }
                Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                buttonRegister.isEnabled = true
                buttonRegister.text = "Registrarse"
            }
        }
    }
}
