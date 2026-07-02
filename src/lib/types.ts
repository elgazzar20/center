/**
 * Center Plus Desktop — Domain Models
 * Local-first data layer. Every record carries `lastUpdated` (epoch ms)
 * used by the sync engine to resolve conflicts (latest write wins).
 */

export type Role = "OWNER" | "ADMIN" | "SECRETARY" | "TEACHER" | "PARENT" | "super_admin";

/** A message exchanged between the owner and a staff member. */
export interface StaffMessage {
  id: string;
  fromUid: string; // sender uid
  fromName: string;
  toUid: string; // recipient uid
  text: string;
  at: number;
  read?: boolean;
}

/** /user_rbac/{userId} — maps an authenticated user to a center + role */
export interface UserRbac {
  uid: string;
  email: string;
  displayName: string;
  role: Role;
  centerId: string; // OWNER => own uid; staff => owner's uid
  permissions: string[]; // granular permission keys
  photoUrl?: string;
  /** Obfuscated password (owner-created accounts sign in with email + password). */
  password?: string;
  /** uid of the owner account this staff member belongs to. */
  ownerId?: string;
  active: boolean;
  /** Fixed monthly salary the center pays this staff member. */
  salary?: number;
  /** Title / position note shown on the profile. */
  title?: string;
  /** Thread of messages between owner ↔ staff. */
  messages?: StaffMessage[];
  createdAt: number;
  lastUpdated: number;
}

/** /centers/{centerId}/branches/{branchId} */
export interface Branch {
  id: string;
  name: string;
  address?: string;
  phone?: string;
  manager?: string;
  isMain: boolean;
  lastUpdated: number;
}

/** /centers/{centerId}/profile/settings */
export interface CenterProfile {
  centerId: string;
  name: string;
  currency: string; // currency CODE, e.g. "EGP", "USD"
  locale: string;
  phone?: string;
  address?: string;
  logoText?: string;
  lastUpdated: number;
}

/** A teacher attached to a student, each with its own monthly fee. */
export interface StudentTeacher {
  teacherId: string;
  fee: number;
}

export interface Student {
  id: string; // STU_XXXX
  name: string;
  grade: string; // grade id from GRADES or a custom value
  groupIds: string[]; // groups the student belongs to (may be empty)
  teachers: StudentTeacher[]; // one or more teachers, each with its own fee
  studentPhone?: string;
  parentName?: string;
  parentPhone?: string;
  discount: number;
  isExempt: boolean;
  qrCode: string;
  registrationDate: number;
  lastUpdated: number;
}

export type TeacherPayType = "percentage" | "fixed";

export interface Teacher {
  id: string;
  name: string;
  phone?: string;
  email?: string;
  subjects: string[]; // one or more subjects
  payType: TeacherPayType; // center takes a % OR teacher pays a flat fee
  commissionRate: number; // % center share when payType === "percentage"
  fixedAmount: number; // flat fee paid to center when payType === "fixed"
  color?: string;
  notes?: string; // private memo on the teacher profile
  lastUpdated: number;
}

export interface Group {
  id: string;
  name: string;
  teacherId?: string;
  grade?: string; // grade this group targets
  subject: string;
  /** Weekly meeting days (1=Mon..7=Sun). Drives expected attendance so a
   *  student is never counted absent on a non-meeting day. */
  days: DayOfWeek[];
  scheduleDescription?: string;
  lastUpdated: number;
}

export interface Classroom {
  id: string;
  name: string;
  capacity: number;
  notes?: string;
  lastUpdated: number;
}

export type DayOfWeek = 1 | 2 | 3 | 4 | 5 | 6 | 7; // Mon..Sun

export interface ScheduleEvent {
  id: string;
  groupId: string;
  classroomId: string;
  dayOfWeek: DayOfWeek;
  startTime: string; // "HH:mm" (24h, stored for conflict math)
  endTime: string; // "HH:mm"
  locked?: boolean; // owner can lock a slot to prevent double booking
  lastUpdated: number;
}

export type AttendanceStatus = "PRESENT" | "ABSENT" | "EXCUSED" | "LATE";

export interface AttendanceRecord {
  id: string;
  studentId: string;
  groupId: string;
  date: number; // epoch ms (start of day)
  status: AttendanceStatus;
  tempDegree?: number;
  notes?: string;
  lastUpdated: number;
}

export type PaymentType =
  | "MONTHLY_FEE"
  | "EXAM_FEE"
  | "BOOKS"
  | "CENTER_SUBSCRIPTION"
  | "OTHER";

export interface Payment {
  id: string;
  studentId: string;
  amount: number;
  date: number;
  type: PaymentType;
  month: string; // "yyyy-MM"
  teacherId?: string; // which teacher this fee is allocated to (if any)
  forCenter?: boolean; // a center subscription payment (no teacher)
  notes?: string;
  lastUpdated: number;
}

export type ExpenseCategory =
  | "Rent"
  | "Salaries"
  | "Electricity"
  | "Internet"
  | "Tools"
  | "Other";

export interface Expense {
  id: string;
  title: string;
  amount: number;
  category: ExpenseCategory;
  date: number;
  notes?: string;
  lastUpdated: number;
}

export interface Exam {
  id: string;
  groupId: string;
  name: string;
  maxGrade: number;
  date: number;
  lastUpdated: number;
}

export interface ExamGrade {
  id: string;
  examId: string;
  studentId: string;
  obtainedGrade: number;
  notes?: string;
  published?: boolean; // sent to the parent portal as a notification
  publishedAt?: number;
  lastUpdated: number;
}

export interface Assignment {
  id: string;
  groupId: string;
  title: string;
  description?: string;
  dueDate: number;
  lastUpdated: number;
}

export interface StudentNote {
  id: string;
  studentId: string;
  teacherId?: string;
  text: string;
  date: number;
  lastUpdated: number;
}

/** Shape of the whole local database (one tenant's dataset). */
export interface DatabaseShape {
  version: number;
  profile: CenterProfile;
  students: Student[];
  teachers: Teacher[];
  groups: Group[];
  classrooms: Classroom[];
  scheduleEvents: ScheduleEvent[];
  attendance: AttendanceRecord[];
  payments: Payment[];
  expenses: Expense[];
  exams: Exam[];
  examGrades: ExamGrade[];
  assignments: Assignment[];
  studentNotes: StudentNote[];
  branches: Branch[];
}

export type SyncStatus = "online" | "syncing" | "offline";

/** A single entry in the local-first sync queue / activity log. */
export interface SyncLogEntry {
  id: string;
  path: string;
  op: "create" | "update" | "delete";
  at: number;
  status: "queued" | "pushed";
}
