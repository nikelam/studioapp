package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.ClientRecord
import com.example.data.model.ClientTier
import com.example.data.model.DashboardSummary
import com.example.data.model.FinanceTransaction
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinanceCashflowBar
import com.example.ui.components.StatusPill
import com.example.ui.components.clientTierColors
import com.example.ui.components.formatVnd
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald

@Composable
fun ClientsFinanceScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    summary: DashboardSummary,
    clientsState: UiState<List<ClientRecord>>,
    bookingsState: UiState<List<Booking>>,
    transactionsState: UiState<List<FinanceTransaction>>,
    onClientClick: (ClientRecord) -> Unit,
    onBookForClient: (ClientRecord) -> Unit,
    onCreateClient: () -> Unit,
    onTransactionClick: (FinanceTransaction) -> Unit,
    onCreateTransaction: () -> Unit
) {
    val tabs = listOf("Khách hàng", "Thu & Chi")
    var clientQuery by remember { mutableStateOf("") }

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
                    modifier = Modifier.testTag("clients_finance_tab_$idx")
                ) {
                    Text(label)
                }
            }
        }

        if (selectedTab == 0) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = clientQuery,
                    onValueChange = { clientQuery = it },
                    placeholder = { Text("Tìm tên hoặc số điện thoại khách...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (clientQuery.isNotEmpty()) {
                            IconButton(onClick = { clientQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Xoá tìm kiếm")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                when (clientsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(clientsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val bookings = (bookingsState as? UiState.Success)?.data.orEmpty()
                        val filtered = remember(clientsState.data, clientQuery) {
                            val q = clientQuery.trim().lowercase()
                            clientsState.data.filter {
                                q.isEmpty() || it.name.lowercase().contains(q) || it.phone.lowercase().contains(q)
                            }
                        }
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.Rounded.People,
                                    title = if (clientsState.data.isEmpty()) "Chưa có khách hàng" else "Không tìm thấy khách hàng",
                                    message = "Lưu thông tin liên hệ và phân hạng VIP / Doanh nghiệp để đặt lịch nhanh chóng.",
                                    actionLabel = if (clientsState.data.isEmpty()) "Thêm khách hàng" else null,
                                    onAction = if (clientsState.data.isEmpty()) onCreateClient else null,
                                    testTag = "clients_empty_add_button"
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filtered, key = { it.id }) { client ->
                                    val matchingBookings = bookings.filter {
                                        it.clientName.equals(client.name, ignoreCase = true) &&
                                            BookingStatus.fromRaw(it.status) != BookingStatus.CANCELLED
                                    }
                                    val totalSpend = matchingBookings.sumOf { it.totalAmount }
                                    ClientCard(
                                        client = client,
                                        bookingCount = matchingBookings.size,
                                        totalSpend = totalSpend,
                                        onClick = { onClientClick(client) },
                                        onBookNow = { onBookForClient(client) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (transactionsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(transactionsState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Tổng quan dòng tiền Studio",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatVnd(summary.netProfit),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (summary.netProfit >= 0L) StudioEmerald else StudioCoral,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FinanceCashflowBar(
                                        income = summary.totalIncome,
                                        expense = summary.totalExpense
                                    )
                                }
                            }
                        }

                        if (transactionsState.data.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.AccountBalanceWallet,
                                    title = "Sổ thu chi đang trống",
                                    message = "Ghi nhận tiền đặt cọc, thanh toán booking hoặc chi phí vận hành studio.",
                                    actionLabel = "Ghi nhận giao dịch",
                                    onAction = onCreateTransaction,
                                    testTag = "finance_empty_add_button"
                                )
                            }
                        } else {
                            items(transactionsState.data, key = { it.id }) { tx ->
                                TransactionCard(tx = tx, onClick = { onTransactionClick(tx) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientCard(
    client: ClientRecord,
    bookingCount: Int,
    totalSpend: Long,
    onClick: () -> Unit,
    onBookNow: () -> Unit
) {
    val tier = ClientTier.fromRaw(client.tier)
    val (bg, fg) = clientTierColors(tier)

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
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val contact = listOf(client.phone, client.email).filter { it.isNotBlank() }.joinToString(" • ")
                    if (contact.isNotBlank()) {
                        Text(
                            text = contact,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                StatusPill(label = tier.labelVi, containerColor = bg, contentColor = fg)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$bookingCount lịch • ${formatVnd(totalSpend)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onBookNow) {
                    Text("+ Đặt lịch")
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    tx: FinanceTransaction,
    onClick: () -> Unit
) {
    val type = TransactionType.fromRaw(tx.type)
    val cat = TransactionCategory.fromRaw(tx.category)
    val isIncome = type == TransactionType.INCOME
    val amountColor = if (isIncome) StudioEmerald else StudioCoral
    val prefix = if (isIncome) "+" else "-"

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cat.labelVi} • ${tx.dateText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$prefix${formatVnd(tx.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
