package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Bicycle
import com.example.data.Ride
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.ceil

class MainActivity : ComponentActivity() {
    private val viewModel: CycleViewModel by viewModels {
        CycleViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize default database records on first run
                LaunchedEffect(Unit) {
                    viewModel.insertDefaultsIfNeeded()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: CycleViewModel) {
    val bicycles by viewModel.bicycles.collectAsStateWithLifecycle()
    val rides by viewModel.rides.collectAsStateWithLifecycle()
    val activeRides by viewModel.activeRides.collectAsStateWithLifecycle()
    val tickerValue by viewModel.ticker.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("TRACKER") } // "TRACKER", "INVENTORY", "HISTORY", "ANALYTICS"
    var showAddBikeDialog by remember { mutableStateOf(false) }
    var activeReceiptToShow by remember { mutableStateOf<Ride?>(null) }

    var bikeToDelete by remember { mutableStateOf<Bicycle?>(null) }
    var rideToStop by remember { mutableStateOf<Ride?>(null) }
    var rideToCancel by remember { mutableStateOf<Ride?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            )
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == "TRACKER",
                    onClick = { currentTab = "TRACKER" },
                    icon = {
                        BadgedBox(badge = {
                            if (activeRides.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(activeRides.size.toString(), color = Color.White)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == "TRACKER") Icons.Filled.PlayCircleFilled else Icons.Outlined.PlayCircle,
                                contentDescription = "Active Tracking"
                            )
                        }
                    },
                    label = { Text("Active") },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_tracker")
                )

