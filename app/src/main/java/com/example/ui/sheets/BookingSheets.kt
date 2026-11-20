package com.example.ui.sheets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ClientRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceType
import com.example.data.model.StudioRoom
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.bookingStatusColors
import com.example.ui.components.formatVnd
import com.example.ui.components.paymentStatusColors
import com.example.ui.components.todayIsoDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingFormSheet(
    initialBooking: Booking?,
    rooms: List<StudioRoom>,
    clients: List<ClientRecord>,
    prefillClient: ClientRecord? = null,
    onDismiss: () -> Unit,
    onSave: (Booking) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialBooking?.title ?: "") }
    var clientName by remember {
        mutableStateOf(initialBooking?.clientName ?: prefillClient?.name ?: "")
    }
    var clientPhone by remember {
        mutableStateOf(initialBooking?.clientPhone ?: prefillClient?.phone ?: "")
    }
    var roomName by remember {
        mutableStateOf(initialBooking?.roomName ?: rooms.firstOrNull()?.name ?: "")
    }
    var serviceType by remember {
        mutableStateOf(ServiceType.fromRaw(initialBooking?.serviceType ?: ServiceType.PHOTO.value))
    }
    var dateText by remember { mutableStateOf(initialBooking?.dateText?.ifBlank { todayIsoDate() } ?: todayIsoDate()) }
    var timeSlot by remember { mutableStateOf(initialBooking?.timeSlot ?: "09:00 - 12:00") }
    var totalText by remember {
        mutableStateOf(initialBooking?.totalAmount?.takeIf { it > 0L }?.toString() ?: "")
    }
    var depositText by remember {
        mutableStateOf(initialBooking?.depositAmount?.takeIf { it > 0L }?.toString() ?: "")
    }
    var paymentStatus by remember {
        mutableStateOf(PaymentStatus.fromRaw(initialBooking?.paymentStatus ?: PaymentStatus.UNPAID.value))
    }
    var status by remember {
        mutableStateOf(BookingStatus.fromRaw(initialBooking?.status ?: BookingStatus.CONFIRMED.value))
    }
    var notes by remember { mutableStateOf(initialBooking?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val timePresets = listOf("08:00 - 11:00", "09:00 - 12:00", "13:30 - 17:30", "18:00 - 21:00")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialBooking == null) "Đặt lịch Studio mới" else "Chỉnh sửa lịch hẹn",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("Tên buổi chụp / Dự án *") },
                placeholder = { Text("VD: Chụp Lookbook BST Thu") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_title_input")
            )

            if (clients.isNotEmpty()) {
                Text(
                    text = "Chọn nhanh khách hàng",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clients.take(8).forEach { client ->
                        SuggestionChip(
                            onClick = {
                                clientName = client.name
                                if (client.phone.isNotBlank()) clientPhone = client.phone
                            },
                            label = { Text(client.name) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { if (it.length <= 80) clientName = it },
                    label = { Text("Khách hàng *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("booking_client_input")
                )
                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { if (it.length <= 30) clientPhone = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (rooms.isNotEmpty()) {
                Text(
                    text = "Chọn phòng Studio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rooms.forEach { room ->
                        FilterChip(
                            selected = roomName == room.name,
                            onClick = { roomName = room.name },
                            label = { Text(room.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = roomName,
                onValueChange = { if (it.length <= 80) roomName = it },
                label = { Text("Phòng / Không gian Studio *") },
                placeholder = { Text("VD: Phòng Cyclorama A") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_room_input")
            )

            Text(
                text = "Loại dịch vụ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceType.entries.forEach { item ->
                    FilterChip(
                        selected = serviceType == item,
                        onClick = { serviceType = item },
                        label = { Text(item.labelVi) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 20) dateText = it },
                    label = { Text("Ngày (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { if (it.length <= 30) timeSlot = it },
                    label = { Text("Khung giờ *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timePresets.forEach { preset ->
                    SuggestionChip(
                        onClick = { timeSlot = preset },
                        label = { Text(preset) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = totalText,
                    onValueChange = { raw -> totalText = raw.filter { it.isDigit() }.take(10) },
                    label = { Text("Tổng tiền (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("booking_total_input")
                )
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { raw -> depositText = raw.filter { it.isDigit() }.take(10) },
                    label = { Text("Đã cọc (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Trạng thái thanh toán",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentStatus.entries.forEach { item ->
                    FilterChip(
                        selected = paymentStatus == item,
                        onClick = { paymentStatus = item },
                        label = { Text(item.labelVi) }
                    )
                }
            }

            Text(
                text = "Trạng thái lịch hẹn",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BookingStatus.entries.forEach { item ->
                    FilterChip(
                        selected = status == item,
                        onClick = { status = item },
                        label = { Text(item.labelVi) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text("Ghi chú yêu cầu thiết bị / ekip") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Huỷ")
                }
                Button(
                    onClick = {
                        val total = totalText.toLongOrNull() ?: 0L
                        val deposit = depositText.toLongOrNull() ?: 0L
                        when {
                            title.isBlank() -> validationError = "Vui lòng nhập tên buổi chụp hoặc dự án."
                            clientName.isBlank() -> validationError = "Vui lòng nhập tên khách hàng."
                            roomName.isBlank() -> validationError = "Vui lòng nhập hoặc chọn phòng studio."
                            dateText.isBlank() || timeSlot.isBlank() ->
                                validationError = "Vui lòng nhập ngày và khung giờ."
                            deposit > total ->
                                validationError = "Số tiền cọc không được lớn hơn tổng tiền."
                            else -> {
                                validationError = null
                                onSave(
                                    (initialBooking ?: Booking()).copy(
                                        title = title.trim(),
                                        clientName = clientName.trim(),
                                        clientPhone = clientPhone.trim(),
                                        roomName = roomName.trim(),
                                        serviceType = serviceType.value,
                                        dateText = dateText.trim(),
                                        timeSlot = timeSlot.trim(),
                                        totalAmount = total,
                                        depositAmount = deposit,
                                        paymentStatus = paymentStatus.value,
                                        status = status.value,
                                        notes = notes.trim()
                                    )
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_booking_button")
                ) {
                    Text(if (initialBooking == null) "Tạo lịch hẹn" else "Lưu thay đổi")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingDetailSheet(
    booking: Booking,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStatusChange: (BookingStatus) -> Unit,
    onRecordIncome: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusEnum = BookingStatus.fromRaw(booking.status)
    val payEnum = PaymentStatus.fromRaw(booking.paymentStatus)
    val serviceEnum = ServiceType.fromRaw(booking.serviceType)
    val (statusBg, statusFg) = bookingStatusColors(statusEnum)
    val (payBg, payFg) = paymentStatusColors(payEnum)
    val remaining = (booking.totalAmount - booking.depositAmount).coerceAtLeast(0L)

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Xoá lịch hẹn?",
            message = "Lịch hẹn \"${booking.title}\" sẽ bị xoá khỏi hệ thống.",
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = statusEnum.labelVi, containerColor = statusBg, contentColor = statusFg)
                        StatusPill(label = payEnum.labelVi, containerColor = payBg, contentColor = payFg)
                    }
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("delete_booking_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xoá lịch hẹn",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailLine("Khách hàng", booking.clientName)
                    if (booking.clientPhone.isNotBlank()) DetailLine("Điện thoại", booking.clientPhone)
                    DetailLine("Phòng Studio", booking.roomName)
                    DetailLine("Dịch vụ", serviceEnum.labelVi)
                    DetailLine("Thời gian", "${booking.dateText} • ${booking.timeSlot}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailLine("Tổng chi phí", formatVnd(booking.totalAmount))
                    DetailLine("Đã đặt cọc", formatVnd(booking.depositAmount))
                    DetailLine("Còn lại", formatVnd(remaining), emphasize = true)
                }
            }

            if (booking.notes.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ghi chú",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = booking.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (statusEnum.isTerminal) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Lịch hẹn đã ở trạng thái kết thúc (${statusEnum.labelVi}) và được khoá chỉnh sửa để bảo toàn dữ liệu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Cập nhật nhanh trạng thái",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BookingStatus.entries.forEach { target ->
                        FilterChip(
                            selected = statusEnum == target,
                            onClick = {
                                if (statusEnum != target) onStatusChange(target)
                            },
                            label = { Text(target.labelVi) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!statusEnum.isTerminal) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chỉnh sửa")
                    }
                }
                Button(
                    onClick = onRecordIncome,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("record_booking_income_button")
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ghi sổ thu")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
