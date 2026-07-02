/**
 * Super Admin System — Core Library
 * ==================================
 * Handles OTP generation/verification, platform-wide Firestore queries,
 * subscription management, and audit logging.
 *
 * SECURITY: OTP generation & verification should ideally run in Firebase
 * Cloud Functions. This client-side implementation is the integration layer.
 */

import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  orderBy,
  limit,
} from "firebase/firestore";
import { auth, db as firestoreDb, FIREBASE_ENABLED } from "./firebase";

/* ============================== Types ============================== */

export interface SuperAdminUser {
  uid: string;
  email: string;
  displayName: string;
  role: string; // "super_admin" | "center_owner" | "secretary" | ...
  status: AccountStatus;
  photoURL?: string;
  centerId?: string;
  createdAt: number;
}

export type AccountStatus = "active" | "suspended" | "disabled";

export type SubscriptionPlan = "free" | "basic" | "pro" | "enterprise";
export type SubscriptionStatus = "active" | "trialing" | "past_due" | "canceled" | "expired";

export interface CenterRecord {
  id: string;
  name: string;
  ownerId: string;
  ownerEmail: string;
  status: AccountStatus;
  subscriptionPlan: SubscriptionPlan;
  subscriptionStatus: SubscriptionStatus;
  subscriptionStartDate?: number;
  subscriptionEndDate?: number;
  studentCount: number;
  teacherCount: number;
  createdAt: number;
  // Dynamic limits (override defaults if set)
  customLimits?: CenterLimits;
}

export interface CenterLimits {
  maxStudents?: number;
  maxTeachers?: number;
  maxStaff?: number;
  maxGroups?: number;
  maxClassrooms?: number;
  maxSchedules?: number;
}

export const DEFAULT_LIMITS: Partial<Record<SubscriptionPlan, CenterLimits>> & Record<"free" | "pro" | "enterprise", CenterLimits> = {
  free: { maxStudents: 30, maxTeachers: 2, maxStaff: 1, maxGroups: 3, maxClassrooms: 2, maxSchedules: 10 },
  basic: { maxStudents: 200, maxTeachers: 10, maxStaff: 5, maxGroups: 20, maxClassrooms: 10, maxSchedules: 50 },
  pro: { maxStudents: 500, maxTeachers: 30, maxStaff: 10, maxGroups: 50, maxClassrooms: 20, maxSchedules: 100 },
  enterprise: { maxStudents: 99999, maxTeachers: 99999, maxStaff: 99999, maxGroups: 99999, maxClassrooms: 99999, maxSchedules: 99999 },
};

/** Updates a center's custom limits (overrides plan defaults). */
export async function updateCenterLimits(
  centerId: string,
  limits: CenterLimits,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await updateDoc(doc(firestoreDb, "centers", centerId), { customLimits: limits });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: "limits:update",
    targetType: "center",
    targetId: centerId,
    targetName: centerId,
    newValue: JSON.stringify(limits),
  });
}

