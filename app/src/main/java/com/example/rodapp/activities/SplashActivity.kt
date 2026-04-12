package com.example.rodapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.main.MainActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        
        // Ajuste de márgenes para las barras del sistema (Edge-to-Edge)
        val mainView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Usamos lifecycleScope para manejar el tiempo de espera y la verificación de sesión
        lifecycleScope.launch {
            delay(3000) // Esperar 3 segundos

            // Verificar si hay una sesión activa en Supabase
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()

            if (currentUser != null) {
                // Si ya inició sesión, vamos directo al Main
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Si no hay sesión, vamos al Onboarding
                startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
            }
            finish()
        }
    }
}