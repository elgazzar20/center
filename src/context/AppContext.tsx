import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import type {
  DatabaseShape,
  UserRbac,
  Role,
  SyncStatus,
  SyncLogEntry,
  Student,
  Teacher,
  Group,
  Classroom,
  ScheduleEvent,
  AttendanceRecord,
  Payment,
  Expense,
  Exam,
  ExamGrade,
  Assignment,
  StudentNote,
  Branch,
} from "../lib/types";
import { translate, type Lang } from "../i18n/translations";
import {
  loadDb,
  persistDb,
  seedDb,
  emptyDb,
  now,
  uid,
  saveBackup,
  listBackups,
  restoreBackup,
  deleteBackup,
  downloadBackupFile,
  parseBackupFile,
  restoreFromFile,
  dbKeyFor,
  type BackupMeta,
  type BackupFile,
} from "../lib/db";
import { nextGrade, obfuscate, deobfuscate } from "../lib/constants";
import { auth, db as firestoreDb, FIREBASE_ENABLED } from "../lib/firebase";
import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  type User as FbUser,
} from "firebase/auth";
import { doc, getDoc, setDoc } from "firebase/firestore";

/* collections that are arrays of stamped records */
type Collections = {
  students: Student;
  teachers: Teacher;
  groups: Group;
  classrooms: Classroom;
  scheduleEvents: ScheduleEvent;
  attendance: AttendanceRecord;
  payments: Payment;
  expenses: Expense;
  exams: Exam;
  examGrades: ExamGrade;
  assignments: Assignment;
  studentNotes: StudentNote;
  branches: Branch;
};
type CollKey = keyof Collections;

export const ALL_PERMS = [
  "students.manage",
  "teachers.manage",
  "classes.manage",
  "schedule.manage",
  "attendance.manage",
  "finance.manage",
  "exams.manage",
  "staff.manage",
  "settings.manage",
  "reports.view",
  "reports.send",
  "revenue.teachers",
  "revenue.center",
  "data.add",
  "data.delete",
  "ai.use",
] as const;
export type Permission = (typeof ALL_PERMS)[number];

export const ROLE_PERMS: Record<Role, Permission[]> = {
  OWNER: [...ALL_PERMS],
  ADMIN: [...ALL_PERMS],
  SECRETARY: [
    "students.manage",
    "attendance.manage",
    "finance.manage",
    "classes.manage",
    "schedule.manage",
    "reports.view",
    "reports.send",
    "revenue.center",
    "data.add",
    "data.delete",
    "ai.use",
  ],
  TEACHER: ["attendance.manage", "exams.manage", "reports.view", "ai.use"],
  PARENT: [],
  super_admin: [], // super admin has its own dashboard, no regular permissions
};

export interface StaffLogItem {
  op: string; // e.g. "students:create"
  label: string; // human readable target, e.g. a student name
  at: number;
}
export interface StaffStat {
  count: number;
  lastAt: number;
  lastOp: string;
  log: StaffLogItem[];
}

const DEMO_CENTER = "demo-center-futureminds";

/* --------------------------- staff activity log ------------------------- */
const ACTIVITY_KEY = (cid: string) => `cpd_activity_${cid}`;
function loadActivity(cid: string): Record<string, StaffStat> {
  const raw = loadPref<Record<string, Omit<StaffStat, "log"> & { log?: StaffLogItem[] }>>(ACTIVITY_KEY(cid), {});
  // backfill log for older saved data
  const out: Record<string, StaffStat> = {};
  for (const [k, v] of Object.entries(raw)) out[k] = { ...v, log: v.log ?? [] };
  return out;
}
function saveActivity(cid: string, data: Record<string, StaffStat>) {
  try {
    localStorage.setItem(ACTIVITY_KEY(cid), JSON.stringify(data));
  } catch {
    /* ignore */
  }
}