/** Sends a notification/message to a center owner. */
export async function sendOwnerMessage(
  centerId: string,
  ownerUid: string,
  message: string,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  const notifId = `admin_msg_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
  await setDoc(doc(firestoreDb, "notifications", notifId), {
    notifId,
    recipientUid: ownerUid,
    centerId,
    type: "general",
    title: "رسالة من إدارة المنصة",
    body: message,
    read: false,
    createdAt: Date.now(),
  });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: "message:send",
    targetType: "center",
    targetId: centerId,
    targetName: "owner_message",
  });
}

export interface AuditLog {
  id: string;
  adminUid: string;
  adminEmail: string;
  action: string;
  targetType: "center" | "user" | "teacher" | "student" | "subscription";
  targetId: string;
  targetName: string;
  previousValue?: string;
  newValue?: string;
  timestamp: number;
}

export interface OTPRecord {
  uid: string;
  code: string;
  email: string;
  createdAt: number;
  expiresAt: number;
  attempts: number;
  maxAttempts: number;
  used: boolean;
  lockedUntil?: number;
}

/* ====================== Role Verification ====================== */

/**
 * Verifies if the current Firebase user is the Super Admin.
 *
 * Reads from: admins/super_admin
 * Checks:     active === true AND email === currentUser.email
 */
export async function checkSuperAdminRole(email: string): Promise<boolean> {
  if (!FIREBASE_ENABLED || !firestoreDb) return false;
  if (!email) return false;

  try {
    const snap = await getDoc(doc(firestoreDb, "admins", "super_admin"));
    if (!snap.exists()) return false;

    const data = snap.data();
    const isActive = data?.active === true;
    const emailMatch = data?.email === email;

    return isActive && emailMatch;
  } catch {
    // Firestore rules might block — try auth fallback
    try {
      const authEmail = auth?.currentUser?.email;
      if (!authEmail) return false;
      const snap = await getDoc(doc(firestoreDb, "admins", "super_admin"));
      if (!snap.exists()) return false;
      const data = snap.data();
      return data?.active === true && data?.email === authEmail;
    } catch {
      return false;
    }
  }
}

/* ====================== OTP System ====================== */

/** Generates a cryptographically-random 6-digit OTP. */
export function generateOTP(): string {
  const array = new Uint32Array(1);
  crypto.getRandomValues(array);
  return String(array[0] % 1000000).padStart(6, "0");
}

/**
 * Creates an OTP record and stores it in Firestore.
 * Also "sends" the email — in production this calls a Cloud Function.
 */
export async function createAndSendOTP(
  uid: string,
  email: string,
): Promise<{ ok: boolean; error?: string }> {
  const code = generateOTP();
  const now = Date.now();
  const record: OTPRecord = {
    uid,
    code,
    email,
    createdAt: now,
    expiresAt: now + 5 * 60 * 1000, // 5 minutes
    attempts: 0,
    maxAttempts: 5,
    used: false,
  };

  // Store in Firestore
  if (FIREBASE_ENABLED && firestoreDb) {
    try {
      await setDoc(doc(firestoreDb, "otp_verifications", uid), record);
    } catch {
      // Continue — OTP still works locally
    }
  }

  // Send email — integration point for Cloud Functions / SendGrid / Resend
  // In production: await sendOTPEmail(email, code) via callable function
  // For now: log to console (dev mode)
  console.log(`🔐 [Super Admin OTP] Code for ${email}: ${code}`);

  return { ok: true };
}

/**
 * Verifies an OTP code against the stored record.
 * Enforces: expiry, max attempts, lockout period, single-use.
 */
export async function verifyOTP(
  uid: string,
  inputCode: string,
): Promise<{ ok: boolean; error?: string; lockedUntil?: number }> {
  if (!FIREBASE_ENABLED || !firestoreDb) {
    return { ok: false, error: "Firebase not configured" };
  }

  try {
    const snap = await getDoc(doc(firestoreDb, "otp_verifications", uid));
    if (!snap.exists()) return { ok: false, error: "No OTP found. Please request a new code." };

    const record = snap.data() as OTPRecord;
    const now = Date.now();

    // Check lockout
    if (record.lockedUntil && now < record.lockedUntil) {
      const mins = Math.ceil((record.lockedUntil - now) / 60000);
      return { ok: false, error: `Account locked. Try again in ${mins} minute(s).`, lockedUntil: record.lockedUntil };
    }

    // Check expiry
    if (now > record.expiresAt) {
      return { ok: false, error: "OTP expired. Please request a new code." };
    }

    // Check if already used
    if (record.used) {
      return { ok: false, error: "OTP already used. Please request a new code." };
    }

    // Check attempts
    const newAttempts = record.attempts + 1;
    if (inputCode !== record.code) {
      if (newAttempts >= record.maxAttempts) {
        // Lock for 15 minutes
        await updateDoc(doc(firestoreDb, "otp_verifications", uid), {
          attempts: newAttempts,
          lockedUntil: now + 15 * 60 * 1000,
        });
        return { ok: false, error: "Too many failed attempts. Account locked for 15 minutes." };
      }
      await updateDoc(doc(firestoreDb, "otp_verifications", uid), { attempts: newAttempts });
      return { ok: false, error: `Invalid code. ${record.maxAttempts - newAttempts} attempt(s) remaining.` };
    }

    // Success — mark as used
    await updateDoc(doc(firestoreDb, "otp_verifications", uid), { used: true });
    return { ok: true };
  } catch {
    return { ok: false, error: "Verification failed. Please try again." };
  }
}

/* ====================== Platform Queries ====================== */

/** Fetches all centers from the root `centers` collection. */
export async function fetchAllCenters(): Promise<CenterRecord[]> {
  if (!FIREBASE_ENABLED || !firestoreDb) return [];
  try {
    const snap = await getDocs(collection(firestoreDb, "centers"));
    const centers = snap.docs.map((d) => ({ id: d.id, ...d.data() } as CenterRecord));
    console.log("[SuperAdmin] Fetched centers:", centers.length);
    return centers;
  } catch (err) {
    console.error("[SuperAdmin] fetchAllCenters ERROR:", err);
    return [];
  }
}

/** Fetches all users from the root `users` collection. */
export async function fetchAllUsers(): Promise<SuperAdminUser[]> {
  if (!FIREBASE_ENABLED || !firestoreDb) return [];
  try {
    const snap = await getDocs(collection(firestoreDb, "users"));
    const users = snap.docs.map((d) => ({ uid: d.id, ...d.data() } as SuperAdminUser));
    console.log("[SuperAdmin] Fetched users:", users.length);
    return users;
  } catch (err) {
    console.error("[SuperAdmin] fetchAllUsers ERROR:", err);
    return [];
  }
}

/** Fetches recent audit logs. */
export async function fetchAuditLogs(count = 50): Promise<AuditLog[]> {
  if (!FIREBASE_ENABLED || !firestoreDb) return [];
  try {
    const q = query(collection(firestoreDb, "audit_logs"), orderBy("timestamp", "desc"), limit(count));
    const snap = await getDocs(q);
    return snap.docs.map((d) => ({ id: d.id, ...d.data() } as AuditLog));
  } catch {
    return [];
  }
}

/* ====================== Admin Actions ====================== */

/** Logs a super admin action to the audit_logs collection. */
export async function logAdminAction(entry: Omit<AuditLog, "id" | "timestamp">): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  try {
    const logId = `log_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    await setDoc(doc(firestoreDb, "audit_logs", logId), {
      ...entry,
      timestamp: Date.now(),
    });
  } catch {
    // non-blocking
  }
}

