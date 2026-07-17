package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ResellItem
import com.example.ui.ResellViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ResellViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    // Filter sold items for revenue & cogs calculations
    val soldItems = remember(items) { items.filter { it.status == "Sold" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Performance", fontWeight = FontWeight.Bold, color = Color.White) },
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
        if (soldItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Sales Logged Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Go back to the dashboard, edit any game or console, change its status to 'Sold', log the sale price, and see your profit margins grow!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Charts
                MonthlyBarChartSection(soldItems, currencyFormatter)
                PlatformDonutChartSection(soldItems, currencyFormatter)

                // High-level statistics summary
                ProfitLeaderboardSection(soldItems, currencyFormatter)
            }
        }
    }
}

// ----------------------------------------------------
// Custom Canvas 1: Monthly sales & profit interactive bar chart
// ----------------------------------------------------
data class MonthlyStats(
    val monthLabel: String,
    val revenue: Double,
    val cost: Double,
    val profit: Double
)

@Composable
fun MonthlyBarChartSection(soldItems: List<ResellItem>, currencyFormatter: NumberFormat) {
    // Process sold items to group by Month
    val monthlyData = remember(soldItems) {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.US)
        val grouped = soldItems.groupBy { sdf.format(Date(it.dateSold)) }
        
        // Sort grouped months chronologically
        grouped.map { (month, list) ->
            val rev = list.sumOf { it.priceSold }
            val cost = list.sumOf { it.priceBought + it.shippingCost }
            val profit = rev - cost
            val sortTime = list.firstOrNull()?.dateSold ?: 0L
            Pair(sortTime, MonthlyStats(month, rev, cost, profit))
        }
        .sortedBy { it.first }
        .map { it.second }
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Sales & Profit Margin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Interactive column chart. Tap on any month to view details.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            
            Spacer(modifier = Modifier.height(16.dp))

            if (monthlyData.isNotEmpty()) {
                val maxVal = remember(monthlyData) {
                    val peak = monthlyData.maxOf { maxOf(it.revenue, it.cost, it.profit) }
                    if (peak > 0) peak * 1.15 else 100.0
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(monthlyData) {
                                detectTapGestures { offset ->
                                    val barWidthZone = size.width / monthlyData.size
                                    val index = (offset.x / barWidthZone).toInt()
                                    if (index in monthlyData.indices) {
                                        selectedIndex = if (selectedIndex == index) -1 else index
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val paddingLeft = 60f
                        val paddingBottom = 60f
                        val plotWidth = canvasWidth - paddingLeft - 20f
                        val plotHeight = canvasHeight - paddingBottom - 20f

                        // Draw Gridlines and Axes
                        val gridPaint = Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            strokeWidth = 1f
                            style = Paint.Style.STROKE
                        }
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.LTGRAY
                            textSize = 28f
                            textAlign = Paint.Align.RIGHT
                        }

                        // Draw Horizontal Gridlines (3 levels)
                        for (i in 0..3) {
                            val y = plotHeight - (plotHeight * i / 3f) + 10f
                            val valueText = currencyFormatter.format(maxVal * i / 3f)
                            drawContext.canvas.nativeCanvas.drawText(
                                valueText,
                                paddingLeft - 10f,
                                y + 10f,
                                textPaint
                            )
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(paddingLeft, y),
                                end = Offset(canvasWidth - 20f, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Columns
                        val numMonths = monthlyData.size
                        val colWidthSpace = plotWidth / numMonths
                        val colPadding = colWidthSpace * 0.2f
                        val actualBarWidth = (colWidthSpace - colPadding) / 3f

                        for (i in monthlyData.indices) {
                            val stats = monthlyData[i]
                            val startX = paddingLeft + (i * colWidthSpace) + (colPadding / 2f)

                            // Normalized heights
                            val revY = plotHeight * (stats.revenue / maxVal).toFloat()
                            val costY = plotHeight * (stats.cost / maxVal).toFloat()
                            val profY = plotHeight * (stats.profit / maxVal).toFloat()

                            // Revenue Bar (Electric Blue)
                            val isSel = selectedIndex == i
                            val alphaFactor = if (selectedIndex == -1 || isSel) 1.0f else 0.3f

                            drawRect(
                                color = Color(0xFF3B82F6).copy(alpha = alphaFactor),
                                topLeft = Offset(startX, plotHeight + 10f - revY),
                                size = Size(actualBarWidth, revY)
                            )

                            // Cost Bar (Amber/Orange)
                            drawRect(
                                color = Color(0xFFF59E0B).copy(alpha = alphaFactor),
                                topLeft = Offset(startX + actualBarWidth, plotHeight + 10f - costY),
                                size = Size(actualBarWidth, costY)
                            )

                            // Net Profit Bar (Neon Green)
                            drawRect(
                                color = Color(0xFF4ADE80).copy(alpha = alphaFactor),
                                topLeft = Offset(startX + (actualBarWidth * 2), plotHeight + 10f - profY),
                                size = Size(actualBarWidth, profY)
                            )

                            // Draw X axis labels
                            val xLabelPaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 26f
                                textAlign = Paint.Align.CENTER
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                stats.monthLabel,
                                startX + (colWidthSpace / 2f) - (colPadding / 2f),
                                canvasHeight - 15f,
                                xLabelPaint
                            )

                            // Highlights
                            if (isSel) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.15f),
                                    topLeft = Offset(startX - 5f, 10f),
                                    size = Size(colWidthSpace - colPadding + 10f, plotHeight + 10f)
                                )
                            }
                        }
                    }
                }

                // Interactive Tooltip Card
                AnimatedVisibility(
                    visible = selectedIndex in monthlyData.indices,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (selectedIndex in monthlyData.indices) {
                        val stats = monthlyData[selectedIndex]
                        val marginPct = if (stats.cost > 0) (stats.profit / stats.cost) * 100.0 else 0.0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("monthly_tooltip_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x333B82F6)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1F3B82F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stats.monthLabel, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        "Margin: ${String.format(Locale.US, "%.1f%%", marginPct)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (stats.profit >= 0) Color(0xFF4ADE80) else Color(0xFFF43F5E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Revenue: ${currencyFormatter.format(stats.revenue)}", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    Text("Cost: ${currencyFormatter.format(stats.cost)}", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    Text("Net: ${currencyFormatter.format(stats.profit)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Revenue", Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem("Inventory Cost", Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem("Net Profit", Color(0xFF4ADE80))
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, shape = RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ----------------------------------------------------
// Custom Canvas 2: Platform net profit contribution pie/donut chart
// ----------------------------------------------------
data class ShareStats(
    val platform: String,
    val profit: Double,
    val color: Color
)

@Composable
fun PlatformDonutChartSection(soldItems: List<ResellItem>, currencyFormatter: NumberFormat) {
    // Compute net profits per platform
    val platformStats = remember(soldItems) {
        val grouped = soldItems.groupBy { it.platform }
        val rawStats = grouped.map { (platform, list) ->
            val rev = list.sumOf { it.priceSold }
            val cost = list.sumOf { it.priceBought + it.shippingCost }
            val net = rev - cost
            ShareStats(platform, net, getPlatformColor(platform))
        }
        // Filter out non-profitable platforms for the donut share
        rawStats.filter { it.profit > 0 }.sortedByDescending { it.profit }
    }

    val totalProfit = remember(platformStats) { platformStats.sumOf { it.profit } }

    var selectedIndex by remember { mutableStateOf(-1) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Profit Contribution by Platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Donut chart distribution. Tap slices or legends to filter.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))

            Spacer(modifier = Modifier.height(16.dp))

            if (platformStats.isNotEmpty() && totalProfit > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .weight(1.1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(140.dp)
                                .pointerInput(platformStats, totalProfit) {
                                    detectTapGestures { offset ->
                                        // Simple approximation tap handler based on sectors
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val x = offset.x - center.x
                                        val y = offset.y - center.y
                                        var angle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f

                                        var curAngle = 0f
                                        var matchedIdx = -1
                                        for (i in platformStats.indices) {
                                            val sweep = (platformStats[i].profit / totalProfit * 360f).toFloat()
                                            if (angle >= curAngle && angle <= (curAngle + sweep)) {
                                                matchedIdx = i
                                                break
                                            }
                                            curAngle += sweep
                                        }
                                        if (matchedIdx != -1) {
                                            selectedIndex = if (selectedIndex == matchedIdx) -1 else matchedIdx
                                        }
                                    }
                                }
                        ) {
                            var startAngle = 0f
                            for (i in platformStats.indices) {
                                val share = platformStats[i]
                                val sweepAngle = (share.profit / totalProfit * 360f).toFloat()
                                val isSel = selectedIndex == i
                                val strokeWidth = if (isSel) 32f else 22f

                                drawArc(
                                    color = share.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth),
                                    size = Size(size.width - 40f, size.height - 40f),
                                    topLeft = Offset(20f, 20f)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center Legend Card
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val activeShare = if (selectedIndex in platformStats.indices) platformStats[selectedIndex] else null
                            if (activeShare != null) {
                                Text(
                                    activeShare.platform,
                                    fontWeight = FontWeight.Bold,
                                    color = activeShare.color,
                                    fontSize = 15.sp
                                )
                                Text(
                                    currencyFormatter.format(activeShare.profit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    String.format(Locale.US, "%.1f%%", (activeShare.profit / totalProfit) * 100.0),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                Text("Net", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(
                                    currencyFormatter.format(totalProfit),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text("Total", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend list
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        platformStats.forEachIndexed { idx, share ->
                            val isSel = selectedIndex == idx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIndex = if (isSel) -1 else idx }
                                    .background(
                                        if (isSel) share.color.copy(alpha = 0.12f) else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(share.color, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        share.platform,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) share.color else Color(0xFFE2E2E2)
                                    )
                                }
                                Text(
                                    currencyFormatter.format(share.profit),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Profit Share Available", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

// ----------------------------------------------------
// Platform Metrics Leaderboard
// ----------------------------------------------------
@Composable
fun ProfitLeaderboardSection(soldItems: List<ResellItem>, currencyFormatter: NumberFormat) {
    val totalRevenue = soldItems.sumOf { it.priceSold }
    val totalCost = soldItems.sumOf { it.priceBought + it.shippingCost }
    val totalProfit = totalRevenue - totalCost

    // Sort platforms by average profit margin
    val sortedPlatforms = remember(soldItems) {
        soldItems.groupBy { it.platform }
            .map { (platform, list) ->
                val pRev = list.sumOf { it.priceSold }
                val pCost = list.sumOf { it.priceBought + it.shippingCost }
                val pNet = pRev - pCost
                val avgMargin = if (pCost > 0) (pNet / pCost) * 100.0 else 0.0
                Triple(platform, pNet, avgMargin)
            }
            .sortedByDescending { it.second }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4ADE80).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("By Net Profit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedPlatforms.forEachIndexed { index, (platform, net, margin) ->
                    val barColor = getPlatformColor(platform)
                    val shareRatio = if (totalProfit > 0) (net / totalProfit).toFloat().coerceIn(0f, 1f) else 0f

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "#${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.width(24.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(barColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        platform,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = barColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Margin: ${String.format(Locale.US, "%.0f%%", margin)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                currencyFormatter.format(net),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Progress bar reflecting profit share
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color(0xFF1E293B), shape = RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(shareRatio)
                                    .height(6.dp)
                                    .background(barColor, shape = RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
