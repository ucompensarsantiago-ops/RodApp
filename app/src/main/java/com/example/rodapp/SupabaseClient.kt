package com.example.rodapp

import android.content.Context
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = "https://erttcudseqjrpyathmal.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVydHRjdWRzZXFqcnB5YXRobWFsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MTUwMzUsImV4cCI6MjA5MTA5MTAzNX0.zmyADFrL2iPvGMhy9eU2SQ7VWZsF_h2E2D86RpMzgWA"
        ) {
            install(Auth) {
                scheme = "rodapp"
                host = "login"
                sessionManager = AndroidSessionManager(context)
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
