package com.example.rodapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.rodapp.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        
        // Ajuste de márgenes para las barras del sistema (Edge-to-Edge)
        // Se usa tv_bienvenida como referencia si existe en activity_splash.xml
        val mainView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configuración del temporizador de 3 segundos (3000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            // Navegamos a OnboardingActivity como primer paso después del Splash
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            // Finalizamos esta actividad para que no se pueda regresar a ella
            finish()
        }, 3000)
    }
}