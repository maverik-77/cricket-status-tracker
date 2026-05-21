package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.CricketRepository
import com.example.ui.CricketDashboard
import com.example.ui.CricketViewModel
import com.example.ui.CricketViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Room Database instance and repositories
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CricketRepository(database.cricketDao())

        // Obtain CricketViewModel through simple dependency-provider Factory
        val factory = CricketViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[CricketViewModel::class.java]

        setContent {
            MyApplicationTheme {
                CricketDashboard(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
