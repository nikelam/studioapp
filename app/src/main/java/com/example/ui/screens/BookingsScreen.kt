package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceType
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.bookingStatusColors
import com.example.ui.components.formatVnd
import com.example.ui.components.paymentStatusColors

@Composable
fun BookingsScreen(
    bookingsState: UiState<List<Booking>>,
    onBookingClick: (Booking) -> Unit,
    onCreateBooking: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<BookingStatus?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm tên dự án, khách hàng, phòng...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Xoá tìm kiếm")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bookings_search_input")
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Tất cả") }
                )
                BookingStatus.entries.forEach { st ->
                    FilterChip(
                        selected = selectedFilter == st,
                        onClick = { selectedFilter = if (selectedFilter == st) null else st },
                        label = { Text(st.labelVi) }
                    )
                }
            }
        }

        when (bookingsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(bookingsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val filtered = remember(bookingsState.data, searchQuery, selectedFilter) {
                    bookingsState.data.filter { item ->
                        val matchesStatus = selectedFilter == null || BookingStatus.fromRaw(item.status) == selectedFilter
                        val q = searchQuery.trim().lowercase()
                        val matchesQuery = q.isEmpty() ||
                            item.title.lowercase().contains(q) ||
                            item.clientName.lowercase().contains(q) ||
                            item.roomName.lowercase().contains(q)
                        matchesStatus && matchesQuery
                    }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Rounded.CalendarMonth,
                            title = if (bookingsState.data.isEmpty()) "Chưa có lịch hẹn" else "Không tìm thấy lịch phù hợp",
                            message = if (bookingsState.data.isEmpty()) {
                                "Tạo lịch hẹn đầu tiên để theo dõi khung giờ phòng, dịch vụ và đặt cọc."
                            } else {
                                "Thử đổi từ khoá tìm kiếm hoặc bộ lọc trạng thái."
                            },
                            actionLabel = if (bookingsState.data.isEmpty()) "Đặt lịch mới" else null,
                            onAction = if (bookingsState.data.isEmpty()) onCreateBooking else null,
                            testTag = "bookings_empty_create"
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { booking ->
                            BookingFullCard(booking = booking, onClick = { onBookingClick(booking) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingFullCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val st = BookingStatus.fromRaw(booking.status)
    val pay = PaymentStatus.fromRaw(booking.paymentStatus)
    val service = ServiceType.fromRaw(booking.serviceType)
    val (stBg, stFg) = bookingStatusColors(st)
    val (payBg, payFg) = paymentStatusColors(pay)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("booking_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = booking.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${booking.clientName} • ${booking.roomName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(label = st.labelVi, containerColor = stBg, contentColor = stFg)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        label = service.labelVi,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    StatusPill(label = pay.labelVi, containerColor = payBg, contentColor = payFg)
                }
                Text(
                    text = formatVnd(booking.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${booking.dateText} • ${booking.timeSlot}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (booking.depositAmount > 0L) {
                    Text(
                        text = "Cọc: ${formatVnd(booking.depositAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
