package com.example.rodapp.main

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.activities.LoginActivity
import com.example.rodapp.databinding.ActivityMainBinding
import com.example.rodapp.main.admin.AdminFragment
import com.example.rodapp.main.perfil.PerfilFragment
import com.example.rodapp.main.productos.CarritoFragment
import com.example.rodapp.main.productos.CatalogoFragment
import com.example.rodapp.main.productos.HomeFragment
import com.example.rodapp.main.productos.fragment_favoritos
import com.example.rodapp.models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste para que el contenido no se solape con la barra de estado y de navegación
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Aplicamos margen superior a la toolbar en lugar de padding interno
            val lp = binding.toolbar.layoutParams as android.view.ViewGroup.MarginLayoutParams
            lp.topMargin = systemBars.top
            binding.toolbar.layoutParams = lp

            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Forzar que aparezca el botón home
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configuración del Drawer (Menú lateral)
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.inicio, R.string.inicio
        )
        // Tintar el icono de la hamburguesa de blanco para que sea visible
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white, theme)
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
                R.id.nav_favoritos -> fragment_favoritos()
                R.id.nav_carrito -> CarritoFragment()
                else -> null
            }

            fragment?.let {
                replaceFragment(it)
                // Sincronizar el menú lateral cuando se pulsa el menú inferior
                binding.navView.setCheckedItem(item.itemId)
                true
            } ?: false
        }

        loadUserRoleAndConfigureMenu()

        // Listener para el Navigation Drawer (Menú lateral)
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    performLogout()
                }
                R.id.nav_perfil -> {
                    replaceFragment(PerfilFragment())
                    uncheckBottomNav()
                }
                R.id.nav_admin -> {
                    replaceFragment(AdminFragment())
                    uncheckBottomNav()
                }
                else -> {
                    // Para Home, Catalogo, Favoritos y Carrito, usamos la sincronía con el BottomNav
                    binding.bottomNav.selectedItemId = item.itemId
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadUserRoleAndConfigureMenu() {
        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val profile = SupabaseClient.client.postgrest.from("users")
                    .select { filter { eq("id", userId) } }
                    .decodeList<UserProfile>().firstOrNull()
                if (profile?.role == "admin") {
                    binding.navView.menu.findItem(R.id.nav_admin)?.isVisible = true
                }
                refreshToolbarAvatar(profile?.urlPhoto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshToolbarAvatar(photoUrl: String? = null) {
        val avatarView = binding.toolbar.findViewById<ImageView>(R.id.imageViewToolbarAvatar) ?: return
        val url = photoUrl ?: SupabaseClient.client.auth.currentUserOrNull()?.let { null }
        avatarView.load(url) {
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_person_placeholder)
            error(R.drawable.ic_person_placeholder)
            fallback(R.drawable.ic_person_placeholder)
        }
        avatarView.setOnClickListener {
            replaceFragment(PerfilFragment())
            uncheckBottomNav()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                // Si falla la conexión, igual limpiamos la sesión local
                e.printStackTrace()
            }
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun uncheckBottomNav() {
        binding.bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNav.menu.size()) {
            binding.bottomNav.menu.getItem(i).isChecked = false
        }
        binding.bottomNav.menu.setGroupCheckable(0, true, true)
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
