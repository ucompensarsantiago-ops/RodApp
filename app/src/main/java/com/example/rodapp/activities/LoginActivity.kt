package com.example.rodapp.activities

import android.content.Intent
import android.os.Bundle
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
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewRegisterLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Inicializar vistas
        editTextEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar botón de Login para validar con Supabase
        buttonLogin.setOnClickListener {
            performLogin()
        }

        // Configurar botón de Google para ir a MainActivity (simulado por ahora)
        val buttonGoogleSignIn = findViewById<Button>(R.id.buttonGoogleSignIn)
        buttonGoogleSignIn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        textViewRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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

                // Iniciar sesión con Supabase Auth
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                Toast.makeText(this@LoginActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                // Mapeo de errores comunes para el usuario
                val errorMsg = when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> 
                        "Correo o contraseña incorrectos"
                    e.message?.contains("Email not confirmed", ignoreCase = true) == true -> 
                        "Por favor, confirma tu correo electrónico"
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Error de conexión. Revisa tu internet"
                    else -> "No se pudo iniciar sesión. Verifica tus datos."
                }
                Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                buttonLogin.isEnabled = true
                buttonLogin.text = "Ingresar"
            }
        }
    }
}
