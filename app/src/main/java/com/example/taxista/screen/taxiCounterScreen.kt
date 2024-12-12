package com.example.taxista.screen

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.taxista.components.QRCodeDialog
import com.example.taxista.components.TaxistaButton
import com.example.taxista.components.TaxistaCard
import com.example.taxista.components.TaxistaMetricCard
import com.example.taxista.viewModel.AuthViewModel
import com.example.taxista.viewModel.TaxiCounterViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiCounterScreen(
    taxiViewModel: TaxiCounterViewModel,
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    var showQRCode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val username by authViewModel.currentUsername.collectAsState()
    val currentLocation by taxiViewModel.currentLocation.collectAsState()
    val routePoints by taxiViewModel.routePoints.collectAsState()
    val distanceTraveled by taxiViewModel.distanceTraveled.collectAsState()
    val totalCost by taxiViewModel.totalCost.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Taxista",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    username.let { username ->
                        if (username.isNotBlank()) {
                            Text(
                                text = "Hi, $username",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    
                    IconButton(onClick = { showQRCode = true }) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode,
                            contentDescription = "Show QR Code"
                        )
                    }
                    
                    IconButton(onClick = {
                        authViewModel.signOut()
                        onSignOut()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map View
            Box(
                modifier = Modifier
                    .weight(10f)
                    .fillMaxWidth()
            ) {
                // Map
                MapViewContainer(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    currentLocation = currentLocation,
                    routePoints = routePoints
                )

                // Floating Action Button for centering map
                FloatingActionButton(
                    onClick = { /* Center map on current location */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center Location")
                }
            }

            // Metrics Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TaxistaMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Distance",
                    value = "${(distanceTraveled * 10).roundToInt() / 10f}",
                    unit = "meters",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                TaxistaMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Cost",
                    value = "${totalCost}",
                    unit = "DH",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reset Button
            TaxistaButton(
                onClick = { taxiViewModel.resetCounter() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Reset Counter",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showQRCode) {
        val driverInfo = """
            🚕 TAXISTA DRIVER INFO 🚕
            ──────────────────────
            👤 Driver: $username
            🪪 License: TX-${username.hashCode().toString().takeLast(4)}
            📍 Current Trip:
               • Distance: ${(distanceTraveled * 10).roundToInt() / 10f} meters
               • Cost: ${totalCost} DH
            ⭐ Rating: 4.9/5.0
            🏢 Company: Taxista Services
            📞 Support: +212-TAXI-APP
            ──────────────────────
            💡 This QR code is updated in real-time
            with current trip information.
            
            🔒 Licensed and verified driver
            ✅ Background checked
            🚘 Insured vehicle
            
            Download Taxista app:
            https://play.google.com/store/apps/taxista
        """.trimIndent()
        
        QRCodeDialog(
            driverInfo = driverInfo,
            onDismiss = { showQRCode = false }
        )
    }
}

@Composable
private fun MapViewContainer(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    currentLocation: GeoPoint?,
    routePoints: List<GeoPoint>
) {
    val mapView = remember {
        MapView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(18.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { map ->
        currentLocation?.let { location ->
            map.overlays.clear()
            
            // Add current location marker
            Marker(map).apply {
                position = location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Current Location"
                map.overlays.add(this)
            }

            // Add route line
            if (routePoints.size > 1) {
                Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = android.graphics.Color.BLUE
                    outlinePaint.strokeWidth = 5f
                    map.overlays.add(this)
                }
            }

            // Center map on current location
            map.controller.setCenter(location)
        }
    }
}
