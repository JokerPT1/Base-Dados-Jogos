package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ResellItem
import com.example.ui.ResellViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: ResellViewModel,
    itemId: Long, // 0L if adding new
    scannedBarcode: String?, // Pre-populated if coming back from barcode scanner
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // Determine if we are editing or creating
    val existingItem = remember(itemId, items) {
        if (itemId != 0L) items.find { it.id == itemId } else null
    }
    val isEditing = existingItem != null

    // Form states
    var type by remember { mutableStateOf(existingItem?.type ?: "GAME") }
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var platform by remember { mutableStateOf(existingItem?.platform ?: "PS2") }
    var barcode by remember { mutableStateOf(existingItem?.barcode ?: scannedBarcode ?: "") }
    var condition by remember { mutableStateOf(existingItem?.condition ?: "Good") }
    var tested by remember { mutableStateOf(existingItem?.tested ?: false) }
    var onSale by remember { mutableStateOf(existingItem?.onSale ?: false) }
    var status by remember { mutableStateOf(existingItem?.status ?: "Inventory") }
    
    var priceBoughtStr by remember { mutableStateOf(existingItem?.priceBought?.toString() ?: "") }
    var priceSoldStr by remember { mutableStateOf(existingItem?.priceSold?.toString() ?: "") }
    var whereBought by remember { mutableStateOf(existingItem?.whereBought ?: "") }
    var whereSold by remember { mutableStateOf(existingItem?.whereSold ?: "") }
    var notes by remember { mutableStateOf(existingItem?.notes ?: "") }
    
    var dateBought by remember { mutableStateOf(existingItem?.dateBought ?: System.currentTimeMillis()) }
    var dateSold by remember { mutableStateOf(existingItem?.dateSold ?: 0L) }

    // Update barcode if a new scanned value arrives
    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let {
            if (it.isNotEmpty()) {
                barcode = it
                Toast.makeText(context, "Loaded scanned barcode: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // List of platforms, conditions, and statuses
    val platforms = listOf("PS1", "PS2", "PS3", "PS4", "PS5", "Switch", "Xbox", "Retro", "Other")
    val conditions = listOf("New", "Like New", "Very Good", "Good", "Acceptable")
    val statuses = listOf("Inventory", "Listed", "Sold", "Keep")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Item Detail" else "Log New Asset",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.deleteItem(itemId)
                                Toast.makeText(context, "Item deleted.", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            modifier = Modifier.testTag("delete_item_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = Color(0xFFF43F5E))
                        }
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
            // GAME VS CONSOLE Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x331E293B), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("GAME", "CONSOLE").forEach { choice ->
                    val isChoiceSelected = type == choice
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChoiceSelected) Color(0xFF3B82F6) else Color.Transparent)
                            .clickable { type = choice }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (choice == "GAME") "Physical Game" else "Console / HW",
                            color = if (isChoiceSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Game Name/Title with Gemini Auto-fill Option
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Item Title", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Metal Gear Solid, PS2 Console...", color = Color(0xFF94A3B8)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedLabelColor = Color(0xFF3B82F6),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    // Gemini Metadata Lookup Button!
                    Button(
                        onClick = {
                            val lookupQuery = name.ifEmpty { barcode }
                            if (lookupQuery.isEmpty()) {
                                Toast.makeText(context, "Enter a title or barcode to search!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            Toast.makeText(context, "Consulting Gemini AI...", Toast.LENGTH_SHORT).show()
                            viewModel.performLookup(lookupQuery, isBarcode = name.isEmpty() && barcode.isNotEmpty()) { meta ->
                                if (meta != null) {
                                    name = meta.name
                                    type = meta.type
                                    platform = meta.platform
                                    priceBoughtStr = meta.estimatedPriceBought.toString()
                                    priceSoldStr = meta.estimatedPriceSold.toString()
                                    notes = "[AI Suggestion Year: ${meta.releaseYear}]\n${meta.notes}"
                                    Toast.makeText(context, "AI Autofilled game details!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to get AI info. Verify internet or API key.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        enabled = !isSearching,
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("gemini_autofill_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Autofill", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Fill", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Platform Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gaming Platform", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF)),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0x331E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(getPlatformColor(platform))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(platform, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(Color(0xFF1E293B))
                        ) {
                            platforms.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(getPlatformColor(p))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(p, color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        platform = p
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Barcode Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("UPC / Barcode (Optional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    placeholder = { Text("Scan or enter barcode number...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barcode_input_field"),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan with Camera",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.clickable {
                                // Save name/notes states so they can be reloaded
                                Toast.makeText(context, "Opening Camera Scanner...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedLabelColor = Color(0xFF3B82F6),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    )
                )
            }

            // Resell Status
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Resell Database Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEach { s ->
                        val isSel = status == s
                        val statusColor = getStatusColor(s)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) statusColor.copy(alpha = 0.25f) 
                                    else Color(0x1AFFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) statusColor else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    status = s 
                                    if (s == "Sold") {
                                        dateSold = System.currentTimeMillis()
                                        onSale = false
                                    } else if (s == "Listed") {
                                        onSale = true
                                    } else {
                                        onSale = false
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                s,
                                color = if (isSel) statusColor else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Switches: Tested & On-Sale
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Tested Working", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text("Item is visually and functionally tested", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                        }
                        Switch(
                            checked = tested,
                            onCheckedChange = { tested = it },
                            modifier = Modifier.testTag("tested_switch")
                        )
                    }

                    HorizontalDivider(color = Color(0x0DFFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Active Listed / On Sale", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text("Available for purchase in local marketplace", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            }
                        }
                        Switch(
                            checked = onSale,
                            onCheckedChange = { onSale = it },
                            modifier = Modifier.testTag("onsale_switch")
                        )
                    }
                }
            }

            // Condition Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Item Condition", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    conditions.forEach { cond ->
                        val isSel = condition == cond
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) Color(0xFF3B82F6).copy(alpha = 0.2f)
                                    else Color(0x1AFFFFFF)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) Color(0xFF3B82F6) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { condition = cond }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cond,
                                color = if (isSel) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Pricing Section (Dynamic)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Price Bought ($)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    OutlinedTextField(
                        value = priceBoughtStr,
                        onValueChange = { priceBoughtStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0.00", color = Color(0xFF94A3B8)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("price_bought_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        )
                    )
                }

                if (status == "Sold") {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Price Sold ($)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        OutlinedTextField(
                            value = priceSoldStr,
                            onValueChange = { priceSoldStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0.00", color = Color(0xFF94A3B8)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("price_sold_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            )
                        )
                    }
                }
            }

            // Location Sourcing Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bought From", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    OutlinedTextField(
                        value = whereBought,
                        onValueChange = { whereBought = it },
                        placeholder = { Text("e.g. Garage sale, eBay...", color = Color(0xFF94A3B8)) },
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x1AFFFFFF)
                        )
                    )
                }

                if (status == "Sold") {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Sold To", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        OutlinedTextField(
                            value = whereSold,
                            onValueChange = { whereSold = it },
                            placeholder = { Text("e.g. Mercari, Local...", color = Color(0xFF94A3B8)) },
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0x1AFFFFFF)
                            )
                        )
                    }
                }
            }

            // Notes field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notes & Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Condition descriptions, box inserts, maps, specific serial numbers...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0x1AFFFFFF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action
            Button(
                onClick = {
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Item title cannot be empty!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val priceBought = priceBoughtStr.toDoubleOrNull() ?: 0.0
                    val priceSold = priceSoldStr.toDoubleOrNull() ?: 0.0

                    val item = ResellItem(
                        id = itemId,
                        type = type,
                        name = name,
                        platform = platform,
                        barcode = barcode,
                        condition = condition,
                        tested = tested,
                        onSale = onSale,
                        status = status,
                        priceBought = priceBought,
                        priceSold = priceSold,
                        whereBought = whereBought,
                        whereSold = whereSold,
                        dateBought = dateBought,
                        dateSold = if (status == "Sold") dateSold.coerceAtLeast(System.currentTimeMillis()) else 0L,
                        notes = notes
                    )

                    viewModel.addOrUpdateItem(item)
                    Toast.makeText(context, "Inventory Log Saved!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_item_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Save to Database", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