interface AppContextValue {
  // i18n
  lang: Lang;
  dir: "rtl" | "ltr";
  t: (key: string, params?: Record<string, string | number>) => string;
  setLang: (l: Lang) => void;
  toggleLang: () => void;
  // theme
  theme: "light" | "dark";
  toggleTheme: () => void;
  // font scale (accessibility)
  fontScale: "small" | "medium" | "large";
  setFontScale: (s: "small" | "medium" | "large") => void;
  // auth
  user: UserRbac | null;
  signIn: (email: string, password: string) => Promise<{ ok: boolean; error?: string }>;
  signUp: (name: string, email: string, password: string) => Promise<{ ok: boolean; error?: string }>;
  signInWithGoogle: () => Promise<{ ok: boolean; error?: string }>;
  signOut: () => void;
  demoAccess: (as: "owner" | "secretary" | "parent") => void;
  inviteStaff: (email: string, name: string, password: string, role: Role, perms: Permission[], opts?: { salary?: number; title?: string }) => { ok: boolean; error?: string; uid?: string };
  updateStaff: (uid: string, patch: Partial<UserRbac>) => void;
  staff: UserRbac[];
  staffActivity: Record<string, StaffStat>;
  deleteStaff: (uid: string) => void;
  sendStaffMessage: (toUid: string, text: string) => void;
  can: (perm: Permission) => boolean;
  canAdd: () => boolean;
  canDelete: () => boolean;
  canAddStudent: () => boolean;
  canAddTeacher: () => boolean;
  // subscription gating
  subscriptionPlan: "free" | "pro" | "enterprise";
  subscriptionEndDate: number | null;
  refreshSubscriptionPlan: () => Promise<void>;
  canUseFeature: (feature: string) => boolean;
  // branches
  currentBranchId: string;
  switchBranch: (branchId: string) => void;
  // data
  db: DatabaseShape;
  upsert: <K extends CollKey>(coll: K, item: Collections[K]) => void;
  remove: <K extends CollKey>(coll: K, id: string) => void;
  updateProfile: (patch: Partial<DatabaseShape["profile"]>) => void;
  resetData: () => void;
  replaceDb: (db: DatabaseShape) => void;
  restoreBackupFromFile: (file: File) => Promise<boolean>;
  // academic year
  promoteYear: () => { promoted: number; skipped: number; backupTs: number };
  // backup
  backups: BackupMeta[];
  createBackup: (label?: string) => void;
  restoreFromBackup: (ts: number) => void;
  removeBackup: (ts: number) => void;
  exportBackup: () => void;
  // sync
  syncStatus: SyncStatus;
  online: boolean;
  setOnline: (b: boolean) => void;
  lastSync: number;
  syncLog: SyncLogEntry[];
  pendingCount: number;
  flushNow: () => void;
}

const Ctx = createContext<AppContextValue | null>(null);

/* ----------------------------- preferences ------------------------------ */
function loadPref<T>(key: string, fallback: T): T {
  try {
    const v = localStorage.getItem(key);
    return v ? (JSON.parse(v) as T) : fallback;
  } catch {
    return fallback;
  }
}

/* ------------------------------ user store ------------------------------ */
const USERS_KEY = "cpd_users";
const SESSION_KEY = "cpd_session";

