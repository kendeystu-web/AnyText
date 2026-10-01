package com.anytext.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anytext.app.ui.browse.BrowseScreen
import com.anytext.app.ui.manager.ManagerScreen
import com.anytext.app.ui.reader.ReaderScreen
import com.anytext.app.ui.settings.SettingsScreen
import com.anytext.app.ui.storage.StorageScreen
import com.anytext.app.ui.theme.AnyTextTheme
import com.anytext.app.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pendingUri = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleViewIntent(intent)
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            AnyTextTheme(dark = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(i: Intent?) {
        val data = i?.data ?: return
        if (i.action == Intent.ACTION_VIEW) {
            try {
                contentResolver.takePersistableUriPermission(
                    data,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // для VIEW-интентов разрешение часто временное — это нормально
            }
            pendingUri.value = "uri|$data"
        }
    }

    @Composable
    private fun AppRoot(vm: AppViewModel) {
        val nav = rememberNavController()
        val pu by pendingUri.collectAsState()
        LaunchedEffect(pu) {
            pu?.let {
                nav.navigate("reader/${Uri.encode(it, "")}")
                pendingUri.value = null
            }
        }
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                ManagerScreen(
                    vm = vm,
                    onBrowse = { path -> nav.navigate("browse/${Uri.encode(path, "")}") },
                    onOpenFile = { spec -> nav.navigate("reader/${Uri.encode(spec, "")}") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenStorage = { nav.navigate("storage") }
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
            composable("storage") {
                StorageScreen(
                    vm = vm,
                    onBrowse = { path -> nav.navigate("browse/${Uri.encode(path, "")}") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("browse/{path}") { entry ->
                val path = Uri.decode(entry.arguments?.getString("path").orEmpty())
                BrowseScreen(
                    path = path,
                    appVm = vm,
                    onOpenDir = { p -> nav.navigate("browse/${Uri.encode(p, "")}") },
                    onOpenFile = { spec -> nav.navigate("reader/${Uri.encode(spec, "")}") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("reader/{key}") { entry ->
                val spec = Uri.decode(entry.arguments?.getString("key").orEmpty())
                ReaderScreen(
                    spec = spec,
                    appVm = vm,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
