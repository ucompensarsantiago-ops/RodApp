package com.example.rodapp.main.perfil

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.rodapp.R
import com.example.rodapp.SupabaseClient
import com.example.rodapp.activities.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class PerfilFragment : Fragment() {

    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        // Inicializar vistas
        editTextName = view.findViewById(R.id.editTextProfileName)
        editTextLastName = view.findViewById(R.id.editTextProfileLastName)
        editTextEmail = view.findViewById(R.id.editTextProfileEmail)
        editTextPassword = view.findViewById(R.id.editTextProfilePassword)
        editTextConfirmPassword = view.findViewById(R.id.editTextProfileConfirmPassword)
        buttonSave = view.findViewById(R.id.buttonSaveProfile)

        // Cargar datos actuales
        loadUserProfile()

        buttonSave.setOnClickListener {
            saveChanges()
        }

        return view
    }

    private fun loadUserProfile() {
        val user = SupabaseClient.client.auth.currentUserOrNull()
        if (user == null) {
            Toast.makeText(requireContext(), "No hay sesión activa", Toast.LENGTH_SHORT).show()
            return
        }

        editTextEmail.setText(user.email)

        lifecycleScope.launch {
            try {
                // Consultar datos de la tabla 'users'
                val profile = SupabaseClient.client.postgrest.from("users")
                    .select {
                        filter {
                            eq("id", user.id)
                        }
                    }.decodeSingle<UserProfile>()

                editTextName.setText(profile.name)
                editTextLastName.setText(profile.lastname)

            } catch (e: Exception) {
                // Si no hay perfil aún, simplemente dejamos los campos vacíos
                e.printStackTrace()
            }
        }
    }

    private fun saveChanges() {
        val name = editTextName.text.toString().trim()
        val lastName = editTextLastName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString()
        val confirmPassword = editTextConfirmPassword.text.toString()

        val user = SupabaseClient.client.auth.currentUserOrNull() ?: return

        if (name.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Nombre, apellido y correo son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Correo no válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isNotEmpty()) {
            if (password.length < 6) {
                Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return
            }
            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return
            }
        }

        lifecycleScope.launch {
            try {
                buttonSave.isEnabled = false
                buttonSave.text = "Guardando..."

                // 1. Actualizar tabla 'users'
                val updatedProfile = UserProfile(id = user.id, name = name, lastname = lastName)
                SupabaseClient.client.postgrest.from("users").upsert(updatedProfile)

                // 2. Actualizar Email si cambió
                if (email != user.email) {
                    SupabaseClient.client.auth.updateUser {
                        this.email = email
                    }
                    Toast.makeText(requireContext(), "Correo actualizado (verifica tu bandeja)", Toast.LENGTH_SHORT).show()
                }

                // 3. Actualizar Password si se ingresó una nueva
                if (password.isNotEmpty()) {
                    SupabaseClient.client.auth.updateUser {
                        this.password = password
                    }
                    Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                }

                Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            } finally {
                buttonSave.isEnabled = true
                buttonSave.text = "Guardar Cambios"
            }
        }
    }
}
