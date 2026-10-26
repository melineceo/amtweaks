package com.multik.amtweaks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multik.amtweaks.ui.AMTweaksTheme
import com.multik.amtweaks.ui.AppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AMTweaksTheme {
                val vm: MainViewModel = viewModel()
                AppScreen(vm)
            }
        }
    }
}
