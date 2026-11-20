package com.example.ui.sheets

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.ClientRecord
import com.example.data.model.ClientTier
import com.example.data.model.FinanceTransaction
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.todayIsoDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientFormSheet(
    initialClient: ClientRecord?,
    onDismiss: () -> Unit,
    onSave: (ClientRecord) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialClient?.name ?: "") }
    var phone by remember { mutableStateOf(initialClient?.phone ?: "") }
    var email by remember { mutableStateOf(initialClient?.email ?: "") }
    var tier by remember {
        mutableStateOf(ClientTier.fromRaw(initialClient?.tier ?: ClientTier.REGULAR.value))
    }
    var notes by remember { mutableStateOf(initialClient?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDeleteDialog(
            title = "Xoá khách hàng?",
            message = "Hồ sơ \"${initialClient?.name.orEmpty()}\" sẽ bị xoá.",
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
                    text = if (initialClient == null) "Thêm khách hàng" else "Sửa hồ sơ khách hàng",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialClient != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xoá khách hàng",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Tên khách hàng / Thương hiệu *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_name_input")
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 30) phone = it },
                label = { Text("Số điện thoại / Zalo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Nhóm khách hàng",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClientTier.entries.forEach { item ->
                    FilterChip(
                        selected = tier == item,
                        onClick = { tier = item },
                        label = { Text(item.labelVi) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text("Ghi chú sở thích / yêu cầu riêng") },
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
                            errorMsg = "Vui lòng nhập tên khách hàng."
                        } else {
                            errorMsg = null
                            onSave(
                                (initialClient ?: ClientRecord()).copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    tier = tier.value,
                                    notes = notes.trim()
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_client_button")
                ) {
                    Text(if (initialClient == null) "Lưu khách hàng" else "Cập nhật")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    initialTx: FinanceTransaction?,
    onDismiss: () -> Unit,
    onSave: (FinanceTransaction) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialTx?.title ?: "") }
    var type by remember {
        mutableStateOf(TransactionType.fromRaw(initialTx?.type ?: TransactionType.INCOME.value))
    }
    var category by remember {
        mutableStateOf(TransactionCategory.fromRaw(initialTx?.category ?: TransactionCategory.BOOKING.value))
    }
    var amountText by remember {
        mutableStateOf(initialTx?.amount?.takeIf { it > 0L }?.toString() ?: "")
    }
    var dateText by remember {
        mutableStateOf(initialTx?.dateText?.ifBlank { todayIsoDate() } ?: todayIsoDate())
    }
    var notes by remember { mutableStateOf(initialTx?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        ConfirmDeleteDialog(
            title = "Xoá giao dịch?",
            message = "Giao dịch \"${initialTx?.title.orEmpty()}\" sẽ bị xoá khỏi sổ tài chính.",
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
                    text = if (initialTx == null) "Ghi nhận thu / chi" else "Sửa giao dịch",
                    style = MaterialTheme.typography.headlineSmall
                )
                if (initialTx != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xoá giao dịch",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = type == item,
                        onClick = {
                            type = item
                            category = if (item == TransactionType.INCOME) {
                                TransactionCategory.BOOKING
                            } else {
                                TransactionCategory.OPERATIONS
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        )
                    ) {
                        Text(if (item == TransactionType.INCOME) "Khoản Thu" else "Khoản Chi")
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("Nội dung giao dịch *") },
                placeholder = { Text("VD: Thanh toán gói chụp cưới") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(10) },
                    label = { Text("Số tiền (VNĐ) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("tx_amount_input")
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 20) dateText = it },
                    label = { Text("Ngày *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Danh mục",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransactionCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.labelVi) }
                    )
                }
            }

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
                        val amount = amountText.toLongOrNull() ?: 0L
                        when {
                            title.isBlank() -> errorMsg = "Vui lòng nhập nội dung giao dịch."
                            amount <= 0L -> errorMsg = "Số tiền phải lớn hơn 0."
                            dateText.isBlank() -> errorMsg = "Vui lòng nhập ngày giao dịch."
                            else -> {
                                errorMsg = null
                                onSave(
                                    (initialTx ?: FinanceTransaction()).copy(
                                        title = title.trim(),
                                        type = type.value,
                                        category = category.value,
                                        amount = amount,
                                        dateText = dateText.trim(),
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
                        .testTag("save_tx_button")
                ) {
                    Text(if (initialTx == null) "Lưu giao dịch" else "Cập nhật")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