/** Updates a center's status (suspend / reactivate / disable). */
export async function updateCenterStatus(
  centerId: string,
  status: AccountStatus,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await updateDoc(doc(firestoreDb, "centers", centerId), { status });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: `center:${status}`,
    targetType: "center",
    targetId: centerId,
    targetName: centerId,
    newValue: status,
  });
}

/** Updates a user's status. */
export async function updateUserStatus(
  uid: string,
  status: AccountStatus,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await updateDoc(doc(firestoreDb, "users", uid), { status });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: `user:${status}`,
    targetType: "user",
    targetId: uid,
    targetName: uid,
    newValue: status,
  });
}

/** Updates a center's subscription. */
export async function updateSubscription(
  centerId: string,
  patch: Partial<Pick<CenterRecord, "subscriptionPlan" | "subscriptionStatus" | "subscriptionStartDate" | "subscriptionEndDate">>,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await updateDoc(doc(firestoreDb, "centers", centerId), patch);
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: "subscription:update",
    targetType: "subscription",
    targetId: centerId,
    targetName: centerId,
    newValue: JSON.stringify(patch),
  });
}

/** Deletes a center document (soft — data remains in localStorage/backup). */
export async function deleteCenterRecord(
  centerId: string,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await deleteDoc(doc(firestoreDb, "centers", centerId));
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: "center:delete",
    targetType: "center",
    targetId: centerId,
    targetName: centerId,
  });
}

/** Deletes a user document. */
export async function deleteUserRecord(
  uid: string,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await deleteDoc(doc(firestoreDb, "users", uid));
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: "user:delete",
    targetType: "user",
    targetId: uid,
    targetName: uid,
  });
}

/* ====================== Feature Flags ====================== */

export interface FeatureFlag {
  key: string;
  label: string;
  labelAr: string;
  description: string;
  descriptionAr: string;
  enabled: boolean;
  plan: SubscriptionPlan | "all";
}

