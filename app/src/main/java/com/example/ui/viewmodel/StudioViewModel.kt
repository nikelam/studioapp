package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ClientRecord
import com.example.data.model.DashboardSummary
import com.example.data.model.EquipmentItem
import com.example.data.model.EquipmentStatus
import com.example.data.model.FinanceTransaction
import com.example.data.model.PaymentStatus
import com.example.data.model.RoomStatus
import com.example.data.model.StudioRoom
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.repository.StudioRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudioViewModel(
    private val repository: StudioRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private fun <T> Flow<List<T>>.toUiStateFlow(
        tagName: String,
        sortSelector: (List<T>) -> List<T>
    ): StateFlow<UiState<List<T>>> =
        this.map<List<T>, UiState<List<T>>> { UiState.Success(sortSelector(it)) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagName", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Không thể tải dữ liệu $tagName"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val bookingsState: StateFlow<UiState<List<Booking>>> =
        repository.observeBookings(currentUserId).toUiStateFlow("bookings") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    val roomsState: StateFlow<UiState<List<StudioRoom>>> =
        repository.observeRooms(currentUserId).toUiStateFlow("rooms") { list ->
            list.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

    val equipmentState: StateFlow<UiState<List<EquipmentItem>>> =
        repository.observeEquipment(currentUserId).toUiStateFlow("equipment") { list ->
            list.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

    val clientsState: StateFlow<UiState<List<ClientRecord>>> =
        repository.observeClients(currentUserId).toUiStateFlow("clients") { list ->
            list.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

    val transactionsState: StateFlow<UiState<List<FinanceTransaction>>> =
        repository.observeTransactions(currentUserId).toUiStateFlow("transactions") { list ->
            list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        bookingsState,
        roomsState,
        equipmentState,
        clientsState,
        transactionsState
    ) { bState, rState, eState, cState, tState ->
        val bookings = (bState as? UiState.Success)?.data.orEmpty()
        val rooms = (rState as? UiState.Success)?.data.orEmpty()
        val equipment = (eState as? UiState.Success)?.data.orEmpty()
        val clients = (cState as? UiState.Success)?.data.orEmpty()
        val transactions = (tState as? UiState.Success)?.data.orEmpty()

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val activeCount = bookings.count {
            val st = BookingStatus.fromRaw(it.status)
            st == BookingStatus.PENDING || st == BookingStatus.CONFIRMED || st == BookingStatus.IN_PROGRESS
        }
        val todayCount = bookings.count {
            it.dateText == todayStr && BookingStatus.fromRaw(it.status) != BookingStatus.CANCELLED
        }
        val pendingBalance = bookings.sumOf { booking ->
            val st = BookingStatus.fromRaw(booking.status)
            val pay = PaymentStatus.fromRaw(booking.paymentStatus)
            if (st != BookingStatus.CANCELLED && pay != PaymentStatus.PAID) {
                (booking.totalAmount - booking.depositAmount).coerceAtLeast(0L)
            } else {
                0L
            }
        }
        val totalIncome = transactions
            .filter { TransactionType.fromRaw(it.type) == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { TransactionType.fromRaw(it.type) == TransactionType.EXPENSE }
            .sumOf { it.amount }

        DashboardSummary(
            totalBookings = bookings.size,
            activeBookings = activeCount,
            todayBookings = todayCount,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            pendingReceivables = pendingBalance,
            availableRooms = rooms.count { RoomStatus.fromRaw(it.status) == RoomStatus.AVAILABLE },
            totalRooms = rooms.size,
            readyEquipmentCount = equipment.count { EquipmentStatus.fromRaw(it.status) == EquipmentStatus.READY },
            totalEquipmentCount = equipment.size,
            totalClients = clients.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardSummary()
    )

    private fun launchResult(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            val result = block()
            if (result.isSuccess) {
                _events.tryEmit(successMsg)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Thao tác thất bại, vui lòng thử lại."
                _events.tryEmit(err)
            }
        }
    }

    fun saveBooking(booking: Booking, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = if (booking.id.isBlank()) {
                repository.createBooking(currentUserId, booking)
            } else {
                repository.updateBooking(currentUserId, booking)
            }
            if (res.isSuccess) {
                _events.tryEmit(if (booking.id.isBlank()) "Đã tạo lịch hẹn mới" else "Đã cập nhật lịch hẹn")
                onComplete()
            } else {
                _events.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Không thể lưu lịch hẹn")
            }
        }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        launchResult("Đã chuyển trạng thái: ${status.labelVi}") {
            repository.updateBookingStatus(currentUserId, bookingId, status)
        }
    }

    fun recordBookingIncome(booking: Booking) {
        val paidAmount = when (PaymentStatus.fromRaw(booking.paymentStatus)) {
            PaymentStatus.PAID -> booking.totalAmount
            PaymentStatus.DEPOSIT -> booking.depositAmount
            PaymentStatus.UNPAID -> booking.depositAmount.takeIf { it > 0L } ?: booking.totalAmount
        }
        if (paidAmount <= 0L) {
            _events.tryEmit("Chưa có số tiền hợp lệ để ghi vào sổ thu chi")
            return
        }
        val category = if (PaymentStatus.fromRaw(booking.paymentStatus) == PaymentStatus.DEPOSIT) {
            TransactionCategory.DEPOSIT.value
        } else {
            TransactionCategory.BOOKING.value
        }
        val tx = FinanceTransaction(
            title = "Thu: ${booking.title}".take(100),
            type = TransactionType.INCOME.value,
            category = category,
            amount = paidAmount,
            dateText = booking.dateText.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            },
            notes = "Khách: ${booking.clientName} • Phòng: ${booking.roomName}".take(500)
        )
        launchResult("Đã ghi nhận khoản thu vào sổ tài chính") {
            repository.createTransaction(currentUserId, tx)
        }
    }

    fun deleteBooking(bookingId: String) {
        launchResult("Đã xoá lịch hẹn") {
            repository.deleteBooking(currentUserId, bookingId)
        }
    }

    fun saveRoom(room: StudioRoom, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = if (room.id.isBlank()) {
                repository.createRoom(currentUserId, room)
            } else {
                repository.updateRoom(currentUserId, room)
            }
            if (res.isSuccess) {
                _events.tryEmit(if (room.id.isBlank()) "Đã thêm phòng studio mới" else "Đã cập nhật thông tin phòng")
                onComplete()
            } else {
                _events.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Không thể lưu phòng studio")
            }
        }
    }

    fun updateRoomStatus(roomId: String, status: RoomStatus) {
        launchResult("Phòng chuyển sang: ${status.labelVi}") {
            repository.updateRoomStatus(currentUserId, roomId, status)
        }
    }

    fun deleteRoom(roomId: String) {
        launchResult("Đã xoá phòng studio") {
            repository.deleteRoom(currentUserId, roomId)
        }
    }

    fun saveEquipment(item: EquipmentItem, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = if (item.id.isBlank()) {
                repository.createEquipment(currentUserId, item)
            } else {
                repository.updateEquipment(currentUserId, item)
            }
            if (res.isSuccess) {
                _events.tryEmit(if (item.id.isBlank()) "Đã thêm thiết bị mới" else "Đã cập nhật thiết bị")
                onComplete()
            } else {
                _events.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Không thể lưu thiết bị")
            }
        }
    }

    fun updateEquipmentStatus(equipmentId: String, status: EquipmentStatus) {
        launchResult("Thiết bị: ${status.labelVi}") {
            repository.updateEquipmentStatus(currentUserId, equipmentId, status)
        }
    }

    fun deleteEquipment(equipmentId: String) {
        launchResult("Đã xoá thiết bị") {
            repository.deleteEquipment(currentUserId, equipmentId)
        }
    }

    fun saveClient(client: ClientRecord, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = if (client.id.isBlank()) {
                repository.createClient(currentUserId, client)
            } else {
                repository.updateClient(currentUserId, client)
            }
            if (res.isSuccess) {
                _events.tryEmit(if (client.id.isBlank()) "Đã thêm khách hàng mới" else "Đã cập nhật hồ sơ khách hàng")
                onComplete()
            } else {
                _events.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Không thể lưu khách hàng")
            }
        }
    }

    fun deleteClient(clientId: String) {
        launchResult("Đã xoá khách hàng") {
            repository.deleteClient(currentUserId, clientId)
        }
    }

    fun saveTransaction(tx: FinanceTransaction, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = if (tx.id.isBlank()) {
                repository.createTransaction(currentUserId, tx)
            } else {
                repository.updateTransaction(currentUserId, tx)
            }
            if (res.isSuccess) {
                _events.tryEmit(if (tx.id.isBlank()) "Đã ghi nhận giao dịch mới" else "Đã cập nhật giao dịch")
                onComplete()
            } else {
                _events.tryEmit(res.exceptionOrNull()?.localizedMessage ?: "Không thể lưu giao dịch")
            }
        }
    }

    fun deleteTransaction(txId: String) {
        launchResult("Đã xoá giao dịch") {
            repository.deleteTransaction(currentUserId, txId)
        }
    }

    private companion object {
        const val TAG = "StudioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
