import { useState } from "react";
import { motion } from "framer-motion";
import {
  Check, Crown, Sparkles, MessageCircle, ArrowLeft, Shield,
  CheckCircle2, Copy, CalendarClock,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import { Card, Badge } from "../components/ui";
import { PLAN_DEFINITIONS, PAYMENT_DETAILS } from "../lib/superadmin";
import { cn } from "../utils/cn";

export function Upgrade({ onClose }: { onClose: () => void }) {
  const { lang, subscriptionPlan, subscriptionEndDate } = useApp();
  const isAr = lang === "ar";

  const planOrder: Record<string, number> = { free: 0, pro: 1, enterprise: 2 };
  const currentLevel = planOrder[subscriptionPlan] ?? 0;
  const availablePlans = PLAN_DEFINITIONS.filter((p) => (planOrder[p.id] ?? 0) > currentLevel);
  const isEnterprise = subscriptionPlan === "enterprise";

  const [selectedPlan, setSelectedPlan] = useState<string | null>(
    availablePlans.length > 0 ? availablePlans[0].id : null
  );
  const selectedPlanDef = selectedPlan
    ? PLAN_DEFINITIONS.find((p) => p.id === selectedPlan)
    : null;

  const copyNumber = () => {
    navigator.clipboard?.writeText(PAYMENT_DETAILS.paymentNumber);
  };

  const openWhatsApp = (planName: string) => {
    const msg = isAr
      ? `السلام عليكم، أريد ترقية الاشتراك إلى الخطة ${planName}`
      : `Hello, I want to upgrade to the ${planName} plan`;
    const url = `https://api.whatsapp.com/send?phone=${PAYMENT_DETAILS.whatsappNumber}&text=${encodeURIComponent(msg)}`;
    window.open(url, "_blank");
  };

  return (
    <div className="min-h-screen bg-bg">
      {/* Header */}
      <header className="sticky top-0 z-30 border-b border-line bg-surface/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-5xl items-center gap-3 px-4">
          <button onClick={onClose} className="inline-flex items-center gap-1 text-sm font-medium text-muted hover:text-ink">
            <ArrowLeft className="h-4 w-4 rtl:rotate-180" />
            {isAr ? "رجوع" : "Back"}
          </button>
          <div className="ms-auto flex items-center gap-2">
            <Crown className="h-5 w-5 text-amber-500" />
            <h1 className="text-sm font-bold text-ink">{isAr ? "الاشتراك والخطة" : "Subscription & Plan"}</h1>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
        {/* Hero */}
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="mb-10 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg">
            <Sparkles className="h-8 w-8" />
          </div>
          <h2 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">
            {isAr ? "اكتشف المزيد من المميزات القوية" : "Discover More Powerful Features"}
          </h2>
          <p className="mt-2 text-sm text-muted">{isAr ? "اختر الخطة المناسبة لسنترك" : "Choose the right plan for your center"}</p>
        </motion.div>

        {/* Plans */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          {availablePlans.length === 0 ? (
            <div className="col-span-full space-y-4">
              {/* Enterprise banner */}
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-8 text-center dark:border-emerald-500/20 dark:bg-emerald-500/5">
                <Crown className="mx-auto mb-3 h-12 w-12 text-emerald-500" />
                <p className="text-lg font-bold text-emerald-700 dark:text-emerald-300">{isAr ? "أنت مشترك في أعلى خطة متاحة" : "You're on the highest plan"}</p>
                <p className="mt-1 text-sm text-muted">{isAr ? "استمتع بكل المميزات بدون حدود" : "Enjoy all features without limits"}</p>
              </div>
              {/* Expiry date */}
              {subscriptionEndDate && (
                <Card className="flex items-center gap-3 p-4">
                  <CalendarClock className="h-5 w-5 shrink-0 text-brand-500" />
                  <div>
                    <p className="text-sm font-semibold text-ink">{isAr ? "موعد انتهاء الاشتراك" : "Subscription Expiry"}</p>
                    <p className="text-xs text-muted">{new Date(subscriptionEndDate).toLocaleDateString(isAr ? "ar-EG" : "en-US", { year: "numeric", month: "long", day: "numeric" })}</p>
                  </div>
                </Card>
              )}
            </div>
          ) : (
            <>
              {PLAN_DEFINITIONS.filter((p) => p.id !== "free").map((plan, i) => {
                const isSelected = selectedPlan === plan.id;
                return (
                  <motion.div key={plan.id} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.1 }}
                    onClick={() => setSelectedPlan(plan.id)}
                    className={cn("relative cursor-pointer rounded-2xl border-2 bg-surface p-6 shadow-sm transition-all",
                      isSelected
                        ? "border-brand-400 shadow-lg shadow-brand-500/10 ring-1 ring-brand-400/30"
                        : "border-line hover:border-brand-200 hover:shadow-md"
                    )}
                  >
                    {plan.id === "pro" && (
                      <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                        <Badge tone="brand" className="px-3 py-1 text-[11px]">{isAr ? "الأكثر شيوعاً" : "Most Popular"}</Badge>
                      </div>
                    )}
                    <div className="text-center">
                      <span className={cn("inline-block rounded-full px-3 py-1 text-xs font-bold", plan.color)}>{plan.name}</span>
                      <div className="mt-4 flex items-baseline justify-center gap-1">
                        <span className="text-4xl font-extrabold text-ink">{plan.price}</span>
                        <span className="text-sm text-muted">{isAr ? "ج.م" : "EGP"}</span>
                      </div>
                      <p className="mt-1 text-xs text-muted">{isAr ? "شهرياً" : "/month"}</p>
                    </div>
                    <ul className="mt-6 space-y-2.5">
                      {plan.features.map((f, j) => (
                        <li key={j} className="flex items-start gap-2 text-xs text-ink">
                          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
                          <span>{f}</span>
                        </li>
                      ))}
                    </ul>
                    <button onClick={(e) => { e.stopPropagation(); openWhatsApp(plan.name); }}
                      className={cn("mt-6 flex w-full items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-bold transition",
                        plan.id === "pro"
                          ? "bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg hover:brightness-110"
                          : "border border-line bg-surface text-ink hover:bg-elevated"
                      )}
                    >
                      {plan.id === "pro" && <Crown className="h-4 w-4" />}
                      {isAr ? "اشترك الآن" : "Subscribe Now"}
                    </button>
                  </motion.div>
                );
              })}
            </>
          )}
        </div>

        {/* Payment Details — hide for enterprise, show dynamic price for others */}
        {!isEnterprise && selectedPlanDef && selectedPlanDef.price > 0 && (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="mt-10">
            <Card className="overflow-hidden">
              <div className="mesh-brand relative px-6 py-8 text-center text-white">
                <div className="relative">
                  <h3 className="text-xl font-bold">{isAr ? "تفاصيل الدفع" : "Payment Details"}</h3>
                  <p className="mt-1 text-sm text-white/80">{isAr ? "حوّل المبلغ ثم أرسل إثبات التحويل" : "Transfer then send proof"}</p>
                </div>
              </div>
              <div className="p-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  {/* Amount — dynamic based on selected plan */}
                  <div className="rounded-xl border border-line p-4 text-center">
                    <p className="text-xs text-muted">{isAr ? "المبلغ المطلوب" : "Amount Due"}</p>
                    <p className="mt-1 text-3xl font-extrabold text-brand-600">{selectedPlanDef.price} <span className="text-base">{isAr ? "ج.م" : "EGP"}</span></p>
                    <p className="mt-1 text-[11px] text-faint">{isAr ? `شهرياً للخطة ${selectedPlanDef.name}` : `Monthly for ${selectedPlanDef.name} plan`}</p>
                  </div>
                  {/* Payment Number */}
                  <div className="rounded-xl border border-line p-4 text-center">
                    <p className="text-xs text-muted">{isAr ? "رقم التحويل" : "Transfer To"}</p>
                    <div className="mt-1 flex items-center justify-center gap-2">
                      <p className="text-2xl font-extrabold font-mono text-ink" dir="ltr">01140617424</p>
                      <button onClick={copyNumber} className="rounded-lg p-1.5 text-muted hover:bg-elevated hover:text-ink">
                        <Copy className="h-4 w-4" />
                      </button>
                    </div>
                    <p className="mt-1 text-[11px] text-faint">{isAr ? "إنستا باي / محفظة كاش" : "InstaPay / Cash Wallet"}</p>
                  </div>
                </div>

                {/* Methods */}
                <div className="mt-4 flex flex-wrap justify-center gap-2">
                  {PAYMENT_DETAILS.paymentMethods.map((m, i) => (
                    <span key={i} className="inline-flex items-center gap-1 rounded-full border border-line bg-elevated/60 px-3 py-1 text-[11px] font-medium text-muted">
                      <Check className="h-3 w-3 text-emerald-500" />
                      {m}
                    </span>
                  ))}
                </div>

                {/* Note */}
                <div className="mt-4 flex items-center justify-center gap-2 rounded-lg bg-amber-50 px-4 py-2.5 text-xs text-amber-700 dark:bg-amber-500/10 dark:text-amber-300">
                  <Shield className="h-4 w-4 shrink-0" />
                  {isAr ? "أرسل سكرين شوت التحويل على واتساب لتفعيل الاشتراك" : "Send transfer screenshot on WhatsApp to activate"}
                </div>

                {/* WhatsApp Button */}
                <button onClick={() => openWhatsApp(selectedPlanDef.name)}
                  className="mt-5 flex w-full items-center justify-center gap-3 rounded-xl bg-[#25D366] px-6 py-4 text-base font-bold text-white shadow-lg shadow-emerald-600/25 transition hover:-translate-y-0.5 hover:brightness-110 active:scale-95"
                >
                  <MessageCircle className="h-6 w-6" />
                  {isAr ? "إرسال إثبات التحويل" : "Send Payment Proof"}
                </button>

                <p className="mt-4 text-center text-xs text-muted">{isAr ? "يمكنك التحدث معنا وتجربة النظام" : "Talk to us and try the system"}</p>
              </div>
            </Card>
          </motion.div>
        )}
      </div>
    </div>
  );
}
