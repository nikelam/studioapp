package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EquipmentCategory
import com.example.data.model.EquipmentItem
import com.example.data.model.EquipmentStatus
import com.example.data.model.RoomCategory
import com.example.data.model.RoomStatus
import com.example.data.model.StudioRoom
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.equipmentStatusColors
import com.example.ui.components.formatVnd
import com.example.ui.components.roomStatusColors

@Composable
fun ResourcesScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    roomsState: UiState<List<StudioRoom>>,
    equipmentState: UiState<List<EquipmentItem>>,
    onRoomClick: (StudioRoom) -> Unit,
    onCycleRoomStatus: (StudioRoom) -> Unit,
    onCreateRoom: () -> Unit,
    onEquipmentClick: (EquipmentItem) -> Unit,
    onCycleEquipmentStatus: (EquipmentItem) -> Unit,
    onCreateEquipment: () -> Unit
) {
    val tabs = listOf("Phòng Studio", "Thiết bị")

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedTab == idx,
                    onClick = { onTabSelected(idx) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                    modifier = Modifier.testTag("resources_tab_$idx")
                ) {
                    Text(label)
                }
            }
        }

        if (selectedTab == 0) {
            when (roomsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(roomsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (roomsState.data.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Rounded.MeetingRoom,
                                title = "Chưa có phòng Studio",
                                message = "Thêm các phòng chụp, phòng thu âm hoặc sân khấu livestream của bạn.",
                                actionLabel = "Thêm phòng ngay",
                                onAction = onCreateRoom,
                                testTag = "resources_empty_room_button"
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(roomsState.data, key = { it.id }) { room ->
                                RoomCard(
                                    room = room,
                                    onClick = { onRoomClick(room) },
                                    onToggleStatus = { onCycleRoomStatus(room) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when (equipmentState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(equipmentState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (equipmentState.data.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Rounded.CameraAlt,
                                title = "Kho thiết bị đang trống",
                                message = "Quản lý máy ảnh, ống kính, đèn chiếu sáng, micro và tình trạng bảo trì.",
                                actionLabel = "Thêm thiết bị ngay",
                                onAction = onCreateEquipment,
                                testTag = "resources_empty_equipment_button"
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(equipmentState.data, key = { it.id }) { item ->
                                EquipmentCard(
                                    item = item,
                                    onClick = { onEquipmentClick(item) },
                                    onToggleStatus = { onCycleEquipmentStatus(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(
    room: StudioRoom,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val st = RoomStatus.fromRaw(room.status)
    val cat = RoomCategory.fromRaw(room.category)
    val (bg, fg) = roomStatusColors(st)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${cat.labelVi} • ${room.areaSqm} m²",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = st.labelVi, containerColor = bg, contentColor = fg)
            }

            if (room.amenities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    room.amenities.take(6).forEach { tag ->
                        StatusPill(
                            label = tag,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatVnd(room.hourlyRate)} / giờ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onToggleStatus) {
                    Text("Đổi trạng thái")
                }
            }
        }
    }
}

@Composable
private fun EquipmentCard(
    item: EquipmentItem,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val st = EquipmentStatus.fromRaw(item.status)
    val cat = EquipmentCategory.fromRaw(item.category)
    val (bg, fg) = equipmentStatusColors(st)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val serialText = if (item.serialCode.isNotBlank()) " • ${item.serialCode}" else ""
                    Text(
                        text = "${cat.labelVi} • SL: ${item.quantity}$serialText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(label = st.labelVi, containerColor = bg, contentColor = fg)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.dailyRate > 0L) "${formatVnd(item.dailyRate)} / ngày" else "Thiết bị nội bộ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onToggleStatus) {
                    Text("Đổi tình trạng")
                }
            }
        }
    }
}