function loadUsers(): UserRbac[] {
  return loadPref<UserRbac[]>(USERS_KEY, []);
}
function saveUsers(u: UserRbac[]) {
  localStorage.setItem(USERS_KEY, JSON.stringify(u));
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(() => loadPref("cpd_lang", "ar"));
  const [theme, setTheme] = useState<"light" | "dark">(() => loadPref("cpd_theme", "light"));
  const [fontScale, setFontScaleState] = useState<"small" | "medium" | "large">(() => loadPref("cpd_font", "medium"));
  const [user, setUser] = useState<UserRbac | null>(() => {
    const uidSession = loadPref<string | null>(SESSION_KEY, null);
    const users = loadUsers();
    return uidSession ? users.find((u) => u.uid === uidSession) ?? null : null;
  });
  const [users, setUsers] = useState<UserRbac[]>(() => loadUsers());

  const centerId = user?.centerId ?? DEMO_CENTER;
  const [currentBranchId, setCurrentBranchId] = useState<string>(() => loadPref("cpd_branch", "main"));
  const [subscriptionPlan, setSubscriptionPlanState] = useState<"free" | "pro" | "enterprise">(() => loadPref(`cpd_plan_${centerId}`, "free"));
  const [subscriptionEndDate, setSubscriptionEndDate] = useState<number | null>(() => loadPref(`cpd_plan_end_${centerId}`, null));
  const effectiveDbKey = dbKeyFor(centerId, currentBranchId);
  const [db, setDb] = useState<DatabaseShape>(() => {
    const existing = loadDb(centerId);
    return existing ?? seedDb(DEMO_CENTER);
  });

  const [online, setOnlineState] = useState(true);
  const [syncStatus, setSyncStatus] = useState<SyncStatus>("online");
  const [lastSync, setLastSync] = useState<number>(() => now());
  const [syncLog, setSyncLog] = useState<SyncLogEntry[]>([]);
  const [backups, setBackups] = useState<BackupMeta[]>([]);
  const [staffActivity, setStaffActivity] = useState<Record<string, StaffStat>>(() => loadActivity(centerId));
  const flushTimer = useRef<number | null>(null);

  /** Bump the acting user's activity stat and append a readable log item. */
  const recordActivity = useCallback(
    (op: string, label = "") => {
      if (!user) return;
      setStaffActivity((prev) => {
        const cur = prev[user.uid] ?? { count: 0, lastAt: 0, lastOp: "", log: [] as StaffLogItem[] };
        const item: StaffLogItem = { op, label, at: now() };
        const next = {
          ...prev,
          [user.uid]: {
            count: cur.count + 1,
            lastAt: item.at,
            lastOp: op,
            log: [item, ...(cur.log ?? [])].slice(0, 50),
          },
        };
        saveActivity(centerId, next);
        return next;
      });
    },
    [user, centerId],
  );

  /* --------------------------- effects: prefs --------------------------- */
  useEffect(() => {
    const dir = lang === "ar" ? "rtl" : "ltr";
    document.documentElement.lang = lang;
    document.documentElement.dir = dir;
    localStorage.setItem("cpd_lang", JSON.stringify(lang));
  }, [lang]);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    localStorage.setItem("cpd_theme", JSON.stringify(theme));
  }, [theme]);

  // font scale — applies a root font-size multiplier for accessibility
  useEffect(() => {
    const scale = fontScale === "small" ? "0.9" : fontScale === "large" ? "1.12" : "1";
    document.documentElement.style.fontSize = `${parseFloat(scale) * 100}%`;
    localStorage.setItem("cpd_font", JSON.stringify(fontScale));
  }, [fontScale]);

  const setFontScale = useCallback((s: "small" | "medium" | "large") => setFontScaleState(s), []);

  /* load the right database whenever center or branch changes */
  useEffect(() => {
    let existing = loadDb(effectiveDbKey);
    // migration: backfill branches for older datasets
    if (existing && (!existing.branches || existing.branches.length === 0)) {
      existing = {
        ...existing,
        branches: [{ id: "main", name: existing.profile.name || "Main Branch", isMain: true, lastUpdated: now() }],
      };
    }
    setDb(existing ?? seedDb(effectiveDbKey));
    setBackups(listBackups(effectiveDbKey));
    setSyncLog([]);
    setStaffActivity(loadActivity(centerId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [effectiveDbKey]);

  /** Switch to a different branch — loads its isolated dataset. */
  const switchBranch = useCallback((branchId: string) => {
    localStorage.setItem("cpd_branch", JSON.stringify(branchId));
    setCurrentBranchId(branchId);
  }, []);

  /* persist db whenever it changes (fast, debounced) */
  const persistTimer = useRef<number | null>(null);
  useEffect(() => {
    if (persistTimer.current) window.clearTimeout(persistTimer.current);
    persistTimer.current = window.setTimeout(() => persistDb(effectiveDbKey, db), 120);
  }, [db, effectiveDbKey]);

  /* ------------------------------ sync engine --------------------------- */
  const flush = useCallback(() => {
    if (flushTimer.current) window.clearTimeout(flushTimer.current);
    setSyncStatus("syncing");
    flushTimer.current = window.setTimeout(() => {
      setSyncLog((prev) => prev.map((e) => (e.status === "queued" ? { ...e, status: "pushed" } : e)));
      setLastSync(now());
      setSyncStatus(online ? "online" : "offline");
    }, 300);
  }, [online]);

  // when coming back online, flush queued writes
  useEffect(() => {
    if (online) {
      const hasQueued = syncLog.some((e) => e.status === "queued");
      if (hasQueued) flush();
      else setSyncStatus("online");
    } else {
      setSyncStatus("offline");
      if (flushTimer.current) window.clearTimeout(flushTimer.current);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [online]);

  const queueSync = useCallback(
    (path: string, op: SyncLogEntry["op"]) => {
      const entry: SyncLogEntry = { id: uid("sync"), path, op, at: now(), status: "queued" };
      setSyncLog((prev) => [entry, ...prev].slice(0, 60));
      if (online) flush();
    },
    [flush, online],
  );

  /* ------------------------------- i18n -------------------------------- */
  const t = useCallback(
    (key: string, params?: Record<string, string | number>) => translate(lang, key, params),
    [lang],
  );
  const setLang = useCallback((l: Lang) => setLangState(l), []);
  const toggleLang = useCallback(() => setLangState((p) => (p === "en" ? "ar" : "en")), []);

  /* ------------------------------- theme ------------------------------- */
  const toggleTheme = useCallback(() => setTheme((p) => (p === "light" ? "dark" : "light")), []);

  /* -------------------------------- auth ------------------------------- */
  const beginSession = useCallback((u: UserRbac) => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(u.uid));
    setUser(u);
  }, []);

  const ensureSeeded = useCallback((cId: string) => {
    if (!loadDb(cId)) persistDb(cId, seedDb(cId));
  }, []);

  /** Maps a Firebase Auth error code to our local error keys. */
  const mapFbError = (code: string): string => {
    if (code.includes("email-already-in-use")) return "email-exists";
    if (code.includes("user-not-found")) return "no-account";
    if (code.includes("wrong-password") || code.includes("invalid-credential")) return "wrong-password";
    if (code.includes("popup-closed") || code.includes("cancelled")) return "cancelled";
    return "network-error";
  };

  /** Ensures a centers/{uid} document exists in Firestore for a new user.
   *  This makes the user appear in the Super Admin dashboard as a "center". */
  const ensureCenterDocument = useCallback(
    async (uid: string, email: string, displayName: string): Promise<void> => {
      if (!FIREBASE_ENABLED || !firestoreDb) {
        console.warn("[ensureCenterDocument] Firebase not enabled, skipping");
        return;
      }
      try {
        const centerRef = doc(firestoreDb, "centers", uid);
        const centerSnap = await getDoc(centerRef);
        if (!centerSnap.exists()) {
          console.log("[ensureCenterDocument] Creating centers/" + uid + " for " + email);
          await setDoc(centerRef, {
            id: uid,
            name: `${displayName}'s Center`,
            ownerId: uid,
            ownerEmail: email,
            status: "active",
            subscriptionPlan: "free",
            subscriptionStatus: "trialing",
            subscriptionStartDate: Date.now(),
            studentCount: 0,
            teacherCount: 0,
            createdAt: Date.now(),
          });
          console.log("[ensureCenterDocument] SUCCESS: centers/" + uid + " created");
        } else {
          console.log("[ensureCenterDocument] centers/" + uid + " already exists");
        }
      } catch (err) {
        console.error("[ensureCenterDocument] FAILED:", err);
      }
    },
    [],
  );

  /** Creates or retrieves the local RBAC user for a Firebase user.
   *  Seeds an EMPTY database (not demo data) so each account is truly isolated. */
  const upsertLocalRbacFromFirebase = useCallback(
    (fb: FbUser): UserRbac => {
      const list = loadUsers();
      const existing = list.find((u) => u.uid === fb.uid);
      if (existing) return existing;

      const ts = now();
      const displayName = fb.displayName ?? fb.email?.split("@")[0] ?? "User";
      const newOwner: UserRbac = {
        uid: fb.uid,
        email: fb.email ?? "",
        displayName,
        role: "OWNER",
        centerId: fb.uid,
        ownerId: fb.uid,
        permissions: ROLE_PERMS.OWNER,
        photoUrl: fb.photoURL ?? undefined,
        active: true,
        createdAt: ts,
        lastUpdated: ts,
      };
      // Seed an EMPTY database — each real account starts fresh, not with demo data
      persistDb(fb.uid, emptyDb(fb.uid, displayName));
      const next = [...list, newOwner];
      saveUsers(next);
      setUsers(next);
      // Also create the centers/{uid} document in Firestore
      void ensureCenterDocument(fb.uid, fb.email ?? "", displayName);
      return newOwner;
    },
    [ensureCenterDocument],
  );

  const signUp = useCallback(
    async (name: string, email: string, password: string): Promise<{ ok: boolean; error?: string }> => {
      try {
        // 0. Local-first: if email already exists locally, block duplicate
        const existingList = loadUsers();
        if (existingList.some((u) => u.email.toLowerCase() === email.toLowerCase())) {
          return { ok: false, error: "email-exists" };
        }

        // 1. Firebase Auth: create the account (this is the critical step)
        const cred = await createUserWithEmailAndPassword(auth!, email, password);
        const fb = cred.user;

        // 2. Firestore: create users/{uid} document — NON-BLOCKING
        //    If this fails (security rules, network), the account is still valid.
        if (FIREBASE_ENABLED && firestoreDb) {
          try {
            await setDoc(doc(firestoreDb, "users", fb.uid), {
              uid: fb.uid,
              email,
              displayName: name,
              role: "center_owner",
              createdAt: Date.now(),
            });
          } catch {
            // Firestore write failed (rules?) — continue with local account
          }
        }

        // 3. Local-first: create isolated RBAC + empty database
        const list = loadUsers();
        const ts = now();
        const owner: UserRbac = {
          uid: fb.uid,
          email,
          displayName: name,
          role: "OWNER",
          centerId: fb.uid,
          ownerId: fb.uid,
          permissions: ROLE_PERMS.OWNER,
          active: true,
          password: obfuscate(password),
          createdAt: ts,
          lastUpdated: ts,
        };
        // EMPTY database — not demo data — so each user's data is truly their own
        persistDb(fb.uid, emptyDb(fb.uid, name));
        const next = [...list, owner];
        saveUsers(next);
        setUsers(next);
        // 4. Create centers/{uid} document so this user appears in Super Admin
        await ensureCenterDocument(fb.uid, email, name);
        beginSession(owner);
        return { ok: true };
      } catch (err: unknown) {
        const code = (err as { code?: string }).code ?? "";
        const mapped = mapFbError(code);

        // FALLBACK: If Firebase is unreachable (network/timeout), create a local-only account
        if (mapped === "network-error") {
          const list = loadUsers();
          if (list.some((u) => u.email.toLowerCase() === email.toLowerCase())) {
            return { ok: false, error: "email-exists" };
          }
          const ts = now();
          const localUid = "local-" + btoa(email).replace(/[^a-zA-Z0-9]/g, "").slice(0, 16);
          const owner: UserRbac = {
            uid: localUid,
            email,
            displayName: name,
            role: "OWNER",
            centerId: localUid,
            ownerId: localUid,
            permissions: ROLE_PERMS.OWNER,
            active: true,
            password: obfuscate(password),
            createdAt: ts,
            lastUpdated: ts,
          };
          persistDb(localUid, emptyDb(localUid, name));
          const next = [...list, owner];
          saveUsers(next);
          setUsers(next);
          beginSession(owner);
          return { ok: true };
        }

        return { ok: false, error: mapped };
      }
    },
    [beginSession, ensureCenterDocument],
  );

  /** Checks Firestore for super_admin role, then signs in normally. */
  const signIn = useCallback(
    async (email: string, password: string): Promise<{ ok: boolean; error?: string }> => {
      try {
        // 1. Firebase: authenticate with email + password
        const cred = await signInWithEmailAndPassword(auth!, email, password);
        const fb = cred.user;

        // 2. Check Firestore for super_admin role FIRST
        let firestoreRole: string | undefined;
        if (FIREBASE_ENABLED && firestoreDb) {
          try {
            const userDoc = await getDoc(doc(firestoreDb, "users", fb.uid));
            if (userDoc.exists()) {
              firestoreRole = userDoc.data()?.role;
            }
          } catch {
            // non-blocking
          }
        }

        // 3. If super_admin, create RBAC with that role (triggers OTP gate)
        if (firestoreRole === "super_admin") {
          const list = loadUsers();
          const ts = now();
          const adminUser: UserRbac = {
            uid: fb.uid,
            email: fb.email ?? "",
            displayName: fb.displayName ?? "Super Admin",
            role: "super_admin" as Role,
            centerId: "super_admin",
            ownerId: fb.uid,
            permissions: [],
            active: true,
            createdAt: ts,
            lastUpdated: ts,
          };
          const next = [...list.filter((u) => u.uid !== fb.uid), adminUser];
          saveUsers(next);
          setUsers(next);
          beginSession(adminUser);
          return { ok: true };
        }

        // 4. Normal flow: look up or create the RBAC user
        const rbacUser = upsertLocalRbacFromFirebase(fb);
        // Ensure centers/{uid} document exists
        ensureCenterDocument(fb.uid, fb.email ?? "", fb.displayName ?? rbacUser.displayName);
        if (!rbacUser.active) return { ok: false, error: "inactive" };
        beginSession(rbacUser);
        return { ok: true };
      } catch (err: unknown) {
        const code = (err as { code?: string }).code ?? "";
        const mapped = mapFbError(code);

        // FALLBACK: If Firebase is unreachable, try local-only auth
        if (mapped === "network-error") {
          const list = loadUsers();
          const localUser = list.find(
            (u) => u.email.toLowerCase() === email.toLowerCase() && u.password,
          );
          if (localUser) {
            if (deobfuscate(localUser.password!) !== password) {
              return { ok: false, error: "wrong-password" };
            }
            if (!localUser.active) return { ok: false, error: "inactive" };
            beginSession(localUser);
            return { ok: true };
          }
          return { ok: false, error: "no-account" };
        }

        return { ok: false, error: mapped };
      }
    },
    [beginSession, upsertLocalRbacFromFirebase, ensureCenterDocument],
  );

  const signInWithGoogle = useCallback(async (): Promise<{ ok: boolean; error?: string }> => {
    try {
      const provider = new GoogleAuthProvider();
      const cred = await signInWithPopup(auth!, provider);
      const fb = cred.user;

      // Create Firestore users/{uid} document if it doesn't exist — NON-BLOCKING
      if (FIREBASE_ENABLED && firestoreDb) {
        try {
          const userDocRef = doc(firestoreDb, "users", fb.uid);
          const userDoc = await getDoc(userDocRef);
          if (!userDoc.exists()) {
            await setDoc(userDocRef, {
              uid: fb.uid,
              email: fb.email ?? "",
              displayName: fb.displayName ?? "Google User",
              photoURL: fb.photoURL ?? "",
              role: "center_owner",
              createdAt: Date.now(),
            });
          }
        } catch {
          // Firestore write failed — continue with local account
        }
      }

      // Local-first: look up or create the RBAC user (empty db, not demo)
      const rbacUser = upsertLocalRbacFromFirebase(fb);
      // Ensure centers/{uid} document exists (in case it wasn't created at signup)
      ensureCenterDocument(fb.uid, fb.email ?? "", fb.displayName ?? "User");
      beginSession(rbacUser);
      return { ok: true };
    } catch (err: unknown) {
      const code = (err as { code?: string }).code ?? "";
      return { ok: false, error: mapFbError(code) };
    }
  }, [beginSession, upsertLocalRbacFromFirebase, ensureCenterDocument]);

  const demoAccess = useCallback(
    (as: "owner" | "secretary" | "parent") => {
      ensureSeeded(DEMO_CENTER);
      const list = loadUsers();
      let owner = list.find((u) => u.centerId === DEMO_CENTER && u.role === "OWNER");
      const ts = now();
      const next = [...list];
      if (!owner) {
        owner = {
          uid: DEMO_CENTER,
          email: "owner@demo.center",
          displayName: "Dr. Mona Adel",
          role: "OWNER",
          centerId: DEMO_CENTER,
          ownerId: DEMO_CENTER,
          permissions: ROLE_PERMS.OWNER,
          active: true,
          createdAt: ts,
          lastUpdated: ts,
        };
        next.push(owner);
      }
      let sessionUser = owner;
      if (as === "secretary") {
        let sec = list.find((u) => u.centerId === DEMO_CENTER && u.role === "SECRETARY");
        if (!sec) {
          sec = {
            uid: uid("staff"),
            email: "secretary@demo.center",
            displayName: "Sara Tarek",
            role: "SECRETARY",
            centerId: DEMO_CENTER,
            ownerId: DEMO_CENTER,
            permissions: ROLE_PERMS.SECRETARY,
            active: true,
            createdAt: ts,
            lastUpdated: ts,
          };
          next.push(sec);
        }
        sessionUser = sec;
      }
      saveUsers(next);
      setUsers(next);
      if (as === "parent") {
        beginSession({
          uid: "parent-guest",
          email: "",
          displayName: "Parent",
          role: "PARENT",
          centerId: DEMO_CENTER,
          ownerId: DEMO_CENTER,
          permissions: [],
          active: true,
          createdAt: ts,
          lastUpdated: ts,
        });
      } else {
        beginSession(sessionUser!);
      }
    },
    [beginSession, ensureSeeded],
  );

  /** Owner creates a sub-account (staff) tied to their centerId, with a password,
   *  custom permissions, optional salary and position. Returns the new uid. */
  const inviteStaff = useCallback(
    (email: string, name: string, password: string, role: Role, perms: Permission[], opts?: { salary?: number; title?: string }): { ok: boolean; error?: string; uid?: string } => {
      if (!user) return { ok: false, error: "no-session" };
      if (!email.trim() || !password.trim()) return { ok: false, error: "required" };
      const list = loadUsers();
      if (list.some((u) => u.email.toLowerCase() === email.toLowerCase()))
        return { ok: false, error: "email-exists" };
      const ts = now();
      const staffMember: UserRbac = {
        uid: uid("staff"),
        email,
        displayName: name || email.split("@")[0],
        role,
        centerId: user.centerId, // shared tenant
        ownerId: user.uid, // links back to the owner
        permissions: role === "OWNER" || role === "ADMIN" ? ROLE_PERMS[role] : perms,
        password: obfuscate(password),
        active: true,
        salary: opts?.salary ?? 0,
        title: opts?.title,
        createdAt: ts,
        lastUpdated: ts,
      };
      const next = [...list, staffMember];
      saveUsers(next);
      setUsers(next);
      queueSync(`/user_rbac/${staffMember.uid}`, "create");
      return { ok: true, uid: staffMember.uid };
    },
    [user, queueSync],
  );

  const updateStaff = useCallback((suid: string, patch: Partial<UserRbac>) => {
    const next = loadUsers().map((u) =>
      u.uid === suid ? { ...u, ...patch, password: patch.password ? obfuscate(patch.password) : u.password, lastUpdated: now() } : u,
    );
    saveUsers(next);
    setUsers(next);
    // sync the logged-in user if they edited themselves
    setUser((cur) => (cur && cur.uid === suid ? { ...cur, ...patch, lastUpdated: now() } : cur));
  }, []);

  const signOut = useCallback(() => {
    localStorage.removeItem(SESSION_KEY);
    setUser(null);
  }, []);

  const staff = useMemo(
    () => (user ? users.filter((u) => u.centerId === user.centerId) : []),
    [users, user],
  );

  const can = useCallback(
    (perm: Permission) => {
      if (!user) return false;
      if (user.role === "OWNER" || user.role === "ADMIN") return true;
      return user.permissions.includes(perm);
    },
    [user],
  );

  const canAdd = useCallback(() => {
    if (!user) return false;
    if (user.role === "OWNER" || user.role === "ADMIN") return true;
    return user.permissions.includes("data.add");
  }, [user]);

  /** Checks if the center has reached its student limit for the current plan. */
  const canAddStudent = useCallback((): boolean => {
    const limits = PLAN_LIMITS[subscriptionPlan];
    if (!limits) return true;
    return db.students.length < limits.maxStudents;
  }, [db.students.length, subscriptionPlan]);

  /** Checks if the center has reached its teacher limit for the current plan. */
  const canAddTeacher = useCallback((): boolean => {
    const limits = PLAN_LIMITS[subscriptionPlan];
    if (!limits) return true;
    return db.teachers.length < limits.maxTeachers;
  }, [db.teachers.length, subscriptionPlan]);

  const canDelete = useCallback(() => {
    if (!user) return false;
    if (user.role === "OWNER" || user.role === "ADMIN") return true;
    return user.permissions.includes("data.delete");
  }, [user]);

  // Plan limits constants (local mirror of superadmin DEFAULT_LIMITS)
  const PLAN_LIMITS: Record<string, { maxStudents: number; maxTeachers: number; maxStaff: number }> = {
    free: { maxStudents: 30, maxTeachers: 2, maxStaff: 0 },
    pro: { maxStudents: 500, maxTeachers: 30, maxStaff: 10 },
    enterprise: { maxStudents: 99999, maxTeachers: 99999, maxStaff: 99999 },
  };

  // Feature gating based on subscription plan
  const FEATURE_REQUIREMENTS: Record<string, "free" | "pro" | "enterprise"> = {
    ai_assistant: "enterprise",
    qr_scanner: "enterprise",
    multi_branch: "enterprise",
    advanced_reports: "pro",
    staff_management: "enterprise",
    excel_import: "pro",
    parent_portal: "pro",
    financial_reports: "pro",
    classes: "pro",
    assignments: "pro",
  };

  const canUseFeature = useCallback(
    (feature: string): boolean => {
      // Demo users always get enterprise for testing
      if (centerId === DEMO_CENTER) return true;
      const required = FEATURE_REQUIREMENTS[feature];
      if (!required) return true;
      if (required === "free") return true;
      if (required === "pro") return subscriptionPlan === "pro" || subscriptionPlan === "enterprise";
      return subscriptionPlan === "enterprise";
    },
    [centerId, subscriptionPlan],
  );

  /* ----------------------------- data mutations ------------------------ */
  const upsert = useCallback(
    <K extends CollKey>(coll: K, item: Collections[K]) => {
      const stamped = { ...item, lastUpdated: now() } as Collections[K] & { id: string };
      const existed = (db[coll] as unknown as Array<{ id: string }>).some((x) => x.id === stamped.id);
      setDb((prev) => {
        const arr = prev[coll] as unknown as Array<{ id: string }>;
        const exists = arr.some((x) => x.id === stamped.id);
        const nextArr = exists ? arr.map((x) => (x.id === stamped.id ? stamped : x)) : [...arr, stamped];
        queueSync(`/centers/${prev.profile.centerId}/${coll}/${stamped.id}`, exists ? "update" : "create");
        return { ...prev, [coll]: nextArr } as DatabaseShape;
      });
      // human-readable label for the staff activity log
      const labelFor = (r: Collections[K]) => {
        const any = r as unknown as Record<string, unknown>;
        return String(any.name ?? any.title ?? any.id ?? "");
      };
      recordActivity(`${coll}:${existed ? "update" : "create"}`, labelFor(item));
    },
    [queueSync, recordActivity, db],
  );

  const remove = useCallback(
    <K extends CollKey>(coll: K, id: string) => {
      const target = (db[coll] as unknown as Array<{ id: string; name?: string; title?: string }>).find((x) => x.id === id);
      setDb((prev) => {
        const arr = prev[coll] as unknown as Array<{ id: string }>;
        queueSync(`/centers/${prev.profile.centerId}/${coll}/${id}`, "delete");
        return { ...prev, [coll]: arr.filter((x) => x.id !== id) } as DatabaseShape;
      });
      recordActivity(`${coll}:delete`, target?.name ?? target?.title ?? id);
    },
    [queueSync, recordActivity],
  );

  /** Permanently remove a staff account (cannot remove the owner). */
  const deleteStaff = useCallback(
    (suid: string) => {
      const next = loadUsers().filter((u) => !(u.uid === suid && u.role !== "OWNER"));
      saveUsers(next);
      setUsers(next);
    },
    [],
  );

  /** Append a message to the thread between owner ↔ staff (stored on both). */
  const sendStaffMessage = useCallback(
    (toUid: string, text: string) => {
      if (!user || !text.trim()) return;
      const ts = now();
      const list = loadUsers();
      const msg = { id: uid("msg"), fromUid: user.uid, fromName: user.displayName, toUid, text: text.trim(), at: ts };
      const next = list.map((u) => {
        if (u.uid === toUid || u.uid === user.uid) {
          return { ...u, messages: [...(u.messages ?? []), msg], lastUpdated: ts };
        }
        return u;
      });
      saveUsers(next);
      setUsers(next);
      // keep the logged-in (sender) user object in sync so its chat updates instantly
      setUser((cur) => (cur ? { ...cur, messages: [...(cur.messages ?? []), msg], lastUpdated: ts } : cur));
    },
    [user],
  );

  const updateProfile = useCallback(
    (patch: Partial<DatabaseShape["profile"]>) => {
      setDb((prev) => ({ ...prev, profile: { ...prev.profile, ...patch, lastUpdated: now() } }));
      queueSync(`/centers/${centerId}/profile/settings`, "update");
    },
    [queueSync, centerId],
  );

  const resetData = useCallback(() => {
    const fresh = seedDb(centerId);
    setDb(fresh);
  }, [centerId]);

  const replaceDb = useCallback((next: DatabaseShape) => setDb(next), []);

  /* --------------------------- academic year --------------------------- */
  /** Promote all graded students to the next grade (course students are
   *  skipped). Automatically takes a backup first. */
  const promoteYear = useCallback(() => {
    const backupTs = saveBackup(centerId, "before-promotion");
    setBackups(listBackups(centerId));
    // Count from the current db (closure) so counts stay correct even under
    // StrictMode double-invocation of the state updater.
    let promoted = 0;
    let skipped = 0;
    for (const s of db.students) {
      if (nextGrade(s.grade)) promoted++;
      else skipped++;
    }
    setDb((prev) => ({
      ...prev,
      students: prev.students.map((s) => {
        const ng = nextGrade(s.grade);
        return ng ? { ...s, grade: ng, lastUpdated: now() } : s;
      }),
    }));
    queueSync(`/centers/${centerId}/profile/settings`, "update");
    return { promoted, skipped, backupTs };
  }, [centerId, db, queueSync]);

  /* ------------------------------ backup ------------------------------- */
  const createBackup = useCallback(
    (label = "manual") => {
      saveBackup(centerId, label);
      setBackups(listBackups(centerId));
    },
    [centerId],
  );

  /** Fetches the subscription plan from Firestore and updates local state. */
  const refreshSubscriptionPlan = useCallback(async (): Promise<void> => {
    if (!FIREBASE_ENABLED || !firestoreDb || !centerId || centerId === DEMO_CENTER) return;
    try {
      const snap = await getDoc(doc(firestoreDb, "centers", centerId));
      if (snap.exists()) {
        const data = snap.data();
        const plan = (data.subscriptionPlan as "free" | "pro" | "enterprise") || "free";
        const status = data.subscriptionStatus as string;
        const endDate = (data.subscriptionEndDate as number) ?? null;
        // Check if subscription is expired
        if (status === "active" && endDate && endDate < Date.now()) {
          // Subscription expired — downgrade to free
          setSubscriptionPlanState("free");
          localStorage.setItem(`cpd_plan_${centerId}`, JSON.stringify("free"));
          setSubscriptionEndDate(null);
          localStorage.setItem(`cpd_plan_end_${centerId}`, JSON.stringify(null));
        } else {
          setSubscriptionPlanState(plan);
          localStorage.setItem(`cpd_plan_${centerId}`, JSON.stringify(plan));
          setSubscriptionEndDate(endDate);
          localStorage.setItem(`cpd_plan_end_${centerId}`, JSON.stringify(endDate));
        }
      }
    } catch {
      // non-blocking
    }
  }, [centerId]);

  // Refresh subscription plan when user logs in or center changes
  useEffect(() => {
    if (user && centerId !== DEMO_CENTER) {
      refreshSubscriptionPlan();
      // Also refresh every 5 minutes to catch admin changes
      const interval = setInterval(refreshSubscriptionPlan, 5 * 60 * 1000);
      return () => clearInterval(interval);
    }
  }, [user, centerId, refreshSubscriptionPlan]);

  const restoreFromBackup = useCallback(
    (ts: number) => {
      restoreBackup(centerId, ts);
      setDb(loadDb(centerId) ?? seedDb(centerId));
      setBackups(listBackups(centerId));
    },
    [centerId],
  );

  const removeBackup = useCallback(
    (ts: number) => {
      deleteBackup(centerId, ts);
      setBackups(listBackups(centerId));
    },
    [centerId],
  );

  const exportBackup = useCallback(() => {
    downloadBackupFile(db, centerId);
  }, [db, centerId]);

  /** Restore a dataset uploaded from the user's device (JSON backup file). */
  const restoreBackupFromFile = useCallback(
    async (file: File) => {
      try {
        const parsed = await parseBackupFile(file);
        const backupFile: BackupFile = { ...parsed, centerId };
        restoreFromFile(centerId, backupFile);
        setDb(loadDb(centerId) ?? seedDb(centerId));
        setBackups(listBackups(centerId));
        return true;
      } catch {
        return false;
      }
    },
    [centerId],
  );

  const value: AppContextValue = {
    lang,
    dir: lang === "ar" ? "rtl" : "ltr",
    t,
    setLang,
    toggleLang,
    theme,
    toggleTheme,
    fontScale,
    setFontScale,
    currentBranchId,
    switchBranch,
    user,
    signIn,
    signUp,
    signInWithGoogle,
    signOut,
    demoAccess,
    inviteStaff,
    updateStaff,
    deleteStaff,
    sendStaffMessage,
    staff,
    staffActivity,
    can,
    canAdd,
    canAddStudent,
    canAddTeacher,
    canDelete,
    subscriptionPlan,
    subscriptionEndDate,
    refreshSubscriptionPlan,
    canUseFeature,
    db,
    upsert,
    remove,
    updateProfile,
    resetData,
    replaceDb,
    restoreBackupFromFile,
    promoteYear,
    backups,
    createBackup,
    restoreFromBackup,
    removeBackup,
    exportBackup,
    syncStatus,
    online,
    setOnline: setOnlineState,
    lastSync,
    syncLog,
    pendingCount: syncLog.filter((e) => e.status === "queued").length,
    flushNow: flush,
  };

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useApp() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}

export { emptyDb };