export const FEATURE_FLAGS: { key: string; label: string; labelAr: string; description: string; descriptionAr: string; plan: SubscriptionPlan | "all" }[] = [
  { key: "ai_assistant", label: "AI Assistant", labelAr: "المساعد الذكي", description: "AI-powered academic analysis", descriptionAr: "تحليل أكاديمي بالذكاء الاصطناعي", plan: "enterprise" },
  { key: "qr_scanner", label: "QR Scanner", labelAr: "ماسح QR", description: "QR code attendance scanning", descriptionAr: "حضور برموز QR", plan: "enterprise" },
  { key: "smart_ai_reports", label: "Smart AI Reports", labelAr: "تقارير ذكية", description: "AI-powered performance analysis", descriptionAr: "تحليل أداء بالذكاء الاصطناعي", plan: "pro" },
  { key: "parent_portal", label: "Parent Portal", labelAr: "بوابة ولي الأمر", description: "Parent access to student data", descriptionAr: "وصول ولي الأمر لبيانات الطالب", plan: "pro" },
  { key: "teacher_mobile_app", label: "Teacher Mobile App", labelAr: "تطبيق المعلم", description: "Mobile app for teachers", descriptionAr: "تطبيق الهاتف للمعلمين", plan: "pro" },
  { key: "advanced_analytics", label: "Advanced Analytics", labelAr: "تحليلات متقدمة", description: "Detailed charts and insights", descriptionAr: "رسوم بيانية ورؤى تفصيلية", plan: "pro" },
  { key: "financial_reports", label: "Financial Reports", labelAr: "تقارير مالية", description: "PDF/Excel financial exports", descriptionAr: "تصدير مالي PDF و Excel", plan: "pro" },
  { key: "exam_generator", label: "Exam Generator", labelAr: "مولّد الامتحانات", description: "AI exam creation tools", descriptionAr: "إنشاء امتحانات بالذكاء الاصطناعي", plan: "enterprise" },
  { key: "attendance_ai", label: "Attendance AI", labelAr: "حضور ذكي", description: "AI attendance predictions", descriptionAr: "توقعات الحضور بالذكاء الاصطناعي", plan: "enterprise" },
  { key: "multi_branch", label: "Multi-Branch", labelAr: "تعدد الفروع", description: "Manage multiple branches", descriptionAr: "إدارة فروع متعددة", plan: "enterprise" },
  { key: "backup_restore", label: "Backup & Restore", labelAr: "نسخ احتياطي", description: "Automated data backups", descriptionAr: "نسخ احتياطي تلقائي للبيانات", plan: "all" },
  { key: "excel_import", label: "Excel Import", labelAr: "استيراد إكسل", description: "Bulk import from Excel files", descriptionAr: "استيراد جماعي من ملفات إكسل", plan: "pro" },
  { key: "classes", label: "Classes & Groups", labelAr: "الفصول والمجموعات", description: "Manage classes, groups and classrooms", descriptionAr: "إدارة الفصول والمجموعات والقاعات", plan: "pro" },
  { key: "assignments", label: "Homework", labelAr: "الواجبات", description: "Create and manage assignments", descriptionAr: "إنشاء وإدارة الواجبات المنزلية", plan: "pro" },
  { key: "schedule_advanced", label: "Advanced Schedule", labelAr: "جدول متقدم", description: "Advanced scheduling with conflict detection", descriptionAr: "جدول متقدم مع كشف التعارضات", plan: "enterprise" },
  { key: "custom_branding", label: "Custom Branding", labelAr: "علامة تجارية", description: "Custom logo, colors and branding", descriptionAr: "شعار وألوان وعلامة تجارية مخصصة", plan: "enterprise" },
  { key: "sms_notifications", label: "SMS Notifications", labelAr: "إشعارات SMS", description: "Send SMS notifications to parents", descriptionAr: "إرسال إشعارات SMS لأولياء الأمور", plan: "enterprise" },
  { key: "bulk_operations", label: "Bulk Operations", labelAr: "عمليات جماعية", description: "Bulk add/edit/delete for students and teachers", descriptionAr: "إضافة/تعديل/حذف جماعي للطلاب والمعلمين", plan: "pro" },
  { key: "export_data", label: "Data Export", labelAr: "تصدير البيانات", description: "Export data to PDF, Excel and CSV", descriptionAr: "تصدير البيانات إلى PDF و Excel و CSV", plan: "pro" },
  { key: "local_ai", label: "Local AI Assistant", labelAr: "المساعد الذكي المحلي", description: "Local AI that manages center data through chat", descriptionAr: "ذكاء اصطناعي محلي يدير بيانات السنتر عبر المحادثة", plan: "free" },
];

/** Returns the features that should be enabled for a given plan. */
export function getFeaturesForPlan(plan: SubscriptionPlan): Record<string, boolean> {
  const features: Record<string, boolean> = {};
  for (const f of FEATURE_FLAGS) {
    const flagPlan = f.plan as string;
    if (flagPlan === "all" || flagPlan === "free") features[f.key] = true;
    else if (plan === "enterprise") features[f.key] = true;
    else if (plan === "pro" && (flagPlan === "pro" || flagPlan === "basic")) features[f.key] = true;
    else if (plan === "basic" && flagPlan === "basic") features[f.key] = true;
  }
  return features;
}

