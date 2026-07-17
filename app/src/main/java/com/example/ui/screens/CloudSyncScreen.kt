package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ResellViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: ResellViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsState()
    
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Form states
    var cloudUrl by remember { mutableStateOf("https://sync.retroresell.io/v1/backup") }
    var backupJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }

    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val formattedSyncTime = remember(lastSyncTime) {
        if (lastSyncTime > 0L) {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            sdf.format(Date(lastSyncTime))
        } else {
            "Never Synced"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync & Backups", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Live Cloud Sync Console (Multi-Device)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Secure Cloud Sync Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "Sync and access your reseller database across multiple devices in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    HorizontalDivider(color = Color(0x0DFFFFFF))

                    // Cloud URL Endpoint
                    OutlinedTextField(
                        value = cloudUrl,
                        onValueChange = { cloudUrl = it },
                        label = { Text("Backup Server Host API") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Sync Status Metadata Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0DFFFFFF), shape = RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sync Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                Text(
                                    if (isSyncing) "Syncing..." else "Connected",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSyncing) Color(0xFFF59E0B) else Color(0xFF4ADE80)
                                )
                            }
                            Text(syncStatus, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Synced:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(formattedSyncTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Sync Now Trigger Button
                    Button(
                        onClick = {
                            if (items.isEmpty()) {
                                Toast.makeText(context, "No items to sync! Add some database items first.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.triggerCloudSync(cloudUrl) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sync Error: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_sync_button"),
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            disabledContainerColor = Color(0x3D3B82F6)
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading database payload...", color = Color.White)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Synchronize with Cloud Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Export / Copy JSON Database string
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Local Database Payload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "Generates a full portable JSON backup payload of your portfolio database. Copy and save this to load your exact database on another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Button(
                        onClick = {
                            viewModel.getBackupPayload { json ->
                                backupJsonText = json
                                Toast.makeText(context, "Backup JSON string generated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80), contentColor = Color(0xFF0F1115)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_backup_button")
                    ) {
                        Text("Generate Backup Code", fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(visible = backupJsonText.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = backupJsonText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E2E2),
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }

                            Button(
                                onClick = {
                                    val clip = ClipData.newPlainText("RetroResellBackup", backupJsonText)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied backup code to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Backup Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Import / Paste JSON Database String to Merge/Sync
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore & Sync from Payload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "Paste a backup JSON payload copied from another device to restore or merge items into your current database instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste backup code here...", color = Color(0xFF94A3B8)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("import_json_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        )
                    )

                    Button(
                        onClick = {
                            if (importJsonText.trim().isEmpty()) {
                                Toast.makeText(context, "Paste some backup payload string first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.restoreFromPayload(importJsonText) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    importJsonText = "" // clear
                                } else {
                                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Restore & Merge Database", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
