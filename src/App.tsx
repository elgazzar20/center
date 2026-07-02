import { useEffect, useState } from "react";
import { AppProvider, useApp } from "./context/AppContext";
import { Toaster } from "./components/ui";
import { Layout } from "./components/Layout";
import { Welcome } from "./pages/Welcome";
import { LoginPage } from "./pages/LoginPage";
import { Dashboard } from "./pages/Dashboard";
import { Students } from "./pages/Students";
import { Teachers } from "./pages/Teachers";
import { Classes } from "./pages/Classes";
import { Schedule } from "./pages/Schedule";
import { Attendance } from "./pages/Attendance";
import { Finance } from "./pages/Finance";
import { Exams } from "./pages/Exams";
import { Reports } from "./pages/Reports";
import { Staff } from "./pages/Staff";
import { Branches } from "./pages/Branches";
import { Messages } from "./pages/Messages";
import { ParentPortal } from "./pages/ParentPortal";
import { AIAssistant } from "./pages/AIAssistant";
import { Settings } from "./pages/Settings";
import { SuperAdminDashboard } from "./pages/superadmin/SuperAdminDashboard";
import { Upgrade } from "./pages/Upgrade";
import { checkSuperAdminRole } from "./lib/superadmin";

type ExternalView = "welcome" | "login" | "parent";

function Root() {
  const { user, currentBranchId, signOut } = useApp();
  const [route, setRoute] = useState("dashboard");
  const [external, setExternal] = useState<ExternalView>("welcome");
  const [loginMode, setLoginMode] = useState<"in" | "up">("in");

  // ===== Super Admin State =====
  const [isSuperAdmin, setIsSuperAdmin] = useState(false);
  const [checkingAdmin, setCheckingAdmin] = useState(false);

  // reset to dashboard whenever the signed-in user or branch changes
  useEffect(() => {
    setRoute("dashboard");
    if (user) setExternal("welcome");
    setIsSuperAdmin(false);
  }, [user?.uid, currentBranchId]);

  // ===== SUPER ADMIN DETECTION (NO OTP) =====
  // Checks admins/super_admin in Firestore.
  // If email matches + active=true → Super Admin dashboard directly.
  // All other users → normal center management flow.
  useEffect(() => {
    if (!user) {
      setIsSuperAdmin(false);
      setCheckingAdmin(false);
      return;
    }

    setCheckingAdmin(true);
    checkSuperAdminRole(user.email).then((result) => {
      setIsSuperAdmin(result);
      setCheckingAdmin(false);
    });
  }, [user]);

  // Listen for navigation events (e.g., Upgrade button in Settings)
  // MUST be at the top level with other hooks, before any early returns.
  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      if (detail === "upgrade") setRoute("upgrade");
    };
    window.addEventListener("navigate", handler);
    return () => window.removeEventListener("navigate", handler);
  }, []);

  // ---- external (pre sign-in) views ----
  if (!user) {
    if (external === "parent") {
      return (
        <div className="min-h-screen bg-bg">
          <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
            <ParentPortal external onClose={() => setExternal("welcome")} />
          </div>
        </div>
      );
    }
    if (external === "login") {
      return (
        <LoginPage
          onClose={() => setExternal("welcome")}
          onParentPortal={() => setExternal("parent")}
          defaultMode={loginMode}
        />
      );
    }
    return (
      <Welcome
        onSignIn={(mode) => { setLoginMode(mode); setExternal("login"); }}
        onParentPortal={() => setExternal("parent")}
      />
    );
  }

  // ===== LOADING while checking admin status =====
  if (checkingAdmin) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-2 border-brand-500/30 border-t-brand-500" />
          <p className="text-sm text-muted">جارٍ التحقق...</p>
        </div>
      </div>
    );
  }

  // ===== SUPER ADMIN DASHBOARD =====
  // Opens directly — no OTP, no gate.
  if (isSuperAdmin) {
    return (
      <SuperAdminDashboard
        adminUid={user.uid}
        adminEmail={user.email}
        onSignOut={() => { signOut(); setIsSuperAdmin(false); }}
      />
    );
  }

  // Parents only ever reach the read-only portal
  if (user.role === "PARENT") return <ParentPortal />;

  // ===== NORMAL USER FLOW =====
  // ===== FEATURE GATING =====
  const { canUseFeature } = useApp();

  const page = (() => {
    // Gate locked features — redirect to upgrade if plan doesn't allow
    const gatedRoutes: Record<string, string> = {
      ai: "ai_assistant",
      staff: "staff_management",
      classes: "classes",
    };
    const gatedKey = gatedRoutes[route];
    if (gatedKey && !canUseFeature(gatedKey)) {
      return <Upgrade onClose={() => setRoute("dashboard")} />;
    }

    switch (route) {
      case "dashboard": return <Dashboard onNavigate={setRoute} />;
      case "students": return <Students />;
      case "teachers": return <Teachers />;
      case "classes": return <Classes />;
      case "schedule": return <Schedule />;
      case "attendance": return <Attendance />;
      case "finance": return <Finance />;
      case "exams": return <Exams />;
      case "reports": return <Reports />;
      case "staff": return <Staff />;
      case "branches": return <Branches />;
      case "messages": return <Messages />;
      case "parent": return <ParentPortal />;
      case "ai": return <AIAssistant />;
      case "settings": return <Settings />;
      case "upgrade": return <Upgrade onClose={() => setRoute("settings")} />;
      default: return <Dashboard onNavigate={setRoute} />;
    }
  })();

  return (
    <Layout current={route} onNavigate={setRoute}>
      {page}
    </Layout>
  );
}

export default function App() {
  return (
    <AppProvider>
      <Root />
      <Toaster />
    </AppProvider>
  );
}
