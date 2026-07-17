package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permissions state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Camera permission is required for physical barcode scanning.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Laser Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    // Demo Barcodes list for emulator simulation
    val simulatedBarcodes = listOf(
        BarcodeSim("008888150119", "Silent Hill 2", "PS2", "Classic Horror"),
        BarcodeSim("012025340156", "Metal Gear Solid", "PS1", "Spy Action"),
        BarcodeSim("045496598112", "Zelda: Tears of the Kingdom", "Switch", "Adventure"),
        BarcodeSim("071171980830", "PlayStation 2 Console", "PS2", "Hardware"),
        BarcodeSim("072267412154", "Elden Ring", "PS5", "Dark Fantasy"),
        BarcodeSim("045496735517", "Pokemon Emerald", "Retro", "Retro RPG")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Scanner", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // 1. Camera preview layer (if permission granted)
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                  it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1115)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Camera Connection", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Permission denied or missing hardware camera.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }

            // 2. Holographic overlay & target reticle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Viewfinder rect outline
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .align(Alignment.Center)
                        .border(2.dp, Color(0xFF3B82F6), shape = RoundedCornerShape(12.dp))
                ) {
                    // Moving scanning red laser
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.02f)
                            .align(Alignment.TopCenter)
                            .absoluteOffset(y = (180.dp * laserOffset) - 5.dp)
                            .background(Color(0xFF4ADE80))
                    )
                }

                Text(
                    "Center barcode inside the blue reticle",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 220.dp)
                )
            }

            // 3. Emulator Demo Simulator (Sliding overlay at the bottom)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFA1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFF94A3B8).copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Streaming Emulator Helper",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                            Text(
                                "No physical camera in browser? Tap any barcode to simulate scanner!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(Icons.Default.SimCard, contentDescription = null, tint = Color(0xFF4ADE80))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(simulatedBarcodes) { sim ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1AFFFFFF), shape = RoundedCornerShape(10.dp))
                                    .clickable {
                                        Toast.makeText(context, "Scanned: ${sim.name}", Toast.LENGTH_SHORT).show()
                                        onBarcodeScanned(sim.barcode)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("simulate_scan_${sim.barcode}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sim.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("UPC: ${sim.barcode} • ${sim.platform} (${sim.desc})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x263B82F6), shape = RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Inject Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BarcodeSim(
    val barcode: String,
    val name: String,
    val platform: String,
    val desc: String
)
