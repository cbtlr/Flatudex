const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read submissions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("submissions").get());
});

test("Authenticated user: can create valid submission under their own UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("submissions").doc("sub_alice").set({
      userId: ALICE_UID,
      authorName: "Alice",
      soundProfile: {
        durationSeconds: 2.5,
        decibels: 75,
        pitchHz: 120,
        waveform: "Explosive Pop",
        resonance: "Echoey",
        qualitativeNotes: "Deep bass rumble"
      },
      smellProfile: {
        pungencyScale: 8,
        lingeringSeconds: 45,
        dispersionRadiusMeters: 4.0,
        thermalSensation: "Warm",
        primaryOdorNote: "Sulfur",
        qualitativeNotes: "Sharp egg note"
      },
      agreeCount: 0
    })
  );
});

test("Authenticated user: cannot forge submission with someone else's UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("submissions").doc("sub_forge").set({
      userId: BOB_UID,
      authorName: "Bob Impersonator",
      soundProfile: { durationSeconds: 1.0 },
      smellProfile: { pungencyScale: 5 }
    })
  );
});

test("Authenticated user: can read public submissions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("submissions").doc("sub_seed").set({
      userId: BOB_UID,
      authorName: "Bob",
      soundProfile: { durationSeconds: 1.0 },
      smellProfile: { pungencyScale: 5 }
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("submissions").doc("sub_seed").get());
});

test("Non-owner cannot delete another user's submission", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("submissions").doc("sub_bob").set({
      userId: BOB_UID,
      authorName: "Bob",
      soundProfile: { durationSeconds: 1.0 },
      smellProfile: { pungencyScale: 5 }
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("submissions").doc("sub_bob").delete());
});

test("Other authenticated user can upvote agreeCount", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("submissions").doc("sub_bob").set({
      userId: BOB_UID,
      authorName: "Bob",
      soundProfile: { durationSeconds: 1.0 },
      smellProfile: { pungencyScale: 5 },
      agreeCount: 3
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("submissions").doc("sub_bob").update({
      agreeCount: 4
    })
  );
});

test("Taxa catalog: authenticated user can read taxa", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("taxa").doc("taxon_1").set({
      scientificName: "Flatus bombus grandis",
      commonName: "Thunderous Bass",
      genus: "Flatus",
      species: "bombus grandis"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("taxa").doc("taxon_1").get());
});
