package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ClientRecord
import com.example.data.model.ClientTier
import com.example.data.model.EquipmentCategory
import com.example.data.model.EquipmentItem
import com.example.data.model.EquipmentStatus
import com.example.data.model.FinanceTransaction
import com.example.data.model.PaymentStatus
import com.example.data.model.RoomCategory
import com.example.data.model.RoomStatus
import com.example.data.model.ServiceType
import com.example.data.model.StudioRoom
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionType
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Vui lòng đăng nhập Google trước khi thao tác dữ liệu.")
        check(uid == expectedUserId) { "Phiên đăng nhập đã thay đổi; từ chối ghi chéo tài khoản." }
        return uid
    }

    private fun <T> observeCollection(
        userId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> {
        if (userId.isBlank() || auth.currentUser?.uid != userId) return emptyFlow()
        val path = "users/$userId/$collectionName"
        return db.collection("users").document(userId).collection(collectionName)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeBookings(userId: String): Flow<List<Booking>> =
        observeCollection(userId, "bookings", Booking::class.java)

    fun observeRooms(userId: String): Flow<List<StudioRoom>> =
        observeCollection(userId, "rooms", StudioRoom::class.java)

    fun observeEquipment(userId: String): Flow<List<EquipmentItem>> =
        observeCollection(userId, "equipment", EquipmentItem::class.java)

    fun observeClients(userId: String): Flow<List<ClientRecord>> =
        observeCollection(userId, "clients", ClientRecord::class.java)

    fun observeTransactions(userId: String): Flow<List<FinanceTransaction>> =
        observeCollection(userId, "transactions", FinanceTransaction::class.java)

    // --- BOOKINGS ---

    suspend fun createBooking(userId: String, booking: Booking): Result<Unit> {
        var docPath = "users/$userId/bookings"
        return try {
            val uid = requireUserId(userId)
            val title = booking.title.trim()
            val clientName = booking.clientName.trim()
            val roomName = booking.roomName.trim()
            val dateText = booking.dateText.trim()
            val timeSlot = booking.timeSlot.trim()
            require(title.length in 1..100) { "Tên lịch hẹn phải từ 1–100 ký tự." }
            require(clientName.length in 1..80) { "Tên khách hàng phải từ 1–80 ký tự." }
            require(roomName.length in 1..80) { "Tên phòng studio phải từ 1–80 ký tự." }
            require(dateText.length in 1..20) { "Ngày hẹn không hợp lệ." }
            require(timeSlot.length in 1..30) { "Khung giờ không hợp lệ." }
            require(booking.totalAmount in 0L..10_000_000_000L) { "Tổng chi phí không hợp lệ." }
            require(booking.depositAmount in 0L..booking.totalAmount) { "Tiền cọc không được vượt quá tổng chi phí." }

            val docRef = db.collection("users").document(uid).collection("bookings").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title,
                "clientName" to clientName,
                "clientPhone" to booking.clientPhone.trim().take(30),
                "roomName" to roomName,
                "serviceType" to ServiceType.fromRaw(booking.serviceType).value,
                "dateText" to dateText,
                "timeSlot" to timeSlot,
                "totalAmount" to booking.totalAmount,
                "depositAmount" to booking.depositAmount,
                "paymentStatus" to PaymentStatus.fromRaw(booking.paymentStatus).value,
                "status" to BookingStatus.fromRaw(booking.status).value,
                "notes" to booking.notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBooking(userId: String, booking: Booking): Result<Unit> {
        var docPath = "users/$userId/bookings/${booking.id}"
        return try {
            val uid = requireUserId(userId)
            require(booking.id.isNotBlank()) { "Thiếu mã lịch hẹn để cập nhật." }
            val title = booking.title.trim()
            val clientName = booking.clientName.trim()
            val roomName = booking.roomName.trim()
            val dateText = booking.dateText.trim()
            val timeSlot = booking.timeSlot.trim()
            require(title.length in 1..100) { "Tên lịch hẹn phải từ 1–100 ký tự." }
            require(clientName.length in 1..80) { "Tên khách hàng phải từ 1–80 ký tự." }
            require(roomName.length in 1..80) { "Tên phòng studio phải từ 1–80 ký tự." }
            require(dateText.length in 1..20) { "Ngày hẹn không hợp lệ." }
            require(timeSlot.length in 1..30) { "Khung giờ không hợp lệ." }
            require(booking.totalAmount in 0L..10_000_000_000L) { "Tổng chi phí không hợp lệ." }
            require(booking.depositAmount in 0L..booking.totalAmount) { "Tiền cọc không được vượt quá tổng chi phí." }

            val docRef = db.collection("users").document(uid).collection("bookings").document(booking.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to title,
                    "clientName" to clientName,
                    "clientPhone" to booking.clientPhone.trim().take(30),
                    "roomName" to roomName,
                    "serviceType" to ServiceType.fromRaw(booking.serviceType).value,
                    "dateText" to dateText,
                    "timeSlot" to timeSlot,
                    "totalAmount" to booking.totalAmount,
                    "depositAmount" to booking.depositAmount,
                    "paymentStatus" to PaymentStatus.fromRaw(booking.paymentStatus).value,
                    "status" to BookingStatus.fromRaw(booking.status).value,
                    "notes" to booking.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatus(userId: String, bookingId: String, status: BookingStatus): Result<Unit> {
        var docPath = "users/$userId/bookings/$bookingId"
        return try {
            val uid = requireUserId(userId)
            require(bookingId.isNotBlank()) { "Mã lịch hẹn không hợp lệ." }
            val docRef = db.collection("users").document(uid).collection("bookings").document(bookingId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBooking(userId: String, bookingId: String): Result<Unit> =
        deleteDocument(userId, "bookings", bookingId)

    // --- ROOMS ---

    suspend fun createRoom(userId: String, room: StudioRoom): Result<Unit> {
        var docPath = "users/$userId/rooms"
        return try {
            val uid = requireUserId(userId)
            val name = room.name.trim()
            require(name.length in 1..80) { "Tên phòng phải từ 1–80 ký tự." }
            require(room.hourlyRate in 0L..1_000_000_000L) { "Giá thuê/giờ không hợp lệ." }
            require(room.areaSqm in 0L..10_000L) { "Diện tích phòng không hợp lệ." }
            val cleanAmenities = room.amenities.map { it.trim() }.filter { it.isNotEmpty() }.take(10)

            val docRef = db.collection("users").document(uid).collection("rooms").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to name,
                "category" to RoomCategory.fromRaw(room.category).value,
                "hourlyRate" to room.hourlyRate,
                "areaSqm" to room.areaSqm,
                "status" to RoomStatus.fromRaw(room.status).value,
                "amenities" to cleanAmenities,
                "notes" to room.notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoom(userId: String, room: StudioRoom): Result<Unit> {
        var docPath = "users/$userId/rooms/${room.id}"
        return try {
            val uid = requireUserId(userId)
            require(room.id.isNotBlank()) { "Thiếu mã phòng." }
            val name = room.name.trim()
            require(name.length in 1..80) { "Tên phòng phải từ 1–80 ký tự." }
            require(room.hourlyRate in 0L..1_000_000_000L) { "Giá thuê/giờ không hợp lệ." }
            require(room.areaSqm in 0L..10_000L) { "Diện tích phòng không hợp lệ." }
            val cleanAmenities = room.amenities.map { it.trim() }.filter { it.isNotEmpty() }.take(10)

            val docRef = db.collection("users").document(uid).collection("rooms").document(room.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to name,
                    "category" to RoomCategory.fromRaw(room.category).value,
                    "hourlyRate" to room.hourlyRate,
                    "areaSqm" to room.areaSqm,
                    "status" to RoomStatus.fromRaw(room.status).value,
                    "amenities" to cleanAmenities,
                    "notes" to room.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRoomStatus(userId: String, roomId: String, status: RoomStatus): Result<Unit> {
        var docPath = "users/$userId/rooms/$roomId"
        return try {
            val uid = requireUserId(userId)
            require(roomId.isNotBlank()) { "Mã phòng không hợp lệ." }
            val docRef = db.collection("users").document(uid).collection("rooms").document(roomId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(userId: String, roomId: String): Result<Unit> =
        deleteDocument(userId, "rooms", roomId)

    // --- EQUIPMENT ---

    suspend fun createEquipment(userId: String, item: EquipmentItem): Result<Unit> {
        var docPath = "users/$userId/equipment"
        return try {
            val uid = requireUserId(userId)
            val name = item.name.trim()
            require(name.length in 1..80) { "Tên thiết bị phải từ 1–80 ký tự." }
            require(item.dailyRate in 0L..1_000_000_000L) { "Giá thuê thiết bị không hợp lệ." }
            require(item.quantity in 1L..1_000L) { "Số lượng phải từ 1 đến 1000." }

            val docRef = db.collection("users").document(uid).collection("equipment").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to name,
                "category" to EquipmentCategory.fromRaw(item.category).value,
                "serialCode" to item.serialCode.trim().take(40),
                "dailyRate" to item.dailyRate,
                "quantity" to item.quantity,
                "status" to EquipmentStatus.fromRaw(item.status).value,
                "notes" to item.notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateEquipment(userId: String, item: EquipmentItem): Result<Unit> {
        var docPath = "users/$userId/equipment/${item.id}"
        return try {
            val uid = requireUserId(userId)
            require(item.id.isNotBlank()) { "Thiếu mã thiết bị." }
            val name = item.name.trim()
            require(name.length in 1..80) { "Tên thiết bị phải từ 1–80 ký tự." }
            require(item.dailyRate in 0L..1_000_000_000L) { "Giá thuê thiết bị không hợp lệ." }
            require(item.quantity in 1L..1_000L) { "Số lượng phải từ 1 đến 1000." }

            val docRef = db.collection("users").document(uid).collection("equipment").document(item.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to name,
                    "category" to EquipmentCategory.fromRaw(item.category).value,
                    "serialCode" to item.serialCode.trim().take(40),
                    "dailyRate" to item.dailyRate,
                    "quantity" to item.quantity,
                    "status" to EquipmentStatus.fromRaw(item.status).value,
                    "notes" to item.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateEquipmentStatus(userId: String, equipmentId: String, status: EquipmentStatus): Result<Unit> {
        var docPath = "users/$userId/equipment/$equipmentId"
        return try {
            val uid = requireUserId(userId)
            require(equipmentId.isNotBlank()) { "Mã thiết bị không hợp lệ." }
            val docRef = db.collection("users").document(uid).collection("equipment").document(equipmentId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.value,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteEquipment(userId: String, equipmentId: String): Result<Unit> =
        deleteDocument(userId, "equipment", equipmentId)

    // --- CLIENTS ---

    suspend fun createClient(userId: String, client: ClientRecord): Result<Unit> {
        var docPath = "users/$userId/clients"
        return try {
            val uid = requireUserId(userId)
            val name = client.name.trim()
            require(name.length in 1..80) { "Tên khách hàng phải từ 1–80 ký tự." }

            val docRef = db.collection("users").document(uid).collection("clients").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to name,
                "phone" to client.phone.trim().take(30),
                "email" to client.email.trim().take(120),
                "tier" to ClientTier.fromRaw(client.tier).value,
                "notes" to client.notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateClient(userId: String, client: ClientRecord): Result<Unit> {
        var docPath = "users/$userId/clients/${client.id}"
        return try {
            val uid = requireUserId(userId)
            require(client.id.isNotBlank()) { "Thiếu mã khách hàng." }
            val name = client.name.trim()
            require(name.length in 1..80) { "Tên khách hàng phải từ 1–80 ký tự." }

            val docRef = db.collection("users").document(uid).collection("clients").document(client.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to name,
                    "phone" to client.phone.trim().take(30),
                    "email" to client.email.trim().take(120),
                    "tier" to ClientTier.fromRaw(client.tier).value,
                    "notes" to client.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(userId: String, clientId: String): Result<Unit> =
        deleteDocument(userId, "clients", clientId)

    // --- TRANSACTIONS ---

    suspend fun createTransaction(userId: String, transaction: FinanceTransaction): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            val title = transaction.title.trim()
            val dateText = transaction.dateText.trim()
            require(title.length in 1..100) { "Nội dung thu chi phải từ 1–100 ký tự." }
            require(transaction.amount in 1L..10_000_000_000L) { "Số tiền phải lớn hơn 0." }
            require(dateText.length in 1..20) { "Ngày giao dịch không hợp lệ." }

            val docRef = db.collection("users").document(uid).collection("transactions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title,
                "type" to TransactionType.fromRaw(transaction.type).value,
                "category" to TransactionCategory.fromRaw(transaction.category).value,
                "amount" to transaction.amount,
                "dateText" to dateText,
                "notes" to transaction.notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(userId: String, transaction: FinanceTransaction): Result<Unit> {
        var docPath = "users/$userId/transactions/${transaction.id}"
        return try {
            val uid = requireUserId(userId)
            require(transaction.id.isNotBlank()) { "Thiếu mã giao dịch." }
            val title = transaction.title.trim()
            val dateText = transaction.dateText.trim()
            require(title.length in 1..100) { "Nội dung thu chi phải từ 1–100 ký tự." }
            require(transaction.amount in 1L..10_000_000_000L) { "Số tiền phải lớn hơn 0." }
            require(dateText.length in 1..20) { "Ngày giao dịch không hợp lệ." }

            val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to title,
                    "type" to TransactionType.fromRaw(transaction.type).value,
                    "category" to TransactionCategory.fromRaw(transaction.category).value,
                    "amount" to transaction.amount,
                    "dateText" to dateText,
                    "notes" to transaction.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> =
        deleteDocument(userId, "transactions", transactionId)

    private suspend fun deleteDocument(
        userId: String,
        collectionName: String,
        documentId: String
    ): Result<Unit> {
        var docPath = "users/$userId/$collectionName/$documentId"
        return try {
            val uid = requireUserId(userId)
            require(documentId.isNotBlank()) { "Thiếu mã tài liệu để xoá." }
            val docRef = db.collection("users").document(uid).collection(collectionName).document(documentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