                NavigationBarItem(
                    selected = currentTab == "INVENTORY",
                    onClick = { currentTab = "INVENTORY" },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "INVENTORY") Icons.Filled.DirectionsBike else Icons.Outlined.DirectionsBike,
                            contentDescription = "Bicycles"
                        )
                    },
                    label = { Text("Bicycles") },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_inventory")
                )

                NavigationBarItem(
                    selected = currentTab == "HISTORY",
                    onClick = { currentTab = "HISTORY" },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "HISTORY") Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Ride History"
                        )
                    },
                    label = { Text("History") },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_history")
                )

                NavigationBarItem(
                    selected = currentTab == "ANALYTICS",
                    onClick = { currentTab = "ANALYTICS" },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "ANALYTICS") Icons.Filled.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics") },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_analytics")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == "INVENTORY") {
                FloatingActionButton(
                    onClick = { showAddBikeDialog = true },
                    modifier = Modifier.testTag("add_bike_fab"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bike")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // High-fidelity dynamic header with stats dashboard
            HeaderStatsDashboard(
                bicyclesCount = bicycles.size,
                activeRidesCount = activeRides.size,
                totalEarnings = rides.filter { it.endTime != null && it.finalCost != null }.sumOf { it.finalCost ?: 0.0 }
            )

            // Content switching based on tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "TRACKER" -> TrackerTabContent(
                        activeRides = activeRides,
                        tickerValue = tickerValue,
                        onPauseRide = { viewModel.pauseRide(it) },
                        onResumeRide = { viewModel.resumeRide(it) },
                        onStopRide = { rideToStop = it },
                        onCancelRide = { rideToCancel = it }
                    )
                    "INVENTORY" -> InventoryTabContent(
                        bicycles = bicycles,
                        onStartRide = { viewModel.startRide(it) },
                        onDeleteBike = { bikeToDelete = it },
                        onEditBike = { viewModel.updateBicycle(it) }
                    )
                    "HISTORY" -> HistoryTabContent(
                        completedRides = rides.filter { it.endTime != null }
                    )
                    "ANALYTICS" -> AnalyticsTabContent(
                        completedRides = rides.filter { it.endTime != null },
                        activeRides = activeRides,
                        bicycles = bicycles
                    )
                }
            }
        }
    }

    // Modal sheet/Dialogs
    if (showAddBikeDialog) {
        AddBicycleDialog(
            onDismiss = { showAddBikeDialog = false },
            onConfirm = { name, rate, interval, color ->
                viewModel.addBicycle(name, rate, interval, color)
                showAddBikeDialog = false
            }
        )
    }

    activeReceiptToShow?.let { originalRide ->
        // Retrieve updated completed ride or display snapshot
        val finishedRide = rides.find { it.id == originalRide.id && it.endTime != null }
        if (finishedRide != null) {
            ViewReceiptDialog(
                ride = finishedRide,
                onDismiss = { activeReceiptToShow = null }
            )
        }
    }

    // Confirm delete bike
    if (bikeToDelete != null) {
        AlertDialog(
            onDismissRequest = { bikeToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Bicycle?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${bikeToDelete?.name}' from your inventory? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        bikeToDelete?.let { viewModel.deleteBicycle(it) }
                        bikeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bikeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm stop ride
    if (rideToStop != null) {
        val durationSecs = if (rideToStop!!.isPaused) {
            rideToStop!!.accumulatedTimeSeconds
        } else {
            rideToStop!!.accumulatedTimeSeconds + ((tickerValue - rideToStop!!.startTime) / 1000)
        }
        val blockCost = CycleViewModel.calculateBlockCost(durationSecs, rideToStop!!.rate, rideToStop!!.intervalMinutes)

        AlertDialog(
            onDismissRequest = { rideToStop = null },
            icon = { Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Stop & Complete Ride?", fontWeight = FontWeight.Bold) },
            text = {
                val minsVal = durationSecs / 60
                val secsVal = durationSecs % 60
                Text("Do you want to stop the rent session for '${rideToStop?.bikeName}'?\n\nDuration: ${minsVal}m ${secsVal}s\nEstimated Fee: ₹${String.format("%.0f", blockCost)}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        rideToStop?.let {
                            viewModel.stopRide(it)
                            activeReceiptToShow = it
                        }
                        rideToStop = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop & Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToStop = null }) {
                    Text("Don't Stop")
                }
            }
        )
    }

    // Confirm cancel/discard ride
    if (rideToCancel != null) {
        AlertDialog(
            onDismissRequest = { rideToCancel = null },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
            title = { Text("Discard Ride Session?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to discard the active ride on '${rideToCancel?.bikeName}'? No history log will be recorded and no payment will be billed.") },
            confirmButton = {
                Button(
                    onClick = {
                        rideToCancel?.let { viewModel.cancelRide(it) }
                        rideToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToCancel = null }) {
                    Text("Resume Ride")
                }
            }
        )
    }
}

@Composable
fun HeaderStatsDashboard(
    bicyclesCount: Int,
    activeRidesCount: Int,
    totalEarnings: Double
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Track My Ride",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ride Monitor & Inventory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Bicycles",
                    value = "$bicyclesCount",
                    icon = Icons.Default.DirectionsBike,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                StatItem(
                    label = "Active Renting",
                    value = "$activeRidesCount",
                    icon = Icons.Default.PlayCircleOutline,
                    highlight = activeRidesCount > 0,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                StatItem(
                    label = "Total Billings",
                    value = String.format("₹%.0f", totalEarnings),
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// =====================================
// TRACKER TAB CONTENT
// =====================================
@Composable
fun TrackerTabContent(
    activeRides: List<Ride>,
    tickerValue: Long,
    onPauseRide: (Ride) -> Unit,
    onResumeRide: (Ride) -> Unit,
    onStopRide: (Ride) -> Unit,
    onCancelRide: (Ride) -> Unit
) {
    if (activeRides.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Rides",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Go to the 'Bicycles' tab and start a rent session to monitor duration and live billing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val overtimeRides = activeRides.filter { ride ->
            val elapsed = if (ride.isPaused) {
                ride.accumulatedTimeSeconds
            } else {
                ride.accumulatedTimeSeconds + ((tickerValue - ride.startTime) / 1000)
            }
            elapsed > (ride.intervalMinutes * 60)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (overtimeRides.isNotEmpty()) {
                item {
                    val namesList = overtimeRides.joinToString { it.bikeName }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("overtime_alert_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Time Exceeded Alert",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TIME LIMIT EXCEEDED ALERT!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${overtimeRides.size} active ride(s) have crossed their pricing schedule time limit block: $namesList. Extra fees are currently accumulating.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            items(activeRides, key = { it.id }) { ride ->
                ActiveRideCard(
                    ride = ride,
                    currentTimeMs = tickerValue,
                    onPause = { onPauseRide(ride) },
                    onResume = { onResumeRide(ride) },
                    onStop = { onStopRide(ride) },
                    onCancel = { onCancelRide(ride) }
                )
            }
        }
    }
}

@Composable
fun ActiveRideCard(
    ride: Ride,
    currentTimeMs: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    // Determine dynamic elapsed seconds
    val elapsedSeconds = if (ride.isPaused) {
        ride.accumulatedTimeSeconds
    } else {
        ride.accumulatedTimeSeconds + ((currentTimeMs - ride.startTime) / 1000)
    }

    val liveBlockCost = CycleViewModel.calculateBlockCost(elapsedSeconds, ride.rate, ride.intervalMinutes)
    val livePreciseCost = CycleViewModel.calculatePreciseCost(elapsedSeconds, ride.rate, ride.intervalMinutes)
    val isOverTime = elapsedSeconds > (ride.intervalMinutes * 60)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isOverTime) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .testTag("active_ride_card_${ride.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with cycle name + pricing blueprint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOverTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ride.bikeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOverTime) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format("Plan: ₹%.0f / %dmints", ride.rate, ride.intervalMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverTime) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Chronometer & Live billing rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Formatting time cleanly
                val hrs = elapsedSeconds / 3600
                val mins = (elapsedSeconds % 3600) / 60
                val secs = elapsedSeconds % 60
                val cronString = String.format("%02d:%02d:%02d", hrs, mins, secs)

                Column {
                    Text(
                        text = "RIDE DURATION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = cronString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isOverTime) {
                            MaterialTheme.colorScheme.error
                        } else if (ride.isPaused) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                // Billing readout
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LIVE BILLING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = String.format("₹%.2f", livePreciseCost),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Expanded price breakdown details
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pro-Rata Rate (Precise)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("₹%.2f", livePreciseCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Block schedule fee (Rounded)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("₹%.0f", liveBlockCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Overtime warning message panel
            if (isOverTime) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Time limit exceeded",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val extraSeconds = elapsedSeconds - (ride.intervalMinutes * 60)
                        val extraMins = extraSeconds / 60
                        val extraSecs = extraSeconds % 60
                        Text(
                            text = String.format(
                                "Overtime warning! Crossed the %d-min limit block by %02dm %02ds.",
                                ride.intervalMinutes, extraMins, extraSecs
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ride.isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_ride_${ride.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_ride_${ride.id}")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("cancel_ride_${ride.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Discard Session", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stop_ride_${ride.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// =====================================
// INVENTORY TAB CONTENT
// =====================================
@Composable
fun InventoryTabContent(
    bicycles: List<Bicycle>,
    onStartRide: (Bicycle) -> Unit,
    onDeleteBike: (Bicycle) -> Unit,
    onEditBike: (Bicycle) -> Unit
) {
    var bikeToEdit by remember { mutableStateOf<Bicycle?>(null) }

    if (bicycles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsBike,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Bicycles Yet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Click the 'Add Bike' button below to add your first bicycle with customized rent rates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(bicycles, key = { it.id }) { bike ->
                BicycleCard(
                    bicycle = bike,
                    onStartRide = { onStartRide(bike) },
                    onDelete = { onDeleteBike(bike) },
                    onEdit = { bikeToEdit = bike }
                )
            }
        }
    }

    bikeToEdit?.let { bike ->
        EditBicycleDialog(
            bicycle = bike,
            onDismiss = { bikeToEdit = null },
            onConfirm = { name, rate, interval, colorHex ->
                onEditBike(bike.copy(name = name, rate = rate, intervalMinutes = interval, colorHex = colorHex))
                bikeToEdit = null
            }
        )
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF3F51B5) // fallback Indigo
    }
}

@Composable
fun BicycleCard(
    bicycle: Bicycle,
    onStartRide: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val bikeColor = parseHexColor(bicycle.colorHex)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bike_card_${bicycle.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Color Label Indicator + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bicycle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit bike details",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove bike",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Info Display Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RENTAL RATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("₹%.0f / %dmints", bicycle.rate, bicycle.intervalMinutes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Status Badge
                val isRenting = bicycle.status == "Renting"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRenting) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = bicycle.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRenting) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartRide,
                enabled = bicycle.status != "Renting",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_ride_for_${bicycle.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                if (bicycle.status == "Renting") {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Currently Out on Ride")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start New Ride session")
                }
            }
        }
    }
}

// =====================================
// HISTORY TAB CONTENT
// =====================================
@Composable
fun HistoryTabContent(
    completedRides: List<Ride>
) {
    if (completedRides.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.HistoryToggleOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No History Logs Yet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your completed rentals, with timing metrics and calculated invoices, will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Archived Rides (${completedRides.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val sumTotal = completedRides.sumOf { it.finalCost ?: 0.0 }
                Text(
                    text = String.format("Total: ₹%.0f", sumTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(completedRides, key = { it.id }) { ride ->
                    HistoryRideCard(ride = ride)
                }
            }
        }
    }
}

@Composable
fun HistoryRideCard(ride: Ride) {
    val totalSeconds = if (ride.endTime != null) (ride.endTime - ride.startTime) / 1000 else 0L

    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    val formattedDuration = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m ${secs}s"

    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    val dateTimeStr = sdf.format(java.util.Date(ride.startTime))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ride.bikeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = String.format("₹%.0f", ride.finalCost ?: 0.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateTimeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("Plan: ₹%.0f / %dmints", ride.rate, ride.intervalMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// =====================================
// DIALOGS & SHEET HELPERS
// =====================================
@Composable
fun AddBicycleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("100") }
    var interval by remember { mutableStateOf("15") }
    var selectedColor by remember { mutableStateOf("#3F51B5") }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val colorsMap = listOf(
        "#3F51B5" to "Indigo",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#F44336" to "Red",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple"
    )

    val isFormModified = name.isNotBlank() || rate != "100" || interval != "15" || selectedColor != "#3F51B5"

    AlertDialog(
        onDismissRequest = {
            if (isFormModified) {
                showDiscardConfirm = true
            } else {
                onDismiss()
            }
        },
        title = { Text("Add New Bicycle", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bicycle Name") },
                    placeholder = { Text("e.g. Speedster 200") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bike_name"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it.filter { char -> char.isDigit() } },
                        label = { Text("Rate (₹)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bike_rate"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it.filter { char -> char.isDigit() } },
                        label = { Text("Interval (Mins)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_bike_interval"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Column {
                    Text(
                        text = "Identify Color Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsMap.forEach { colorPair ->
                            val isSelected = selectedColor == colorPair.first
                            val animatedSize by animateDpAsState(targetValue = if (isSelected) 36.dp else 28.dp)

                            Box(
                                modifier = Modifier
                                    .size(animatedSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(colorPair.first))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedColor = colorPair.first }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRate = rate.toDoubleOrNull() ?: 100.0
                    val finalInterval = interval.toIntOrNull() ?: 15
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), finalRate, finalInterval, selectedColor)
                    }
                },
                enabled = name.isNotBlank() && rate.isNotBlank() && interval.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_bike")
            ) {
                Text("Confirm & Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isFormModified) {
                        showDiscardConfirm = true
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Discard Unsaved Bicycle?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel? The information you entered will not be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
fun EditBicycleDialog(
    bicycle: Bicycle,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(bicycle.name) }
    var rate by remember { mutableStateOf(bicycle.rate.toInt().toString()) }
    var interval by remember { mutableStateOf(bicycle.intervalMinutes.toString()) }
    var selectedColor by remember { mutableStateOf(bicycle.colorHex) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val colorsMap = listOf(
        "#3F51B5" to "Indigo",
        "#009688" to "Teal",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#F44336" to "Red",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple"
    )

    val isFormModified = name != bicycle.name || rate != bicycle.rate.toInt().toString() || interval != bicycle.intervalMinutes.toString() || selectedColor != bicycle.colorHex

    AlertDialog(
        onDismissRequest = {
            if (isFormModified) {
                showDiscardConfirm = true
            } else {
                onDismiss()
            }
        },
        title = { Text("Edit Bicycle Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bicycle Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it.filter { char -> char.isDigit() } },
                        label = { Text("Rate (₹)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it.filter { char -> char.isDigit() } },
                        label = { Text("Interval (Mins)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Column {
                    Text(
                        text = "Identify Color Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsMap.forEach { colorPair ->
                            val isSelected = selectedColor == colorPair.first
                            val animatedSize by animateDpAsState(targetValue = if (isSelected) 36.dp else 28.dp)

                            Box(
                                modifier = Modifier
                                    .size(animatedSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(colorPair.first))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedColor = colorPair.first }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRate = rate.toDoubleOrNull() ?: bicycle.rate
                    val finalInterval = interval.toIntOrNull() ?: bicycle.intervalMinutes
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), finalRate, finalInterval, selectedColor)
                    }
                },
                enabled = name.isNotBlank() && rate.isNotBlank() && interval.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isFormModified) {
                        showDiscardConfirm = true
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            icon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Discard Unsaved Changes?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel? Any changes made to the bicycle details will not be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
fun ViewReceiptDialog(
    ride: Ride,
    onDismiss: () -> Unit
) {
    val durationSeconds = if (ride.endTime != null) (ride.endTime - ride.startTime) / 1000 else 0L

    val hrs = durationSeconds / 3600
    val mins = (durationSeconds % 3600) / 60
    val secs = durationSeconds % 60
    val durationFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs)

    val preciseCost = CycleViewModel.calculatePreciseCost(durationSeconds, ride.rate, ride.intervalMinutes)
    val finalCostStr = String.format("₹%.0f", ride.finalCost ?: 0.0)

    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
    val startStr = dateFormat.format(java.util.Date(ride.startTime))
    val endStr = ride.endTime?.let { dateFormat.format(java.util.Date(it)) } ?: "Ongoing"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Rental Invoice", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL AMOUNT DUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = finalCostStr,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Billing details list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReceiptDetailLine(label = "Bicycle Unit", value = ride.bikeName)
                    ReceiptDetailLine(label = "Rental Plan", value = String.format("₹%.0f per %d mins", ride.rate, ride.intervalMinutes))
                    ReceiptDetailLine(label = "Duration Mode", value = durationFormatted)

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    ReceiptDetailLine(label = "Rent Started", value = startStr)
                    ReceiptDetailLine(label = "Rent Stopped", value = endStr)

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    ReceiptDetailLine(
                        label = "Precise pro-rata cost",
                        value = String.format("₹%.2f", preciseCost)
                    )
                    ReceiptDetailLine(
                        label = "Block-schedule cost",
                        value = finalCostStr,
                        highlight = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dismiss_receipt")
            ) {
                Text("Close & Complete")
            }
        }
    )
}

@Composable
fun ReceiptDetailLine(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// =====================================
// ANALYTICS TAB CONTENT
// =====================================
data class DailyStat(val key: String, val label: String, val revenue: Double, val ridesCount: Int)
data class MonthlyStat(val key: String, val label: String, val revenue: Double, val ridesCount: Int)

@Composable
fun AnalyticsTabContent(
    completedRides: List<Ride>,
    activeRides: List<Ride>,
    bicycles: List<Bicycle>
) {
    if (completedRides.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Analytics Data Yet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Once you complete a rental session, real-time analytics for total rides, revenue tracking, and weekly/monthly daily cycles will be unlocked here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Collect metrics
        val totalRevenue = completedRides.sumOf { it.finalCost ?: 0.0 }
        val totalCompletedCount = completedRides.size
        val avgRevenuePerRide = if (totalCompletedCount > 0) totalRevenue / totalCompletedCount else 0.0

        val sdfDb = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val sdfLabel = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        val sdfMonthDb = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val sdfMonthLabel = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())

        // Compile Daily Trend
        val dailyGrouped = completedRides.groupBy { sdfDb.format(java.util.Date(it.startTime)) }
        val dailyTrend = dailyGrouped.map { (dateStr, ridesList) ->
            val parsedDate = try { sdfDb.parse(dateStr) } catch(e: Exception) { null } ?: java.util.Date()
            val displayLabel = sdfLabel.format(parsedDate)
            DailyStat(dateStr, displayLabel, ridesList.sumOf { it.finalCost ?: 0.0 }, ridesList.size)
        }.sortedBy { it.key }

        // Compile Monthly Trend
        val monthlyGrouped = completedRides.groupBy { sdfMonthDb.format(java.util.Date(it.startTime)) }
        val monthlyTrend = monthlyGrouped.map { (monthStr, ridesList) ->
            val parsedDate = try { sdfMonthDb.parse(monthStr) } catch(e: Exception) { null } ?: java.util.Date()
            val displayLabel = sdfMonthLabel.format(parsedDate)
            MonthlyStat(monthStr, displayLabel, ridesList.sumOf { it.finalCost ?: 0.0 }, ridesList.size)
        }.sortedBy { it.key }

        var isDailySelected by remember { mutableStateOf(true) }
        var selectedDayKey by remember { mutableStateOf<String?>(dailyTrend.lastOrNull()?.key) }
        var selectedMonthKey by remember { mutableStateOf<String?>(monthlyTrend.lastOrNull()?.key) }

        LaunchedEffect(dailyTrend) {
            if (selectedDayKey == null || dailyTrend.none { it.key == selectedDayKey }) {
                selectedDayKey = dailyTrend.lastOrNull()?.key
            }
        }
        LaunchedEffect(monthlyTrend) {
            if (selectedMonthKey == null || monthlyTrend.none { it.key == selectedMonthKey }) {
                selectedMonthKey = monthlyTrend.lastOrNull()?.key
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Dashboard
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        title = "Completed Rides",
                        value = totalCompletedCount.toString(),
                        subtitle = "${activeRides.size} active now",
                        icon = Icons.Default.DirectionsBike,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Earnings Tracker",
                        value = String.format("₹%.0f", totalRevenue),
                        subtitle = "Cumulative billing",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1.3f),
                        isHighlight = true
                    )
                    KpiMetricCard(
                        title = "Avg Ticket",
                        value = String.format("₹%.0f", avgRevenuePerRide),
                        subtitle = "Average per session",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            // Trend Visualizer Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Revenue Cyclical Analytics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Analyze daily and monthly yields",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDailySelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { isDailySelected = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Daily",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isDailySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (!isDailySelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { isDailySelected = false }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Monthly",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (!isDailySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isDailySelected) {
                            BarTimelineChart(
                                items = dailyTrend,
                                selectedKey = selectedDayKey,
                                onSelect = { selectedDayKey = it }
                            )
                        } else {
                            BarMonthTimelineChart(
                                items = monthlyTrend,
                                selectedKey = selectedMonthKey,
                                onSelect = { selectedMonthKey = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isDailySelected) {
                            val activeDay = dailyTrend.find { it.key == selectedDayKey }
                            if (activeDay != null) {
                                SelectedTrendDetailCard(
                                    label = activeDay.label,
                                    revenue = activeDay.revenue,
                                    ridesCount = activeDay.ridesCount,
                                    isMonthly = false
                                )
                            }
                        } else {
                            val activeMonth = monthlyTrend.find { it.key == selectedMonthKey }
                            if (activeMonth != null) {
                                SelectedTrendDetailCard(
                                    label = activeMonth.label,
                                    revenue = activeMonth.revenue,
                                    ridesCount = activeMonth.ridesCount,
                                    isMonthly = true
                                )
                            }
                        }
                    }
                }
            }

            // Bicycles Share distribution
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bicycle Yield Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total cumulative billing share per inventory unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        bicycles.forEach { bike ->
                            val bikeCompletedRides = completedRides.filter { it.bikeId == bike.id }
                            val totalBikeRevenue = bikeCompletedRides.sumOf { it.finalCost ?: 0.0 }
                            val shareFraction = if (totalRevenue > 0) totalBikeRevenue / totalRevenue else 0.0

                            BikeRevenueShareRow(
                                bikeName = bike.name,
                                colorHex = bike.colorHex,
                                revenue = totalBikeRevenue,
                                bookings = bikeCompletedRides.size,
                                fraction = shareFraction
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }

            // Smart business insights Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dynamic Optimization Hints",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val dayOfWeekSdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                        val dayDistribution = completedRides.groupBy { dayOfWeekSdf.format(java.util.Date(it.startTime)) }
                        val busiestDay = dayDistribution.maxByOrNull { it.value.size }?.key

                        val activeAndArchivedIds = (completedRides.map { it.bikeId } + activeRides.map { it.bikeId }).toSet()
                        val idleBikesList = bicycles.filter { !activeAndArchivedIds.contains(it.id) }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (busiestDay != null) {
                                Text(
                                    text = "• Peak Demand Timing: Rent operations show that $busiestDay carries your peak rental bookings count. Ensure all units are fully serviced before then.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (idleBikesList.isNotEmpty()) {
                                Text(
                                    text = "• Underutilized Inventory: ${idleBikesList.size} unit(s) (${idleBikesList.joinToString { it.name }}) have zero rental activity logs in this database. Try running lower testing pro-rata blocks to encourage rentals.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "• Elite Inventory Yield: Excellent! All ${bicycles.size} listed bicycles are successfully engaged, and have historical or active logs recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val durations = completedRides.map { (it.endTime!! - it.startTime) / 1000 }
                            if (durations.isNotEmpty()) {
                                val avgSecs = durations.average()
                                val avgMins = (avgSecs / 60).toInt()
                                Text(
                                    text = "• Session Habit: Customers typically average $avgMins minutes per ride booking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.TrendingUp,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun BarTimelineChart(
    items: List<DailyStat>,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    val maxRevenue = items.maxOfOrNull { it.revenue } ?: 1.0
    val activeMax = if (maxRevenue > 0) maxRevenue else 1.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data to plot", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { stat ->
                    val isSelected = stat.key == selectedKey
                    val calculatedHeight = ((stat.revenue / activeMax) * 110).toInt().coerceAtLeast(6)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(50.dp)
                            .clickable { onSelect(stat.key) }
                    ) {
                        Text(
                            text = String.format("₹%.0f", stat.revenue),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(calculatedHeight.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarMonthTimelineChart(
    items: List<MonthlyStat>,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    val maxRevenue = items.maxOfOrNull { it.revenue } ?: 1.0
    val activeMax = if (maxRevenue > 0) maxRevenue else 1.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data to plot", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { stat ->
                    val isSelected = stat.key == selectedKey
                    val calculatedHeight = ((stat.revenue / activeMax) * 110).toInt().coerceAtLeast(6)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .clickable { onSelect(stat.key) }
                    ) {
                        Text(
                            text = String.format("₹%.0f", stat.revenue),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(calculatedHeight.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedTrendDetailCard(
    label: String,
    revenue: Double,
    ridesCount: Int,
    isMonthly: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMonthly) "Month: $label" else "Date: $label",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$ridesCount completed rentable bookings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format("₹%.2f", revenue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun BikeRevenueShareRow(
    bikeName: String,
    colorHex: String,
    revenue: Double,
    bookings: Int,
    fraction: Double
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bikeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = String.format("₹%.0f (%d rentals)", revenue, bookings),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            val progressWidthFraction = fraction.toFloat().coerceIn(0f, 1f)
            if (progressWidthFraction > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressWidthFraction)
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}
