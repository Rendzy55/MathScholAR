package com.explorebyte.ar.data.remote

import com.explorebyte.ar.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }
}
