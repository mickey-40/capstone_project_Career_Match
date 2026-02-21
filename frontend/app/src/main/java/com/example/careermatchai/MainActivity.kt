package com.example.careermatchai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.careermatchai.data.local.AppPrefs
import com.example.careermatchai.ui.Di
import com.example.careermatchai.ui.MainScreen
import com.example.careermatchai.ui.MainViewModel
import com.example.careermatchai.ui.NewAnalysisScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.careermatchai.ui.theme.CareerMatchAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm: MainViewModel = Di.provideViewModel(this)
        val prefs = AppPrefs(applicationContext)

        // sensible defaults for first run
        val defaultResume = """
            Senior Backend Engineer with 7+ years in Kotlin, Java, and Python...
        """.trimIndent()
        val defaultJob = """
            Hiring a Backend Engineer (Kotlin/Java, REST, SQL, Docker, cloud)...
        """.trimIndent()

        // ensure demo text has an initial value only once (no-op on later runs)
        lifecycleScope.launch {
            if (runBlocking { prefs.demoResumeFlow.first().isBlank() }) {
                prefs.setDemoResume(defaultResume)
            }
            if (runBlocking { prefs.demoJobFlow.first().isBlank() }) {
                prefs.setDemoJob(defaultJob)
            }
        }

        setContent {

                val nav = rememberNavController()

                // collect persisted UI settings
                val isDark by prefs.isDarkFlow.collectAsState(initial = false)
                val isDemo by prefs.isDemoFlow.collectAsState(initial = false)
                val demoResume by prefs.demoResumeFlow.collectAsState(initial = defaultResume)
                val demoJob by prefs.demoJobFlow.collectAsState(initial = defaultJob)

                CareerMatchAITheme(darkTheme = isDark) {   // <-- use your theme
                    Surface(color = MaterialTheme.colorScheme.background) {
                        NavHost(navController = nav, startDestination = "home") {
                            composable("home") {
                                MainScreen(
                                    viewModel = vm,
                                    onCreateNew = { nav.navigate("new") },
                                    isDark = isDark,
                                    onToggleTheme = { lifecycleScope.launch { prefs.setDark(!isDark) } },
                                    isDemo = isDemo,
                                    onToggleDemo = { lifecycleScope.launch { prefs.setDemo(!isDemo) } },
                                    demoResume = demoResume,
                                    demoJob = demoJob,
                                )
                            }
                            composable("new") {
                                NewAnalysisScreen(
                                    viewModel = vm,
                                    onDone = { nav.popBackStack() },
                                    isDemo = isDemo,
                                    demoResume = demoResume,
                                    demoJob = demoJob
                                )
                            }
                        }
                    }
                }



        }
    }
}
