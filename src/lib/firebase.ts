/**
 * Center Plus Desktop — Firebase Integration
 * ==========================================
 * Initializes Firebase App, Authentication, and Firestore using environment
 * variables. The app remains local-first: every write lands in the local
 * store immediately, then syncs to Firestore asynchronously.
 *
 * Setup:
 *   1. Copy `.env.example` → `.env`
 *   2. Fill in your Firebase project credentials
 *   3. The `auth` and `db` exports below are ready to use
 */

import { initializeApp, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import {
  getFirestore,
  doc,
  setDoc,
  deleteDoc,
  collection,
  getDocs,
  type Firestore,
} from "firebase/firestore";

/* --------------------------- env-based config --------------------------- */
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? "",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? "",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? "",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? "",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? "",
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? "",
};

/** True when all critical env vars are present. */
export const FIREBASE_ENABLED = Boolean(
  firebaseConfig.apiKey && firebaseConfig.projectId && firebaseConfig.appId,
);

/* --------------------------- singletons (lazy) -------------------------- */
let _app: FirebaseApp | null = null;

/**
 * Lazily initialize the Firebase App singleton.
 * Returns null if credentials are not configured.
 */
function getApp(): FirebaseApp | null {
  if (!FIREBASE_ENABLED) return null;
  if (!_app) {
    _app = initializeApp(firebaseConfig);
  }
  return _app;
}

/** Firebase Authentication singleton. Null when not configured. */
export const auth: Auth | null = (() => {
  const app = getApp();
  return app ? getAuth(app) : null;
})();

/** Firestore Database singleton. Null when not configured. */
export const db: Firestore | null = (() => {
  const app = getApp();
  return app ? getFirestore(app) : null;
})();

/** Backwards-compatible exports for the raw singletons. */
export { firebaseConfig, getApp };

/* ----------------------- tenant-isolated path helpers ------------------- */
/**
 * Every collection lives under /centers/{centerId}/ to keep tenant data
 * fully isolated. These helpers build the correct document paths.
 */
export const paths = {
  /** /centers/{centerId}/profile/settings */
  settings: (cid: string) => `centers/${cid}/profile/settings`,
  /** /centers/{centerId}/{collection}/{id} */
  collection: (cid: string, coll: string, id: string) =>
    `centers/${cid}/${coll}/${id}`,
};

/** Map of local collection keys → Firestore sub-collection path segments. */
export const COLLECTIONS = [
  "students", "teachers", "groups", "classrooms", "schedule_events",
  "attendance", "payments", "expenses", "exams", "exam_grades",
  "assignments", "student_notes",
] as const;

/* ------------------------- sync primitives ------------------------------ */

/**
 * Push (upsert) a single record to Firestore at the given path.
 * Uses `merge: true` so partial updates don't clobber existing fields.
 * Falls back to a no-op delay when Firebase is not configured.
 */
export async function pushRecord(path: string, data: unknown): Promise<void> {
  if (!FIREBASE_ENABLED || !db) {
    await new Promise((r) => setTimeout(r, 250));
    return;
  }
  await setDoc(doc(db, path), data as Record<string, unknown>, { merge: true });
}

/**
 * Delete a document from Firestore at the given path.
 * No-op when Firebase is not configured.
 */
export async function deleteRecord(path: string): Promise<void> {
  if (!FIREBASE_ENABLED || !db) return;
  await deleteDoc(doc(db, path));
}

/**
 * Pull all documents from a Firestore collection path.
 * Returns an empty array when Firebase is not configured.
 */
export async function pullCollection(path: string): Promise<Record<string, unknown>[]> {
  if (!FIREBASE_ENABLED || !db) return [];
  const snapshot = await getDocs(collection(db, path));
  return snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
}

/* --------------------------- status helpers ----------------------------- */
export const FIREBASE_STATUS = {
  enabled: FIREBASE_ENABLED,
  projectId: firebaseConfig.projectId || "—",
  ready: FIREBASE_ENABLED ? "Cloud ready" : "Local-only",
};
