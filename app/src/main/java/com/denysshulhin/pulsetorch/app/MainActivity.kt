package com.denysshulhin.pulsetorch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.denysshulhin.pulsetorch.app.navigation.AppNavGraph
import com.denysshulhin.pulsetorch.core.design.theme.PulseTorchTheme

class MainActivity : ComponentActivity() {

    private val appVm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PulseTorchTheme {
                AppNavGraph(appVm)
            }
        }
    }
}
