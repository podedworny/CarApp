package com.example.carapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.carapp.ui.GarageApp
import com.example.carapp.ui.garage.GarageViewModel
import com.example.carapp.ui.theme.CarAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GarageViewModel by viewModels {
        GarageViewModel.Factory((application as CarApplication).container.carRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CarAppTheme { GarageApp(viewModel, (application as CarApplication).container) } }
    }
}
