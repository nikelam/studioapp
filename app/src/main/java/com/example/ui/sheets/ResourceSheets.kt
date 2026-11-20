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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.EquipmentCategory
import com.example.data.model.EquipmentItem
import com.example.data.model.EquipmentStatus
import com.example.data.model.RoomCategory
import com.example.data.model.RoomStatus
import com.example.data.model.StudioRoom
import com.example.ui.components.ConfirmDeleteDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomFormSheet(
    initialRoom: StudioRoom?,
    onDismiss: () -> Unit,
    onSave: (StudioRoom) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialRoom?.name ?: "") }
    var category by remember {
        mutableStateOf(RoomCategory.fromRaw(initialRoom?.category ?: RoomCategory.PHOTO.value))
    }
    var hourlyRateText by remember {
        mutableStateOf(initialRoom?.hourlyRate?.takeIf { it > 0L }?.toString() ?: "")
    }
    var areaText by remember {
        mutableStateOf(initialRoom?.areaSqm?.takeIf { it > 0L }?.toString() ?: "")
    }
    var status by remember {
        mutableStateOf(RoomStatus.fromRaw(initialRoom?.status ?: RoomStatus.AVAILABLE.value))
    }
    var amenitiesText by remember {
        mutableStateOf(initialRoom?.amenities?.joinToString(", ") ?: "")
    }
    var notes by remember { mutableStateOf(initialRoom?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDeleteDialog(
            title = "Xoá phòng studio?",
            message = "Phòng \"${initialRoom?.name.orEmpty()}\" sẽ bị xoá khỏi danh sách.",
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
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialRoom == null) "Thêm phòng Studio" else "Chỉnh sửa phòng",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialRoom != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xoá phòng",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Tên phòng Studio *") },
                placeholder = { Text("VD: Phòng Cyclorama A") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("room_name_input")
            )

            Text(
                text = "Loại phòng",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoomCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.labelVi) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hourlyRateText,
                    onValueChange = { hourlyRateText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Giá thuê / giờ (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = areaText,
                    onValueChange = { areaText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Diện tích (m²)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Trạng thái hiện tại",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoomStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.labelVi) }
                    )
                }
            }

            OutlinedTextField(
                value = amenitiesText,
                onValueChange = { amenitiesText = it },
                label = { Text("Tiện ích (ngăn cách bởi dấu phẩy)") },
                placeholder = { Text("VD: Phông vô cực 6m, Máy lạnh, Phòng thay đồ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text("Ghi chú") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                        if (name.isBlank()) {
                            errorMsg = "Vui lòng nhập tên phòng studio."
                        } else {
                            errorMsg = null
                            val parsedAmenities = amenitiesText
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(10)
                            onSave(
                                (initialRoom ?: StudioRoom()).copy(
                                    name = name.trim(),
                                    category = category.value,
                                    hourlyRate = hourlyRateText.toLongOrNull() ?: 0L,
                                    areaSqm = areaText.toLongOrNull() ?: 0L,
                                    status = status.value,
                                    amenities = parsedAmenities,
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_room_button")
                ) {
                    Text(if (initialRoom == null) "Thêm phòng" else "Lưu thay đổi")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EquipmentFormSheet(
    initialItem: EquipmentItem?,
    onDismiss: () -> Unit,
    onSave: (EquipmentItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var category by remember {
        mutableStateOf(EquipmentCategory.fromRaw(initialItem?.category ?: EquipmentCategory.CAMERA.value))
    }
    var serialCode by remember { mutableStateOf(initialItem?.serialCode ?: "") }
    var dailyRateText by remember {
        mutableStateOf(initialItem?.dailyRate?.takeIf { it > 0L }?.toString() ?: "")
    }
    var quantityText by remember {
        mutableStateOf((initialItem?.quantity ?: 1L).toString())
    }
    var status by remember {
        mutableStateOf(EquipmentStatus.fromRaw(initialItem?.status ?: EquipmentStatus.READY.value))
    }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDeleteDialog(
            title = "Xoá thiết bị?",
            message = "Thiết bị \"${initialItem?.name.orEmpty()}\" sẽ bị xoá khỏi kho.",
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
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem == null) "Thêm thiết bị Studio" else "Chỉnh sửa thiết bị",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialItem != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xoá thiết bị",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Tên thiết bị / Model *") },
                placeholder = { Text("VD: Đèn Godox SL200 III") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("equipment_name_input")
            )

            Text(
                text = "Phân loại",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EquipmentCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.labelVi) }
                    )
                }
            }

            OutlinedTextField(
                value = serialCode,
                onValueChange = { if (it.length <= 40) serialCode = it },
                label = { Text("Mã tài sản / Serial") },
                placeholder = { Text("VD: CAM-01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dailyRateText,
                    onValueChange = { dailyRateText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Giá thuê/ngày (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Số lượng *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Tình trạng",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EquipmentStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.labelVi) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text("Ghi chú bảo trì / phụ kiện đi kèm") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                        val qty = quantityText.toLongOrNull() ?: 0L
                        when {
                            name.isBlank() -> errorMsg = "Vui lòng nhập tên thiết bị."
                            qty !in 1L..1000L -> errorMsg = "Số lượng phải từ 1 đến 1000."
                            else -> {
                                errorMsg = null
                                onSave(
                                    (initialItem ?: EquipmentItem()).copy(
                                        name = name.trim(),
                                        category = category.value,
                                        serialCode = serialCode.trim(),
                                        dailyRate = dailyRateText.toLongOrNull() ?: 0L,
                                        quantity = qty,
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
                        .testTag("save_equipment_button")
                ) {
                    Text(if (initialItem == null) "Thêm thiết bị" else "Lưu thay đổi")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
