package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.RideFareDatabase
import com.example.data.repository.RideRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RideFareViewModel
import com.example.ui.viewmodel.RideFareViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = RideFareDatabase.getDatabase(applicationContext)
        val repository = RideRepository(database.rideDao())
        val viewModelFactory = RideFareViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: RideFareViewModel = viewModel(factory = viewModelFactory)
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
