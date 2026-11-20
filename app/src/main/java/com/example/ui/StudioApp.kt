package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.ClientRecord
import com.example.data.model.EquipmentItem
import com.example.data.model.EquipmentStatus
import com.example.data.model.FinanceTransaction
import com.example.data.model.RoomStatus
import com.example.data.model.StudioRoom
import com.example.data.model.UiState
import com.example.data.repository.StudioRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.screens.BookingsScreen
import com.example.ui.screens.ClientsFinanceScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ResourcesScreen
import com.example.ui.sheets.BookingDetailSheet
import com.example.ui.sheets.BookingFormSheet
import com.example.ui.sheets.ClientFormSheet
import com.example.ui.sheets.EquipmentFormSheet
import com.example.ui.sheets.RoomFormSheet
import com.example.ui.sheets.TransactionFormSheet
import com.example.ui.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_BOOKINGS = "bookings"
const val ROUTE_RESOURCES = "resources"
const val ROUTE_CLIENTS_FINANCE = "clients_finance"

private data class StudioNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val studioDestinations = listOf(
    StudioNavDestination(
        route = ROUTE_OVERVIEW,
        label = "Tổng quan",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    StudioNavDestination(
        route = ROUTE_BOOKINGS,
        label = "Lịch hẹn",
        selectedIcon = Icons.Rounded.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    StudioNavDestination(
        route = ROUTE_RESOURCES,
        label = "Tài nguyên",
        selectedIcon = Icons.Rounded.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    ),
    StudioNavDestination(
        route = ROUTE_CLIENTS_FINANCE,
        label = "Thu chi",
        selectedIcon = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            StudioMainContent(currentUser = user, currentUserId = user.uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioMainContent(
    currentUser: FirebaseUser,
    currentUserId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(StudioRepository(db), currentUserId)
            }
        }
    )

    val bookingsState by viewModel.bookingsState.collectAsStateWithLifecycle()
    val roomsState by viewModel.roomsState.collectAsStateWithLifecycle()
    val equipmentState by viewModel.equipmentState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_OVERVIEW

    var resourcesTab by rememberSaveable { mutableIntStateOf(0) }
    var clientsFinanceTab by rememberSaveable { mutableIntStateOf(0) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Modal sheet states
    var editingBooking by remember { mutableStateOf<Booking?>(null) }
    var showBookingForm by remember { mutableStateOf(false) }
    var prefillBookingClient by remember { mutableStateOf<ClientRecord?>(null) }
    var detailBookingId by remember { mutableStateOf<String?>(null) }

    var editingRoom by remember { mutableStateOf<StudioRoom?>(null) }
    var showRoomForm by remember { mutableStateOf(false) }

    var editingEquipment by remember { mutableStateOf<EquipmentItem?>(null) }
    var showEquipmentForm by remember { mutableStateOf(false) }

    var editingClient by remember { mutableStateOf<ClientRecord?>(null) }
    var showClientForm by remember { mutableStateOf(false) }

    var editingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
    var showTransactionForm by remember { mutableStateOf(false) }

    val roomsList = (roomsState as? UiState.Success)?.data.orEmpty()
    val clientsList = (clientsState as? UiState.Success)?.data.orEmpty()
    val bookingsList = (bookingsState as? UiState.Success)?.data.orEmpty()

    val detailBooking = detailBookingId?.let { id -> bookingsList.firstOrNull { it.id == id } }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Tài khoản StudioFlow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = currentUser.displayName ?: "Quản lý Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!currentUser.email.isNullOrBlank()) {
                        Text(
                            text = currentUser.email!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Dữ liệu đang đồng bộ bảo mật qua Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        signOut(credentialManager, scope)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = studioDestinations.firstOrNull { it.route == currentRoute }?.label ?: "StudioFlow",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.testTag("profile_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = currentUser.displayName?.firstOrNull()?.uppercaseChar()
                                if (initial != null) {
                                    Text(
                                        text = initial.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Tài khoản",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        val currentDestination = navBackStackEntry?.destination
                        studioDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (currentRoute) {
                            ROUTE_OVERVIEW, ROUTE_BOOKINGS -> {
                                editingBooking = null
                                prefillBookingClient = null
                                showBookingForm = true
                            }
                            ROUTE_RESOURCES -> {
                                if (resourcesTab == 0) {
                                    editingRoom = null
                                    showRoomForm = true
                                } else {
                                    editingEquipment = null
                                    showEquipmentForm = true
                                }
                            }
                            ROUTE_CLIENTS_FINANCE -> {
                                if (clientsFinanceTab == 0) {
                                    editingClient = null
                                    showClientForm = true
                                } else {
                                    editingTransaction = null
                                    showTransactionForm = true
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("primary_fab")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Thêm mới"
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        val currentDestination = navBackStackEntry?.destination
                        studioDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_OVERVIEW,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .widthIn(max = 900.dp)
                    ) {
                        composable(ROUTE_OVERVIEW) {
                            OverviewScreen(
                                displayName = currentUser.displayName ?: "",
                                summary = summary,
                                bookingsState = bookingsState,
                                roomsState = roomsState,
                                onNewBooking = {
                                    editingBooking = null
                                    prefillBookingClient = null
                                    showBookingForm = true
                                },
                                onNewRoom = {
                                    editingRoom = null
                                    showRoomForm = true
                                },
                                onBookingClick = { booking -> detailBookingId = booking.id },
                                onRoomClick = { room ->
                                    editingRoom = room
                                    showRoomForm = true
                                },
                                onNavigateToBookings = { navigateToTab(ROUTE_BOOKINGS) },
                                onNavigateToResources = { navigateToTab(ROUTE_RESOURCES) }
                            )
                        }

                        composable(ROUTE_BOOKINGS) {
                            BookingsScreen(
                                bookingsState = bookingsState,
                                onBookingClick = { booking -> detailBookingId = booking.id },
                                onCreateBooking = {
                                    editingBooking = null
                                    prefillBookingClient = null
                                    showBookingForm = true
                                }
                            )
                        }

                        composable(ROUTE_RESOURCES) {
                            ResourcesScreen(
                                selectedTab = resourcesTab,
                                onTabSelected = { resourcesTab = it },
                                roomsState = roomsState,
                                equipmentState = equipmentState,
                                onRoomClick = { room ->
                                    editingRoom = room
                                    showRoomForm = true
                                },
                                onCycleRoomStatus = { room ->
                                    val next = when (RoomStatus.fromRaw(room.status)) {
                                        RoomStatus.AVAILABLE -> RoomStatus.OCCUPIED
                                        RoomStatus.OCCUPIED -> RoomStatus.MAINTENANCE
                                        RoomStatus.MAINTENANCE -> RoomStatus.AVAILABLE
                                    }
                                    viewModel.updateRoomStatus(room.id, next)
                                },
                                onCreateRoom = {
                                    editingRoom = null
                                    showRoomForm = true
                                },
                                onEquipmentClick = { item ->
                                    editingEquipment = item
                                    showEquipmentForm = true
                                },
                                onCycleEquipmentStatus = { item ->
                                    val next = when (EquipmentStatus.fromRaw(item.status)) {
                                        EquipmentStatus.READY -> EquipmentStatus.IN_USE
                                        EquipmentStatus.IN_USE -> EquipmentStatus.MAINTENANCE
                                        EquipmentStatus.MAINTENANCE -> EquipmentStatus.READY
                                    }
                                    viewModel.updateEquipmentStatus(item.id, next)
                                },
                                onCreateEquipment = {
                                    editingEquipment = null
                                    showEquipmentForm = true
                                }
                            )
                        }

                        composable(ROUTE_CLIENTS_FINANCE) {
                            ClientsFinanceScreen(
                                selectedTab = clientsFinanceTab,
                                onTabSelected = { clientsFinanceTab = it },
                                summary = summary,
                                clientsState = clientsState,
                                bookingsState = bookingsState,
                                transactionsState = transactionsState,
                                onClientClick = { client ->
                                    editingClient = client
                                    showClientForm = true
                                },
                                onBookForClient = { client ->
                                    editingBooking = null
                                    prefillBookingClient = client
                                    showBookingForm = true
                                },
                                onCreateClient = {
                                    editingClient = null
                                    showClientForm = true
                                },
                                onTransactionClick = { tx ->
                                    editingTransaction = tx
                                    showTransactionForm = true
                                },
                                onCreateTransaction = {
                                    editingTransaction = null
                                    showTransactionForm = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Modal Sheets ---
    if (detailBooking != null) {
        BookingDetailSheet(
            booking = detailBooking,
            onDismiss = { detailBookingId = null },
            onEdit = {
                editingBooking = detailBooking
                prefillBookingClient = null
                detailBookingId = null
                showBookingForm = true
            },
            onStatusChange = { newStatus ->
                viewModel.updateBookingStatus(detailBooking.id, newStatus)
            },
            onRecordIncome = {
                viewModel.recordBookingIncome(detailBooking)
            },
            onDelete = {
                viewModel.deleteBooking(detailBooking.id)
                detailBookingId = null
            }
        )
    }

    if (showBookingForm) {
        BookingFormSheet(
            initialBooking = editingBooking,
            rooms = roomsList,
            clients = clientsList,
            prefillClient = prefillBookingClient,
            onDismiss = {
                showBookingForm = false
                editingBooking = null
                prefillBookingClient = null
            },
            onSave = { updated ->
                viewModel.saveBooking(updated) {
                    showBookingForm = false
                    editingBooking = null
                    prefillBookingClient = null
                }
            }
        )
    }

    if (showRoomForm) {
        RoomFormSheet(
            initialRoom = editingRoom,
            onDismiss = {
                showRoomForm = false
                editingRoom = null
            },
            onSave = { room ->
                viewModel.saveRoom(room) {
                    showRoomForm = false
                    editingRoom = null
                }
            },
            onDelete = editingRoom?.let { target ->
                {
                    viewModel.deleteRoom(target.id)
                    showRoomForm = false
                    editingRoom = null
                }
            }
        )
    }

    if (showEquipmentForm) {
        EquipmentFormSheet(
            initialItem = editingEquipment,
            onDismiss = {
                showEquipmentForm = false
                editingEquipment = null
            },
            onSave = { item ->
                viewModel.saveEquipment(item) {
                    showEquipmentForm = false
                    editingEquipment = null
                }
            },
            onDelete = editingEquipment?.let { target ->
                {
                    viewModel.deleteEquipment(target.id)
                    showEquipmentForm = false
                    editingEquipment = null
                }
            }
        )
    }

    if (showClientForm) {
        ClientFormSheet(
            initialClient = editingClient,
            onDismiss = {
                showClientForm = false
                editingClient = null
            },
            onSave = { client ->
                viewModel.saveClient(client) {
                    showClientForm = false
                    editingClient = null
                }
            },
            onDelete = editingClient?.let { target ->
                {
                    viewModel.deleteClient(target.id)
                    showClientForm = false
                    editingClient = null
                }
            }
        )
    }

    if (showTransactionForm) {
        TransactionFormSheet(
            initialTx = editingTransaction,
            onDismiss = {
                showTransactionForm = false
                editingTransaction = null
            },
            onSave = { tx ->
                viewModel.saveTransaction(tx) {
                    showTransactionForm = false
                    editingTransaction = null
                }
            },
            onDelete = editingTransaction?.let { target ->
                {
                    viewModel.deleteTransaction(target.id)
                    showTransactionForm = false
                    editingTransaction = null
                }
            }
        )
    }
}
