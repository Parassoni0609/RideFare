package com.example

import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.example.util.PriceAlertNotificationHelper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.local.RideFareDatabase
import com.example.data.repository.RideRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RideFareViewModel
import com.example.ui.viewmodel.RideFareViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var rideViewModel: RideFareViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = RideFareDatabase.getDatabase(applicationContext)
        val repository = RideRepository(database.rideDao())
        val viewModelFactory = RideFareViewModelFactory(repository)

        rideViewModel = ViewModelProvider(this, viewModelFactory)[RideFareViewModel::class.java]
        if (savedInstanceState == null) openNotificationRoute(intent)
        setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = rideViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNotificationRoute(intent)
    }

    private fun openNotificationRoute(intent: Intent?) {
        val id = intent?.getLongExtra(PriceAlertNotificationHelper.OPEN_ROUTE_ID, -1L) ?: -1L
        if (id > 0) rideViewModel.openSavedRoute(id)
    }
}
