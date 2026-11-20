package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.DashboardSummary
import com.example.data.model.RoomCategory
import com.example.data.model.RoomStatus
import com.example.data.model.StudioRoom
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.bookingStatusColors
import com.example.ui.components.formatVnd
import com.example.ui.components.roomStatusColors
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSky

@Composable
fun OverviewScreen(
    displayName: String,
    summary: DashboardSummary,
    bookingsState: UiState<List<Booking>>,
    roomsState: UiState<List<StudioRoom>>,
    onNewBooking: () -> Unit,
    onNewRoom: () -> Unit,
    onBookingClick: (Booking) -> Unit,
    onRoomClick: (StudioRoom) -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToResources: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Atmospheric Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero),
                    contentDescription = "StudioFlow Không gian điều hành",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xE60E1016),
                                    Color(0x990E1016),
                                    Color(0x4D0E1016)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = "Hôm nay: ${summary.todayBookings} buổi",
                            containerColor = StudioAmber.copy(alpha = 0.25f),
                            contentColor = Color(0xFFFFDEA6)
                        )
                        StatusPill(
                            label = "${summary.availableRooms}/${summary.totalRooms} phòng trống",
                            containerColor = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Xin chào, ${displayName.ifBlank { "Quản lý Studio" }}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = onNewBooking,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("overview_new_booking_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đặt lịch mới", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. KPI 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Lợi nhuận ròng",
                        value = formatVnd(summary.netProfit),
                        subtitle = "Thu ${formatVnd(summary.totalIncome)}",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accentColor = StudioEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Công nợ chờ thu",
                        value = formatVnd(summary.pendingReceivables),
                        subtitle = "${summary.activeBookings} lịch đang chạy",
                        icon = Icons.Rounded.Payments,
                        accentColor = StudioAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Lịch hẹn",
                        value = "${summary.totalBookings} buổi",
                        subtitle = "${summary.todayBookings} buổi hôm nay",
                        icon = Icons.Rounded.CalendarMonth,
                        accentColor = StudioSky,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Thiết bị & Khách",
                        value = "${summary.readyEquipmentCount}/${summary.totalEquipmentCount} TB",
                        subtitle = "${summary.totalClients} khách hàng",
                        icon = Icons.Rounded.CameraAlt,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Studio Rooms Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = "Trạng thái phòng Studio",
                    actionLabel = if ((roomsState as? UiState.Success)?.data?.isNotEmpty() == true) "Xem tất cả" else null,
                    onActionClick = onNavigateToResources
                )
                when (roomsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    is UiState.Error -> {
                        Text(roomsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        if (roomsState.data.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Rounded.MeetingRoom,
                                title = "Chưa có phòng studio",
                                message = "Thêm phòng chụp, thu âm hoặc quay phim để theo dõi tình trạng trống.",
                                actionLabel = "Thêm phòng đầu tiên",
                                onAction = onNewRoom,
                                testTag = "overview_add_room_button"
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(roomsState.data, key = { it.id }) { room ->
                                    val st = RoomStatus.fromRaw(room.status)
                                    val (bg, fg) = roomStatusColors(st)
                                    val cat = RoomCategory.fromRaw(room.category)
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .width(196.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(18.dp)
                                            )
                                            .clickable { onRoomClick(room) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StatusPill(label = st.labelVi, containerColor = bg, contentColor = fg)
                                            Text(
                                                text = room.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${cat.labelVi} • ${formatVnd(room.hourlyRate)}/h",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Upcoming Bookings
        item {
            SectionHeader(
                title = "Lịch hẹn gần đây",
                actionLabel = "Quản lý lịch",
                onActionClick = onNavigateToBookings
            )
        }

        when (bookingsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(bookingsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val activeList = bookingsState.data.filter {
                    val st = BookingStatus.fromRaw(it.status)
                    st != BookingStatus.CANCELLED
                }.take(5)

                if (activeList.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Schedule,
                            title = "Chưa có lịch hẹn nào",
                            message = "Bắt đầu tạo lịch chụp, thu âm hoặc livestream để theo dõi tiến độ và tiền cọc.",
                            actionLabel = "Tạo lịch hẹn ngay",
                            onAction = onNewBooking,
                            testTag = "overview_empty_booking_button"
                        )
                    }
                } else {
                    items(activeList, key = { it.id }) { booking ->
                        CompactBookingRow(booking = booking, onClick = { onBookingClick(booking) })
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun CompactBookingRow(
    booking: Booking,
    onClick: () -> Unit
) {
    val st = BookingStatus.fromRaw(booking.status)
    val (bg, fg) = bookingStatusColors(st)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = booking.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${booking.clientName} • ${booking.roomName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${booking.dateText} | ${booking.timeSlot}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(label = st.labelVi, containerColor = bg, contentColor = fg)
                Text(
                    text = formatVnd(booking.totalAmount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
