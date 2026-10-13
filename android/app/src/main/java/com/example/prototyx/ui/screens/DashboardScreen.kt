package com.example.prototyx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.MarketIndicatorsResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*

data class PresetBasket(
    val title: String,
    val icon: String,
    val subtitle: String,
    val holdings: Map<String, Float>
)

data class PortfolioHealthReport(
    val score: Int,
    val status: String,
    val statusColor: Color,
    val diagnosis: String
)

@Composable
fun DashboardScreen(
    repository: DataRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for Tickers and their Weights (Portfolio Holdings)
    val holdings by repository.holdings.collectAsState(initial = emptyMap())
    val coroutineScope = rememberCoroutineScope()
    var selectedTicker by remember { mutableStateOf("") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchWeight by remember { mutableStateOf("") }

    val activeTickers = remember(holdings) { holdings.keys.toList() }
    val currentWeights = remember(holdings) { holdings.values.toList() }
    
    val defaultColors = listOf(
        Color(0xFF111111), Color(0xFF787774), Color(0xFFC0C0C0), Color(0xFFE1F3FE),
        Color(0xFF1F6C9F), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800)
    )

    LaunchedEffect(selectedTicker) {
        if (selectedTicker.isNotEmpty()) {
            isLoading = true
            searchError = null
            try {
                tickerData = repository.getIndicators(selectedTicker)
            } catch (e: Exception) {
                searchError = "Intelligence extraction failed for $selectedTicker: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    // Set initial selected ticker if empty and holdings exist
    LaunchedEffect(holdings) {
        if (selectedTicker.isEmpty() && holdings.isNotEmpty()) {
            selectedTicker = holdings.keys.first()
        }
    }

    val demoBaskets = remember {
        listOf(
            PresetBasket("Tech Titans", "🚀", "Growth Conviction", mapOf("NVDA" to 0.35f, "AAPL" to 0.35f, "MSFT" to 0.30f)),
            PresetBasket("Balanced Core", "⚖️", "Indian Bluechips", mapOf("RELIANCE" to 0.40f, "TCS" to 0.30f, "HDFCBANK" to 0.30f)),
            PresetBasket("Defensive Yield", "🛡️", "Low Volatility", mapOf("ITC" to 0.40f, "HINDUNILVR" to 0.35f, "TCS" to 0.25f))
        )
    }

    val healthReport = remember(holdings) {
        if (holdings.isEmpty()) {
            PortfolioHealthReport(0, "EMPTY PORTFOLIO", Color.Gray, "Select a 1-tap demo preset or discover assets below to generate a health audit.")
        } else {
            val totalAssets = holdings.size
            val totalWeight = holdings.values.sum()
            val maxWeight = if (totalWeight > 0) (holdings.values.maxOrNull() ?: 0f) / totalWeight else 1f
            
            var score = 50
            if (totalAssets >= 3) score += 25
            else if (totalAssets == 2) score += 12
            
            if (maxWeight < 0.45f) score += 20
            else if (maxWeight > 0.60f) score -= 25

            score = score.coerceIn(25, 96)

            val (status, color, diagnosis) = when {
                score >= 80 -> Triple(
                    "ROBUST ALLOCATION",
                    Color(0xFF2E7D32),
                    "Portfolio is resilient across $totalAssets holdings. No single stock exceeds 45% capital allocation."
                )
                score >= 60 -> Triple(
                    "MODERATE CONCENTRATION",
                    Color(0xFFF57C00),
                    "Top holding represents ${(maxWeight * 100).toInt()}% of portfolio. Rebalance recommended to limit sector drawdown."
                )
                else -> Triple(
                    "HIGH VULNERABILITY",
                    Color(0xFFD32F2F),
                    "Elevated single-asset concentration (${(maxWeight * 100).toInt()}%). Add defensive counterweights or hedge exposure."
                )
            }
            PortfolioHealthReport(score, status, color, diagnosis)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Hero Header
        item(key = "heading") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditorialHeading(text = "Prototyx")
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataLabel(text = "DYNAMIC PORTFOLIO OPS")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                syncError = null
                                try {
                                    repository.syncWithCloud()
                                } catch (e: Exception) {
                                    syncError = "Sync failed: ${e.message}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync with Cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.logout()
                                onLogout()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (syncError != null) {
            item {
                Text(
                    text = syncError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // 1-Tap Quick-Test Preset Baskets (Examiner Zero Friction)
        item(key = "presets_row") {
            Column {
                MetadataLabel(text = "1-Tap Quick-Test Portfolios", color = Color(0xFF1F6C9F))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    demoBaskets.forEach { basket ->
                        PresetBasketChip(
                            basket = basket,
                            onClick = {
                                coroutineScope.launch {
                                    // Remove old holdings and load preset
                                    holdings.keys.forEach { oldTicker ->
                                        repository.removeHolding(oldTicker)
                                    }
                                    basket.holdings.forEach { (ticker, weight) ->
                                        repository.updateHolding(ticker, weight)
                                    }
                                    selectedTicker = basket.holdings.keys.first()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Portfolio Health & Stress Scorecard
        item(key = "health_scorecard") {
            PortfolioHealthCard(report = healthReport)
        }

        // Search & Discovery (Universal Input)
        item(key = "search_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Asset Discovery & Allocation")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Ticker (e.g. NVDA)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = searchWeight,
                        onValueChange = { searchWeight = it },
                        label = { Text("Weight (e.g. 0.2)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Update Portfolio Mesh",
                    onClick = {
                        val ticker = searchQuery.trim().uppercase()
                        val weight = searchWeight.toDoubleOrNull()?.toFloat() ?: 0f
                        if (ticker.isNotEmpty()) {
                            coroutineScope.launch {
                                if (weight > 0) {
                                    repository.updateHolding(ticker, weight)
                                } else {
                                    repository.removeHolding(ticker)
                                }
                            }
                            selectedTicker = ticker
                            searchQuery = ""
                            searchWeight = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter weight 0 to remove an asset.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Real-time Asset Intelligence
        item(key = "asset_intel") {
            DoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialHeading(text = "Asset Intelligence", modifier = Modifier.weight(1f))
                    
                    if (activeTickers.isNotEmpty()) {
                        TextButton(onClick = {
                            val nextIndex = (activeTickers.indexOf(selectedTicker) + 1) % activeTickers.size
                            selectedTicker = activeTickers[nextIndex]
                        }) {
                            MetadataLabel(text = "ACTIVE: $selectedTicker", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (searchError != null) {
                    Text(text = searchError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                } else if (tickerData != null) tickerData?.let { data ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        BentoMetric(label = "Last Price", value = "₹${data.close}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "PE Ratio", value = "${data.peRatio ?: "N/A"}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Div Yield", value = "${String.format("%.2f", (data.dividendYield ?: 0.0) * 100)}%", modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MetadataLabel(text = "Technical Mesh Metrics")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TechnicalMetricItem("Relative Strength Index", data.rsi?.let { String.format("%.2f", it) } ?: "N/A")
                    TechnicalMetricItem("50-Day Moving Avg", "₹${data.sma50?.let { String.format("%.2f", it) } ?: "N/A"}")
                    TechnicalMetricItem("200-Day Moving Avg", "₹${data.sma200?.let { String.format("%.2f", it) } ?: "N/A"}")

                    data.aiMemo?.let { memo ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = memo,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Text("Search an asset above to load intelligence.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Multi-Asset Distribution Map (Truly Dynamic)
        if (activeTickers.isNotEmpty()) {
            item(key = "dist_map") {
                DoubleBezelCard {
                    EditorialHeading(text = "Portfolio Exposure")
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PortfolioPieChart(
                            weights = currentWeights, 
                            colors = defaultColors.take(activeTickers.size).let { 
                                if (it.size < activeTickers.size) it + List(activeTickers.size - it.size) { Color.Gray } else it 
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    val totalWeight = currentWeights.sum()
                    val legendItems = activeTickers.mapIndexed { index, ticker ->
                        val weight = holdings[ticker] ?: 0f
                        val percentage = if (totalWeight > 0) (weight / totalWeight * 100).toInt() else 0
                        val color = if (index < defaultColors.size) defaultColors[index] else Color.Gray
                        "$ticker ($percentage%)" to color
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        legendItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { (label, color) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        LegendItem(label, color)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TechnicalMetricItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PortfolioPieChart(weights: List<Float>, colors: List<Color>) {
    Canvas(
        modifier = Modifier
            .size(180.dp)
    ) {
        var startAngle = -90f
        val sum = weights.sum()
        for (i in weights.indices) {
            val sweepAngle = (weights[i] / sum) * 360f
            drawArc(
                color = colors[i],
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Soft separation
                useCenter = false,
                style = Stroke(width = 12.dp.toPx()) // Thinner stroke
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PresetBasketChip(
    basket: PresetBasket,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = basket.icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = basket.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = basket.subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PortfolioHealthCard(report: PortfolioHealthReport) {
    DoubleBezelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorialHeading(text = "Portfolio Health Meter")
            Surface(
                color = report.statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = report.status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = report.statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${report.score}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / 100",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                MetadataLabel(text = "STRESS & RESILIENCE SCORE")
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(report.statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(report.statusColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = report.diagnosis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme {
        DashboardScreen(repository = MockDataRepository(), onLogout = {})
    }
}