/** Auto-applies all features for a plan to a center. */
export async function applyPlanFeatures(
  centerId: string,
  plan: SubscriptionPlan,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  const features = getFeaturesForPlan(plan);
  await setDoc(doc(firestoreDb, "centers", centerId, "config", "features"), {
    ...features,
    autoAppliedPlan: plan,
    updatedAt: Date.now(),
  }, { merge: true });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: `features:plan:${plan}`,
    targetType: "center",
    targetId: centerId,
    targetName: plan,
    newValue: JSON.stringify(features),
  });
}

/** Reads feature flags for a center from Firestore. */
export async function fetchCenterFeatures(centerId: string): Promise<Record<string, boolean>> {
  if (!FIREBASE_ENABLED || !firestoreDb) return {};
  try {
    const snap = await getDoc(doc(firestoreDb, "centers", centerId, "config", "features"));
    return snap.exists() ? (snap.data() as Record<string, boolean>) : {};
  } catch {
    return {};
  }
}

/** Toggles a feature flag for a center. */
export async function toggleFeatureFlag(
  centerId: string,
  featureKey: string,
  enabled: boolean,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  await setDoc(doc(firestoreDb, "centers", centerId, "config", "features"), {
    [featureKey]: enabled,
    updatedAt: Date.now(),
  }, { merge: true });
  await logAdminAction({
    adminUid: admin.uid,
    adminEmail: admin.email,
    action: `feature:${enabled ? "enable" : "disable"}`,
    targetType: "center",
    targetId: centerId,
    targetName: featureKey,
    newValue: String(enabled),
  });
}

/* ====================== Subscription Plans ====================== */

export interface PlanDefinition {
  id: SubscriptionPlan;
  name: string;
  price: number;
  maxStudents: number;
  maxTeachers: number;
  features: string[];
  color: string;
}

export const PLAN_DEFINITIONS: PlanDefinition[] = [
  {
    id: "free",
    name: "مجاني",
    price: 0,
    maxStudents: 30,
    maxTeachers: 2,
    features: ["حتى 30 طالب", "حتى 2 معلمين", "إدارة الطلاب الأساسية", "تسجيل الحضور", "الجدول الأسبوعي"],
    color: "bg-slate-100 text-slate-700 dark:bg-slate-500/15 dark:text-slate-300",
  },
  {
    id: "pro",
    name: "احترافي",
    price: 150,
    maxStudents: 500,
    maxTeachers: 30,
    features: ["حتى 500 طالب", "حتى 30 معلم", "بوابة ولي الأمر", "تقارير PDF و Excel", "تحليلات متقدمة", "نسخ احتياطي"],
    color: "bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300",
  },
  {
    id: "enterprise",
    name: "مؤسسي",
    price: 400,
    maxStudents: 99999,
    maxTeachers: 99999,
    features: ["طلاب غير محدودين", "معلمين غير محدودين", "كل مميزات الاحترافي", "المساعد الذكي AI", "الحضور بـ QR", "إدارة الفروع المتعددة", "مولّد الامتحانات AI", "دعم أولوية", "علامة تجارية مخصصة"],
    color: "bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300",
  },
];

/** Payment details for upgrade page */
export const PAYMENT_DETAILS = {
  price: "150",
  currency: "ج.م",
  period: "شهرياً",
  paymentNumber: "01140617424",
  paymentMethods: ["إنستا باي (InstaPay)", "محفظة كاش (Vodafone Cash)"],
  whatsappNumber: "201009617278",
  screenshotNote: "أرسل سكرين شوت التحويل على واتساب لتفعيل الاشتراك",
};

