package com.example.rodapp.main.perfil

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.rodapp.R
import com.example.rodapp.SecurityManager
import com.example.rodapp.SupabaseClient
import com.example.rodapp.main.MainActivity
import com.example.rodapp.models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.File

@Serializable
private data class PhotoUrlUpdate(
    val id: String,
    @SerialName("url_photo") val urlPhoto: String
)

class PerfilFragment : Fragment() {

    private lateinit var textViewFullName: TextView
    private lateinit var textViewRole: TextView
    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonSave: Button
    private lateinit var layoutBiometric: LinearLayout
    private lateinit var switchBiometric: SwitchCompat
    private lateinit var imageViewProfilePhoto: ImageView
    private lateinit var frameLayoutProfilePhoto: FrameLayout

    private var isInternalChange = false
    private var cameraImageUri: Uri? = null

    // Contrato personalizado que otorga permisos URI explícitos (requerido desde Android 18)
    private val takePictureLauncher = registerForActivityResult(
        object : ActivityResultContract<Uri, Boolean>() {
            override fun createIntent(context: Context, input: Uri): Intent {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, input)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                context.packageManager
                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .forEach {
                        context.grantUriPermission(
                            it.activityInfo.packageName,
                            input,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                return intent
            }
            override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
                resultCode == Activity.RESULT_OK
        }
    ) { success ->
        Log.d("PerfilFragment", "Camera result: success=$success, uri=$cameraImageUri")
        if (success) {
            val uri = cameraImageUri
            if (uri != null) uploadPhoto(uri)
            else Log.e("PerfilFragment", "cameraImageUri is null after success")
        } else {
            Toast.makeText(requireContext(), "No se tomó ninguna foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadPhoto(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera()
        else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openGallery()
        else Toast.makeText(requireContext(), "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        textViewFullName = view.findViewById(R.id.textViewProfileFullName)
        textViewRole = view.findViewById(R.id.textViewProfileRole)
        editTextName = view.findViewById(R.id.editTextProfileName)
        editTextLastName = view.findViewById(R.id.editTextProfileLastName)
        editTextEmail = view.findViewById(R.id.editTextProfileEmail)
        editTextPassword = view.findViewById(R.id.editTextProfilePassword)
        editTextConfirmPassword = view.findViewById(R.id.editTextProfileConfirmPassword)
        buttonSave = view.findViewById(R.id.buttonSaveProfile)
        layoutBiometric = view.findViewById(R.id.layoutBiometricToggle)
        switchBiometric = view.findViewById(R.id.switchBiometric)
        imageViewProfilePhoto = view.findViewById(R.id.imageViewProfilePhoto)
        frameLayoutProfilePhoto = view.findViewById(R.id.frameLayoutProfilePhoto)

        setupBiometricUI()
        loadUserProfile()

        buttonSave.setOnClickListener { saveChanges() }
        frameLayoutProfilePhoto.setOnClickListener { showPhotoOptions() }

        return view
    }

    // -------------------------------------------------------------------------
    // Foto de perfil
    // -------------------------------------------------------------------------

    private fun showPhotoOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Foto de perfil")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                when (which) {
                    0 -> checkCameraAndOpen()
                    1 -> checkGalleryAndOpen()
                }
            }
            .show()
    }

    private fun checkCameraAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Permiso de cámara")
                    .setMessage("Necesitamos acceso a la cámara para tomar tu foto de perfil.")
                    .setPositiveButton("Permitir") { _, _ -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkGalleryAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> openGallery()
            shouldShowRequestPermissionRationale(permission) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Permiso de galería")
                    .setMessage("Necesitamos acceso a tu galería para seleccionar una foto de perfil.")
                    .setPositiveButton("Permitir") { _, _ -> galleryPermissionLauncher.launch(permission) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> galleryPermissionLauncher.launch(permission)
        }
    }

    private fun openCamera() {
        val imagesDir = File(requireContext().cacheDir, "images").also { it.mkdirs() }
        val imageFile = File(imagesDir, "profile_temp.jpg")
        // Borrar archivo previo para evitar lecturas de caché corrupta
        if (imageFile.exists()) imageFile.delete()
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        Log.d("PerfilFragment", "Launching camera with URI: $uri")
        takePictureLauncher.launch(uri)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadPhoto(uri: Uri) {
        frameLayoutProfilePhoto.isEnabled = false

        lifecycleScope.launch {
            try {
                // La sesión puede estar cargándose del disco — esperar si es necesario
                var user = SupabaseClient.client.auth.currentUserOrNull()
                if (user == null) {
                    delay(800)
                    user = SupabaseClient.client.auth.currentUserOrNull()
                }
                if (user == null) {
                    Log.e("PerfilFragment", "No active session for upload")
                    Toast.makeText(requireContext(), "Sesión no disponible. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                Log.d("PerfilFragment", "Starting upload for uri=$uri")
                val bytes = withContext(Dispatchers.IO) { compressImage(uri) }
                Log.d("PerfilFragment", "Compressed image: ${bytes.size} bytes")

                val path = "${user.id}/profile.jpg"
                Log.d("PerfilFragment", "Uploading to storage path: $path")
                SupabaseClient.client.storage["avatars"].upload(path, bytes) {
                    upsert = true
                }
                Log.d("PerfilFragment", "Upload complete")

                val publicUrl = SupabaseClient.client.storage["avatars"].publicUrl(path)
                Log.d("PerfilFragment", "Public URL: $publicUrl")

                SupabaseClient.client.postgrest.from("users").upsert(
                    PhotoUrlUpdate(id = user.id, urlPhoto = publicUrl)
                )
                Log.d("PerfilFragment", "DB url_photo updated")

                val cacheBustedUrl = "$publicUrl?v=${System.currentTimeMillis()}"
                imageViewProfilePhoto.load(cacheBustedUrl) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person_placeholder)
                    error(R.drawable.ic_person_placeholder)
                }

                (activity as? MainActivity)?.refreshToolbarAvatar(cacheBustedUrl)

                Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("PerfilFragment", "Upload failed", e)
                Toast.makeText(requireContext(), "Error al subir la foto: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                frameLayoutProfilePhoto.isEnabled = true
            }
        }
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo leer la imagen. El archivo puede estar vacío.")
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (original == null) {
            throw IllegalStateException("No se pudo decodificar la imagen. Intenta con otra foto.")
        }

        val maxSize = 800
        val scaled = if (original.width > maxSize || original.height > maxSize) {
            val ratio = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt(),
                (original.height * ratio).toInt(),
                true
            )
        } else {
            original
        }

        return ByteArrayOutputStream().also { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }.toByteArray()
    }

    // -------------------------------------------------------------------------
    // Carga de perfil
    // -------------------------------------------------------------------------

    private fun loadUserProfile() {
        val user = SupabaseClient.client.auth.currentUserOrNull()
        if (user == null) {
            Toast.makeText(requireContext(), "No hay sesión activa", Toast.LENGTH_SHORT).show()
            return
        }

        editTextEmail.setText(user.email)

        lifecycleScope.launch {
            try {
                val profile = SupabaseClient.client.postgrest.from("users")
                    .select {
                        filter { eq("id", user.id) }
                    }.decodeList<UserProfile>().firstOrNull()

                if (profile != null) {
                    editTextName.setText(profile.name ?: "")
                    editTextLastName.setText(profile.lastname ?: "")
                    textViewFullName.text = "${profile.name.orEmpty()} ${profile.lastname.orEmpty()}".trim()
                    textViewRole.text = "Nivel de cuenta: ${if (profile.role == "admin") "Administrador" else "Cliente"}"

                    if (!profile.urlPhoto.isNullOrEmpty()) {
                        imageViewProfilePhoto.load(profile.urlPhoto) {
                            transformations(CircleCropTransformation())
                            placeholder(R.drawable.ic_person_placeholder)
                            error(R.drawable.ic_person_placeholder)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("PerfilFragment", "Error loading profile", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "Sin conexión. Verifica tu red e intenta de nuevo.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Guardar cambios de nombre/email/contraseña
    // -------------------------------------------------------------------------

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

                val updatedProfile = UserProfile(id = user.id, name = name, lastname = lastName)
                SupabaseClient.client.postgrest.from("users").upsert(updatedProfile)

                if (email != user.email) {
                    SupabaseClient.client.auth.updateUser { this.email = email }
                    Toast.makeText(requireContext(), "Correo actualizado (verifica tu bandeja)", Toast.LENGTH_SHORT).show()
                }

                if (password.isNotEmpty()) {
                    SupabaseClient.client.auth.updateUser { this.password = password }
                    if (SecurityManager.isBiometricEnabled(requireContext())) {
                        SecurityManager.saveCredentials(requireContext(), email, password)
                    }
                    Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                } else if (email != user.email && SecurityManager.isBiometricEnabled(requireContext())) {
                    val (_, savedPass) = SecurityManager.getCredentials(requireContext())
                    if (savedPass != null) {
                        SecurityManager.saveCredentials(requireContext(), email, savedPass)
                    }
                }

                textViewFullName.text = "$name $lastName".trim()
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

    // -------------------------------------------------------------------------
    // Biometría
    // -------------------------------------------------------------------------

    private fun setupBiometricUI() {
        if (SecurityManager.isBiometricAvailable(requireContext())) {
            layoutBiometric.visibility = View.VISIBLE

            isInternalChange = true
            switchBiometric.isChecked = SecurityManager.isBiometricEnabled(requireContext())
            isInternalChange = false

            switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                if (isInternalChange) return@setOnCheckedChangeListener

                if (isChecked) {
                    SecurityManager.showBiometricPrompt(
                        activity = requireActivity(),
                        title = "Habilitar Acceso",
                        onSuccess = {
                            val email = editTextEmail.text.toString()
                            val password = editTextPassword.text.toString()

                            if (password.isNotEmpty()) {
                                SecurityManager.saveCredentials(requireContext(), email, password)
                                SecurityManager.setBiometricEnabled(requireContext(), true)
                                Toast.makeText(requireContext(), "Huella habilitada", Toast.LENGTH_SHORT).show()
                            } else {
                                val (_, savedPass) = SecurityManager.getCredentials(requireContext())
                                if (savedPass != null) {
                                    SecurityManager.setBiometricEnabled(requireContext(), true)
                                    Toast.makeText(requireContext(), "Huella habilitada", Toast.LENGTH_SHORT).show()
                                } else {
                                    SecurityManager.saveCredentials(requireContext(), email, null)
                                    SecurityManager.setBiometricEnabled(requireContext(), true)
                                    Toast.makeText(requireContext(), "Huella habilitada para acceso rápido", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onError = { error ->
                            isInternalChange = true
                            switchBiometric.isChecked = false
                            isInternalChange = false
                            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    SecurityManager.setBiometricEnabled(requireContext(), false)
                    Toast.makeText(requireContext(), "Huella deshabilitada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
