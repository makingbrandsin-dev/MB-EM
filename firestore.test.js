const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "emp_alice_123";
const BOB_UID = "emp_bob_456";
const ADMIN_UID = "admin_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

// --- SECURITY BOUNDARY & AUTHORIZATION TESTS ---

test("1. Unauthenticated user: cannot read tasks or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc("user_1").get());
});

test("2. Unauthenticated user: cannot create invoice or admin broadcast", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("invoices").doc("inv_1").set({
      invoiceNumber: "INV-001",
      clientName: "Acme Corp",
      amount: 5000,
    })
  );
  await assertFails(
    unauthDb.collection("admin_broadcasts").doc("bc_1").set({
      title: "System Maintenance",
      message: "Server update at midnight",
    })
  );
});

test("3. Self-Assigned Role Prevention: Standard user cannot create user doc with role=admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      email: "alice@company.com",
      name: "Alice Smith",
      role: "admin",
      status: "ACTIVE",
    })
  );
});

test("4. Forged Admin Role Prevention: Standard user cannot update role to admin on existing profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      email: "alice@company.com",
      name: "Alice Smith",
      role: "employee",
      status: "ACTIVE",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
    })
  );
});

test("5. Unauthorized Deletion: Employee cannot delete invoice or shift schedule", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("invoices").doc("inv_100").set({
      invoiceNumber: "INV-100",
      clientName: "Global Tech",
      amount: 12000,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertFails(aliceDb.collection("invoices").doc("inv_100").delete());
});

test("6. Valid Employee Operation: Authenticated employee can create and read tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_1").set({
      title: "Complete Security Audit",
      status: "In Progress",
      assignee: "alice@company.com",
    })
  );
  await assertSucceeds(aliceDb.collection("tasks").doc("task_1").get());
});

test("7. Valid Admin Operation: Trusted admin can post broadcasts and approve users", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@makingbrands.com",
    });
    await context.firestore().collection("users").doc(BOB_UID).set({
      email: "bob@company.com",
      name: "Bob Jones",
      role: "employee",
      status: "PENDING",
    });
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "admin@makingbrands.com" }).firestore();
  await assertSucceeds(
    adminDb.collection("admin_broadcasts").doc("bc_100").set({
      title: "Q4 Growth Meeting",
      message: "All-hands call at 3 PM",
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(BOB_UID).update({
      status: "ACTIVE",
    })
  );
});

test("8. Cross-Organization / Org Tampering: Employee cannot create or modify organization records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertFails(
    aliceDb.collection("organizations").doc("org_corp_evil").set({
      name: "Compromised Org",
      owner: ALICE_UID,
    })
  );
});

test("9. Unauthorized Modification: Employee cannot alter another user's profile or approval status", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      email: "bob@company.com",
      name: "Bob Jones",
      role: "employee",
      status: "PENDING",
      orgId: "org_hq",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@company.com" }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      status: "ACTIVE",
    })
  );
});

