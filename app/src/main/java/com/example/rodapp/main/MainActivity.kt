package com.example.rodapp.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.rodapp.R
import com.example.rodapp.databinding.ActivityMainBinding
import com.example.rodapp.main.admin.AdminFragment
import com.example.rodapp.main.perfil.PerfilFragment
import com.example.rodapp.main.productos.CatalogoFragment
import com.example.rodapp.main.productos.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste para que el contenido no se solape con la barra de estado y de navegación
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        // Quitar el título por defecto para usar el nuestro personalizado
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configuración del Drawer (Menú lateral)
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.inicio, R.string.inicio
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navegación Inicial
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.navView.setCheckedItem(R.id.nav_home)
            binding.bottomNav.selectedItemId = R.id.nav_home
        }

        // Listener para el Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_catalogo -> CatalogoFragment()
                R.id.nav_perfil -> PerfilFragment()
                R.id.nav_admin -> AdminFragment()
                else -> null
            }

            fragment?.let {
                replaceFragment(it)
                // Sincronizar el menú lateral cuando se pulsa el menú inferior
                binding.navView.setCheckedItem(item.itemId)
                true
            } ?: false
        }

        // Listener para el Navigation Drawer (Menú lateral)
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    finish()
                }
                else -> {
                    // Al cambiar el selectedItemId del BottomNav, se dispara su listener de arriba
                    // Esto evita duplicar la lógica de replaceFragment y mantiene sincronía
                    binding.bottomNav.selectedItemId = item.itemId
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}