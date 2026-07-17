package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ResellItem
import com.example.ui.ResellViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ResellViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf("All") }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    // List of available platforms
    val platforms = listOf("All", "PS1", "PS2", "PS3", "PS4", "PS5", "Switch", "Xbox", "Retro", "Other")

    // Filtered items
    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || 
                            item.barcode.contains(searchQuery) ||
                            item.notes.contains(searchQuery, ignoreCase = true)
        val matchesPlatform = selectedPlatformFilter == "All" || item.platform == selectedPlatformFilter
        matchesSearch && matchesPlatform
    }

    // Financial Computations
    val totalItemsCount = items.size
    val totalInvested = items.sumOf { it.priceBought }
    val totalSoldRevenue = items.filter { it.status == "Sold" }.sumOf { it.priceSold }
    val totalSoldCost = items.filter { it.status == "Sold" }.sumOf { it.priceBought }
    val netProfit = totalSoldRevenue - totalSoldCost
    val profitMarginPct = if (totalSoldCost > 0) (netProfit / totalSoldCost) * 100.0 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                "Vault Pro Hub",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Color(0xFF4ADE80), shape = CircleShape)
                                )
                                Text(
                                    "CLOUD SYNCED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), shape = CircleShape)
                            .testTag("scan_shortcut_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Item", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onNavigateToCloudSync,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Cloud Backup", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Resell Item", modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            // Metrics Overview Section
            MetricsOverviewGrid(
                totalCount = totalItemsCount,
                invested = currencyFormatter.format(totalInvested),
                revenue = currencyFormatter.format(totalSoldRevenue),
                profit = currencyFormatter.format(netProfit),
                margin = String.format(Locale.US, "%.1f%%", profitMarginPct),
                marginColor = if (netProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar_input"),
                placeholder = { Text("Search by game, console, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Platform Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(platforms) { platform ->
                    val isSelected = selectedPlatformFilter == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPlatformFilter = platform },
                        label = { Text(platform) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getPlatformColor(platform).copy(alpha = 0.25f),
                            selectedLabelColor = getPlatformColor(platform),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = getPlatformColor(platform),
                            borderColor = Color.Transparent
                        )
                    )
                }
            }

            // Database empty state / list
            if (items.isEmpty()) {
                EmptyStateView(
                    modifier = Modifier.weight(1f),
                    onLoadDemoData = {
                        seedDemoData(viewModel)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        InventoryCard(
                            item = item,
                            currencyFormatter = currencyFormatter,
                            onEditClick = { onEditItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsOverviewGrid(
    totalCount: Int,
    invested: String,
    revenue: String,
    profit: String,
    margin: String,
    marginColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F131C)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(Color(0x3D0F1115), shape = RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Active",
                value = totalCount.toString(),
                icon = Icons.Default.Inventory,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Capital Tied",
                value = invested,
                icon = Icons.Default.AttachMoney,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Sales Revenue",
                value = revenue,
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Margins / Net",
                value = "$margin ($profit)",
                icon = Icons.Default.Sell,
                color = marginColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x1F94A3B8)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x12FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E2E2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun InventoryCard(
    item: ResellItem,
    currencyFormatter: NumberFormat,
    onEditClick: () -> Unit
) {
    val platformColor = getPlatformColor(item.platform)
    val statusColor = getStatusColor(item.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .testTag("inventory_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x661E293B)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Platform Badge Indicator
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(platformColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Central info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Platform badge
                    Box(
                        modifier = Modifier
                            .background(platformColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, platformColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            item.platform,
                            style = MaterialTheme.typography.labelSmall,
                            color = platformColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Item Type
                    val typeColor = if (item.type == "CONSOLE") Color(0xFFF43F5E) else Color(0xFF3B82F6)
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            item.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (item.tested) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Tested Working",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E2E2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.notes.isNotEmpty()) {
                    Text(
                        item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right pricing & status info
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 10.dp)
            ) {
                // Status Pill
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        item.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing
                if (item.status == "Sold") {
                    Text(
                        currencyFormatter.format(item.priceSold),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80)
                    )
                    Text(
                        "Cost: ${currencyFormatter.format(item.priceBought)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Text(
                        currencyFormatter.format(item.priceBought),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E2E2)
                    )
                    if (item.onSale) {
                        Text(
                            "Listed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B)
                        )
                    } else {
                        Text(
                            "Inventory",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier, onLoadDemoData: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Inventory is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Scan video game barcodes or type console information to build your reseller database.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLoadDemoData,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("load_demo_data_button")
        ) {
            Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Seed Retro Resell Portfolio", fontWeight = FontWeight.SemiBold)
        }
    }
}

// Global color mapping helpers
fun getPlatformColor(platform: String): Color {
    return when (platform.uppercase()) {
        "PS1" -> Color(0xFF9FA8DA) // Light purple/blue retro
        "PS2" -> Color(0xFF3F51B5) // Classic PlayStation dark blue
        "PS3" -> Color(0xFFE53935) // Spiderman red
        "PS4" -> Color(0xFF1E88E5) // Blue
        "PS5" -> Color(0xFF00E5FF) // Futuristic cyan
        "SWITCH" -> Color(0xFFFF5252) // Nintendo Red
        "XBOX" -> Color(0xFF4CAF50) // Xbox Green
        "RETRO" -> Color(0xFFFF9800) // Amber Retro console vibe
        else -> Color(0xFF9E9E9E) // Gray
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "Inventory" -> Color(0xFF9C27B0) // Purple
        "Listed" -> Color(0xFF2196F3) // Blue
        "Sold" -> Color(0xFF4CAF50) // Green
        "Keep" -> Color(0xFFFF9800) // Orange / Personal collection
        else -> Color(0xFF757575)
    }
}

fun seedDemoData(viewModel: ResellViewModel) {
    val now = System.currentTimeMillis()
    val oneMonth = 30L * 24 * 60 * 60 * 1000
    val twoMonths = 60L * 24 * 60 * 60 * 1000
    val threeMonths = 90L * 24 * 60 * 60 * 1000

    val demoItems = listOf(
        ResellItem(
            type = "GAME",
            name = "Silent Hill 2 (Black Label)",
            platform = "PS2",
            barcode = "008888150119",
            condition = "Very Good",
            tested = true,
            status = "Sold",
            priceBought = 15.0,
            priceSold = 110.0,
            whereBought = "Garage Sale",
            whereSold = "eBay",
            dateBought = now - threeMonths,
            dateSold = now - twoMonths,
            notes = "Complete in box with map. Bought from a local garage sale. High value PS2 collector item."
        ),
        ResellItem(
            type = "GAME",
            name = "Metal Gear Solid",
            platform = "PS1",
            barcode = "012025340156",
            condition = "Good",
            tested = true,
            status = "Sold",
            priceBought = 5.0,
            priceSold = 45.0,
            whereBought = "Thrift Store",
            whereSold = "Facebook Marketplace",
            dateBought = now - twoMonths,
            dateSold = now - oneMonth,
            notes = "Discs polished. Fully tested working. Double jewel case."
        ),
        ResellItem(
            type = "CONSOLE",
            name = "PlayStation 2 Slim (Silver)",
            platform = "PS2",
            barcode = "071171980830",
            condition = "Good",
            tested = true,
            status = "Sold",
            priceBought = 25.0,
            priceSold = 85.0,
            whereBought = "Flea Market",
            whereSold = "Mercari",
            dateBought = now - oneMonth,
            dateSold = now - 5 * 24 * 60 * 60 * 1000,
            notes = "Includes OEM silver controller and memory card. Cleaned lasers."
        ),
        ResellItem(
            type = "GAME",
            name = "The Legend of Zelda: Tears of the Kingdom",
            platform = "Switch",
            barcode = "045496598112",
            condition = "Like New",
            tested = true,
            status = "Listed",
            onSale = true,
            priceBought = 30.0,
            priceSold = 0.0,
            whereBought = "Pawn Shop",
            whereSold = "",
            dateBought = now - 15 * 24 * 60 * 60 * 1000,
            notes = "Listed on eBay. Disc and artwork flawless."
        ),
        ResellItem(
            type = "GAME",
            name = "Elden Ring",
            platform = "PS5",
            barcode = "072267412154",
            condition = "New",
            tested = true,
            status = "Listed",
            onSale = true,
            priceBought = 20.0,
            priceSold = 0.0,
            whereBought = "Clearance",
            whereSold = "",
            dateBought = now - 10 * 24 * 60 * 60 * 1000,
            notes = "Brand new factory sealed. Great listing potential."
        ),
        ResellItem(
            type = "CONSOLE",
            name = "PlayStation Classic Retro Mini",
            platform = "Retro",
            barcode = "071171954157",
            condition = "New",
            tested = false,
            status = "Inventory",
            priceBought = 40.0,
            priceSold = 0.0,
            whereBought = "Facebook Marketplace",
            dateBought = now - 3 * 24 * 60 * 60 * 1000,
            notes = "Box is slightly dented, console is pristine."
        ),
        ResellItem(
            type = "GAME",
            name = "Castlevania: Symphony of the Night",
            platform = "PS1",
            barcode = "008371715016",
            condition = "Acceptable",
            tested = true,
            status = "Keep",
            priceBought = 40.0,
            priceSold = 0.0,
            whereBought = "Retro Shop",
            dateBought = now - fourMonths(),
            notes = "Personal favorite, keeping in personal inventory collection. Disc is scratched but loads perfect."
        )
    )

    for (item in demoItems) {
        viewModel.addOrUpdateItem(item)
    }
}

fun fourMonths(): Long = 120L * 24 * 60 * 60 * 1000
