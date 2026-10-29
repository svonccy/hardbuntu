package com.hardbuntu.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hardbuntu.app.data.remote.SupabaseClient
import com.hardbuntu.app.ui.navigation.AppNavigation
import com.hardbuntu.app.ui.theme.HardbuntuTheme
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private val storeViewModel: StoreViewModel by viewModels { StoreViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al girar la pantalla el intent se reenvía: solo se procesa en el arranque real
        if (savedInstanceState == null) handleAuthLink(intent)
        setContent {
            HardbuntuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(storeViewModel = storeViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthLink(intent)
    }

    /** Procesa los enlaces hardbuntu://auth de los correos de Supabase (confirmación y recuperación). */
    private fun handleAuthLink(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data ?: return
        if (uri.scheme != SupabaseClient.AUTH_SCHEME || uri.host != SupabaseClient.AUTH_HOST) return

        val isRecovery = uri.fragment?.contains("type=recovery") == true
        SupabaseClient.instance.handleDeeplinks(intent) {
            if (isRecovery) storeViewModel.onPasswordRecoveryLink()
        }
    }
}
