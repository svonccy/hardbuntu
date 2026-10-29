package com.hardbuntu.app.data.remote

import com.hardbuntu.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    /** Deep link al que Supabase redirige desde los correos de confirmación y recuperación: hardbuntu://auth */
    const val AUTH_SCHEME = "hardbuntu"
    const val AUTH_HOST = "auth"

    val instance: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                scheme = AUTH_SCHEME
                host = AUTH_HOST
            }
            install(Storage)
        }
    }
}