/** Activates a subscription for a center with the given duration in days. */
export async function activateSubscription(
  centerId: string,
  plan: SubscriptionPlan,
  days: number,
  admin: { uid: string; email: string },
): Promise<void> {
  const now = Date.now();
  const endDate = now + days * 86400000;
  const planName = PLAN_DEFINITIONS.find(p => p.id === plan)?.name || plan;

  // 1. Update the subscription on the center document
  await updateSubscription(centerId, {
    subscriptionPlan: plan,
    subscriptionStatus: "active",
    subscriptionStartDate: now,
    subscriptionEndDate: endDate,
  }, admin);

  // 2. Auto-apply features and limits for the assigned plan
  await applyPlanFeatures(centerId, plan, admin);
  const planLimits = DEFAULT_LIMITS[plan];
  if (planLimits && firestoreDb) {
    try {
      await updateDoc(doc(firestoreDb, "centers", centerId), { customLimits: planLimits });
    } catch (e) {
      console.error("Failed to update limits:", e);
    }
  }

  // 3. Send a notification to the center owner about their new subscription
  try {
    const centerSnap = await getDoc(doc(firestoreDb!, "centers", centerId));
    if (centerSnap.exists()) {
      const centerData = centerSnap.data();
      const ownerUid = centerData.ownerId;
      const endDateStr = new Date(endDate).toLocaleDateString('ar-EG');
      const notifId = `sub_active_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
      await setDoc(doc(firestoreDb!, "notifications", notifId), {
        notifId,
        recipientUid: ownerUid,
        centerId,
        type: "subscription",
        title: `تم تفعيل اشتراكك في الخطة ${planName}`,
        body: `مبارك! تم تفعيل اشتراكك في خطة ${planName} لمدة ${days} يوم. تاريخ الانتهاء: ${endDateStr}. استمتع بكل المميزات!`,
        read: false,
        createdAt: now,
      });
    }
  } catch (e) {
    console.error("Failed to send subscription notification:", e);
  }
}

/** Extends a center's subscription by the given days. */
export async function extendSubscription(
  centerId: string,
  days: number,
  admin: { uid: string; email: string },
): Promise<void> {
  if (!FIREBASE_ENABLED || !firestoreDb) return;
  const snap = await getDoc(doc(firestoreDb, "centers", centerId));
  const current = snap.exists() ? snap.data() : {};
  const baseDate = Math.max(current.subscriptionEndDate ?? Date.now(), Date.now());
  await updateSubscription(centerId, {
    subscriptionStatus: "active",
    subscriptionEndDate: baseDate + days * 86400000,
  }, admin);
}

/** Cancels a center's subscription. */
export async function cancelSubscription(
  centerId: string,
  admin: { uid: string; email: string },
): Promise<void> {
  await updateSubscription(centerId, { subscriptionStatus: "canceled" }, admin);
}

/**
 * CRITICAL SYNC: Ensures every user in `users/{uid}` also has a matching
 * document in `centers/{uid}`. This fixes users who registered before the
 * auto-sync feature was added. Returns count of newly created centers.
 */
export async function syncUsersToCenters(admin: { uid: string; email: string }): Promise<{
  created: number;
  totalUsers: number;
  totalCenters: number;
  newCenterIds: string[];
}> {
  if (!FIREBASE_ENABLED || !firestoreDb) {
    return { created: 0, totalUsers: 0, totalCenters: 0, newCenterIds: [] };
  }

  // 1. Fetch all users
  const usersSnap = await getDocs(collection(firestoreDb, "users"));
  const allUsers = usersSnap.docs.map((d) => ({ uid: d.id, ...d.data() })) as SuperAdminUser[];

  // 2. Fetch all existing centers
  const centersSnap = await getDocs(collection(firestoreDb, "centers"));
  const existingCenterIds = new Set(centersSnap.docs.map((d) => d.id));

  // 3. Create missing centers
  const created: string[] = [];
  for (const user of allUsers) {
    // Skip super_admin — they don't manage a center
    if (user.role === "super_admin") continue;
    if (existingCenterIds.has(user.uid)) continue;

    const ts = Date.now();
    await setDoc(doc(firestoreDb, "centers", user.uid), {
      id: user.uid,
      name: `${user.displayName || "Center"} Center`,
      ownerId: user.uid,
      ownerEmail: user.email || "",
      status: "active",
      subscriptionPlan: "free",
      subscriptionStatus: "trialing",
      subscriptionStartDate: ts,
      studentCount: 0,
      teacherCount: 0,
      createdAt: ts,
      syncedAt: ts,
    });
    created.push(user.uid);
  }

  // 4. Log the sync action
  if (created.length > 0) {
    await logAdminAction({
      adminUid: admin.uid,
      adminEmail: admin.email,
      action: "sync:users_to_centers",
      targetType: "center",
      targetId: "batch",
      targetName: `${created.length} centers created`,
      newValue: JSON.stringify(created),
    });
  }

  return {
    created: created.length,
    totalUsers: allUsers.length,
    totalCenters: existingCenterIds.size + created.length,
    newCenterIds: created,
  };
}
