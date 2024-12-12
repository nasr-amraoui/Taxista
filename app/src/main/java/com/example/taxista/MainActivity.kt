package com.example.taxista

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taxista.screen.LoginScreen
import com.example.taxista.screen.SignUpScreen
import com.example.taxista.screen.TaxiCounterScreen
import com.example.taxista.ui.theme.TaxistaTheme
import com.example.taxista.viewModel.AuthViewModel
import com.example.taxista.viewModel.TaxiCounterViewModel
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel
    private lateinit var taxiViewModel: TaxiCounterViewModel

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.entries.all { it.value }
        if (locationGranted) {
            // Location permissions granted, initialize viewModel
            taxiViewModel = TaxiCounterViewModel(this)
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val permissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!permissionsGranted) {
            locationPermissionRequest.launch(requiredPermissions)
        } else {
            // Permissions already granted, initialize viewModel
            taxiViewModel = TaxiCounterViewModel(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase if not already initialized
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize OSMDroid
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
        
        // Initialize ViewModels
        authViewModel = AuthViewModel()
        checkAndRequestLocationPermissions()
        
        // Initialize Google Sign In
        authViewModel.initGoogleSignIn(this, getString(R.string.default_web_client_id))

        setContent {
            TaxistaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser by authViewModel.currentUser.collectAsState()
                    val authState by authViewModel.authState.collectAsState()
                    
                    LaunchedEffect(currentUser) {
                        if (currentUser != null) {
                            navController.navigate("taxi_counter") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (currentUser != null) "taxi_counter" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToSignUp = { navController.navigate("signup") },
                                onLoginSuccess = { 
                                    navController.navigate("taxi_counter") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("signup") {
                            SignUpScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate("login") },
                                onSignUpSuccess = {
                                    navController.navigate("taxi_counter") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("taxi_counter") {
                            if (::taxiViewModel.isInitialized) {
                                TaxiCounterScreen(
                                    taxiViewModel = taxiViewModel,
                                    authViewModel = authViewModel,
                                    onSignOut = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
    }
}
