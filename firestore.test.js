const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_studio_1";
const BOB_UID = "bob_studio_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/bookings`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/rooms`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/equipment`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/clients`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());

  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").set({
      userId: ALICE_UID,
      title: "Chup Lookbook",
      clientName: "Lan Anh",
      roomName: "Studio A",
      serviceType: "photo",
      dateText: "2026-11-20",
      timeSlot: "09:00 - 12:00",
      totalAmount: 2500000,
      depositAmount: 500000,
      paymentStatus: "deposit",
      status: "confirmed",
      notes: "",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Bookings: Kotlin create, partial update, terminal lock, and negative guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create matching BookingRepository.createBooking
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").set({
      userId: ALICE_UID,
      title: "Chup Lookbook Thu Dong",
      clientName: "Minh Anh",
      clientPhone: "0901234567",
      roomName: "Studio Cyclorama A",
      serviceType: "photo",
      dateText: "2026-11-21",
      timeSlot: "09:00 - 12:00",
      totalAmount: 3000000,
      depositAmount: 1000000,
      paymentStatus: "deposit",
      status: "pending",
      notes: "Can chuan bi den Profoto",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List matches repository query
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/bookings`).get());

  // Partial status update
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").update({
      status: "confirmed",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_ghost").set({
      id: "bk_ghost",
      userId: ALICE_UID,
      title: "Ghost ID",
      clientName: "Khach A",
      roomName: "Studio A",
      serviceType: "photo",
      dateText: "2026-11-21",
      timeSlot: "14:00 - 16:00",
      totalAmount: 1000000,
      depositAmount: 0,
      paymentStatus: "unpaid",
      status: "pending",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject deposit exceeding totalAmount
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").update({
      depositAmount: 9999999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Transition to terminal state 'completed'
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").update({
      status: "completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock: subsequent updates must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/bookings`).doc("bk_1").update({
      status: "confirmed",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Rooms, Equipment, Clients, and Transactions: Kotlin payloads & cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Room
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/rooms`).doc("room_1").set({
      userId: ALICE_UID,
      name: "Phong Cyclorama A",
      category: "photo",
      hourlyRate: 500000,
      areaSqm: 65,
      status: "available",
      amenities: ["Cyclorama 6m", "Dieu hoa", "Phong thay do"],
      notes: "",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/rooms`).doc("room_1").update({
      status: "occupied",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Equipment
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/equipment`).doc("eq_1").set({
      userId: ALICE_UID,
      name: "Sony A7R V",
      category: "camera",
      serialCode: "SN-A7RV-01",
      dailyRate: 850000,
      quantity: 2,
      status: "ready",
      notes: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Client
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/clients`).doc("cl_1").set({
      userId: ALICE_UID,
      name: "Brand Thoi Trang Z",
      phone: "0988777666",
      email: "contact@brandz.vn",
      tier: "vip",
      notes: "Uu tien lich buoi sang",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Transaction
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").set({
      userId: ALICE_UID,
      title: "Coc booking Lookbook",
      type: "income",
      category: "deposit",
      amount: 1000000,
      dateText: "2026-11-20",
      notes: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Negative: Bob cannot read or write Alice's collections
  await assertFails(bobDb.collection(`users/${ALICE_UID}/rooms`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/rooms`).doc("room_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/rooms`).doc("room_1").update({
      status: "maintenance",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/clients`).doc("cl_1").delete());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/transactions`).get());

  // Negative: future timestamp tampered by client must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_future").set({
      userId: ALICE_UID,
      title: "Tampered",
      type: "income",
      category: "other",
      amount: 100000,
      dateText: "2026-11-20",
      notes: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
