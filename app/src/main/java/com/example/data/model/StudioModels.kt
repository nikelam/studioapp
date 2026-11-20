package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ServiceType(val value: String, val labelVi: String) {
    PHOTO("photo", "Chụp ảnh"),
    VIDEO("video", "Quay phim"),
    AUDIO("audio", "Thu âm"),
    PODCAST("podcast", "Podcast"),
    LIVESTREAM("livestream", "Livestream"),
    OTHER("other", "Khác");

    companion object {
        fun fromRaw(raw: String): ServiceType =
            entries.firstOrNull { it.value == raw } ?: PHOTO
    }
}

enum class PaymentStatus(val value: String, val labelVi: String) {
    UNPAID("unpaid", "Chưa cọc"),
    DEPOSIT("deposit", "Đã cọc"),
    PAID("paid", "Đã thanh toán");

    companion object {
        fun fromRaw(raw: String): PaymentStatus =
            entries.firstOrNull { it.value == raw } ?: UNPAID
    }
}

enum class BookingStatus(val value: String, val labelVi: String) {
    PENDING("pending", "Chờ xác nhận"),
    CONFIRMED("confirmed", "Đã xác nhận"),
    IN_PROGRESS("in_progress", "Đang thực hiện"),
    COMPLETED("completed", "Hoàn thành"),
    CANCELLED("cancelled", "Đã huỷ");

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    companion object {
        fun fromRaw(raw: String): BookingStatus =
            entries.firstOrNull { it.value == raw } ?: PENDING
    }
}

enum class RoomCategory(val value: String, val labelVi: String) {
    PHOTO("photo", "Chụp ảnh"),
    VIDEO("video", "Quay phim"),
    AUDIO("audio", "Thu âm"),
    MULTIPURPOSE("multipurpose", "Đa năng");

    companion object {
        fun fromRaw(raw: String): RoomCategory =
            entries.firstOrNull { it.value == raw } ?: MULTIPURPOSE
    }
}

enum class RoomStatus(val value: String, val labelVi: String) {
    AVAILABLE("available", "Sẵn sàng"),
    OCCUPIED("occupied", "Đang sử dụng"),
    MAINTENANCE("maintenance", "Bảo trì");

    companion object {
        fun fromRaw(raw: String): RoomStatus =
            entries.firstOrNull { it.value == raw } ?: AVAILABLE
    }
}

enum class EquipmentCategory(val value: String, val labelVi: String) {
    CAMERA("camera", "Máy ảnh"),
    LENS("lens", "Ống kính"),
    LIGHTING("lighting", "Ánh sáng"),
    AUDIO("audio", "Âm thanh"),
    ACCESSORY("accessory", "Phụ kiện");

    companion object {
        fun fromRaw(raw: String): EquipmentCategory =
            entries.firstOrNull { it.value == raw } ?: CAMERA
    }
}

enum class EquipmentStatus(val value: String, val labelVi: String) {
    READY("ready", "Sẵn sàng"),
    IN_USE("in_use", "Đang sử dụng"),
    MAINTENANCE("maintenance", "Bảo trì");

    companion object {
        fun fromRaw(raw: String): EquipmentStatus =
            entries.firstOrNull { it.value == raw } ?: READY
    }
}

enum class ClientTier(val value: String, val labelVi: String) {
    NEW("new", "Khách mới"),
    REGULAR("regular", "Thân thiết"),
    VIP("vip", "VIP"),
    CORPORATE("corporate", "Doanh nghiệp");

    companion object {
        fun fromRaw(raw: String): ClientTier =
            entries.firstOrNull { it.value == raw } ?: NEW
    }
}

enum class TransactionType(val value: String, val labelVi: String) {
    INCOME("income", "Thu"),
    EXPENSE("expense", "Chi");

    companion object {
        fun fromRaw(raw: String): TransactionType =
            entries.firstOrNull { it.value == raw } ?: INCOME
    }
}

enum class TransactionCategory(val value: String, val labelVi: String) {
    BOOKING("booking", "Thu booking"),
    DEPOSIT("deposit", "Đặt cọc"),
    RENTAL("rental", "Thuê thiết bị"),
    EQUIPMENT("equipment", "Mua/Sửa thiết bị"),
    OPERATIONS("operations", "Vận hành"),
    STAFF("staff", "Nhân sự"),
    OTHER("other", "Khác");

    companion object {
        fun fromRaw(raw: String): TransactionCategory =
            entries.firstOrNull { it.value == raw } ?: OTHER
    }
}

@Keep
@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val roomName: String = "",
    val serviceType: String = ServiceType.PHOTO.value,
    val dateText: String = "",
    val timeSlot: String = "",
    val totalAmount: Long = 0L,
    val depositAmount: Long = 0L,
    val paymentStatus: String = PaymentStatus.UNPAID.value,
    val status: String = BookingStatus.PENDING.value,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudioRoom(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = RoomCategory.MULTIPURPOSE.value,
    val hourlyRate: Long = 0L,
    val areaSqm: Long = 0L,
    val status: String = RoomStatus.AVAILABLE.value,
    val amenities: List<String> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EquipmentItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = EquipmentCategory.CAMERA.value,
    val serialCode: String = "",
    val dailyRate: Long = 0L,
    val quantity: Long = 1L,
    val status: String = EquipmentStatus.READY.value,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClientRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val tier: String = ClientTier.NEW.value,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = TransactionType.INCOME.value,
    val category: String = TransactionCategory.BOOKING.value,
    val amount: Long = 0L,
    val dateText: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DashboardSummary(
    val totalBookings: Int = 0,
    val activeBookings: Int = 0,
    val todayBookings: Int = 0,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val pendingReceivables: Long = 0L,
    val availableRooms: Int = 0,
    val totalRooms: Int = 0,
    val readyEquipmentCount: Int = 0,
    val totalEquipmentCount: Int = 0,
    val totalClients: Int = 0
) {
    val netProfit: Long get() = totalIncome - totalExpense
}
