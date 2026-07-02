import { useState, useRef, useEffect } from "react";
import { Sparkles, Bot, KeyRound, Loader2, Eye, EyeOff, Wand2, FileText, Send, MessageSquare, Cpu } from "lucide-react";
import { useApp } from "../context/AppContext";
import {
  PageHeader,
  Button,
  Card,
  Select,
  Input,
  Field,
  Badge,
  Tabs,
} from "../components/ui";
import { generateStudentPdf } from "../lib/pdf";
import { buildPrompt, generateInsight, processLocalCommand, type AiResult } from "../lib/ai";
import { cn } from "../utils/cn";

const KEY_STORE = "cpd_gemini_key";

function RichText({ text }: { text: string }) {
  return (
    <div className="space-y-1.5 text-sm leading-relaxed text-ink">
      {text.split("\n").map((line, i) => {
        if (!line.trim()) return <div key={i} className="h-1.5" />;
        const parts = line.split(/(\*\*[^*]+\*\*)/g);
        const rendered = parts.map((p, j) =>
          p.startsWith("**") && p.endsWith("**") ? (
            <strong key={j} className="font-semibold text-ink">{p.slice(2, -2)}</strong>
          ) : (
            <span key={j}>{p}</span>
          ),
        );
        const isHeading = /^\*\*[^*]+\*\*:?$/.test(line.trim());
        return (
          <p key={i} className={cn(isHeading && "mt-2 font-semibold")}>
            {rendered}
          </p>
        );
      })}
    </div>
  );
}

interface ChatMessage {
  role: "user" | "assistant";
  text: string;
  action?: { type: "upsert" | "remove"; coll: string; item?: any; id?: string };
}

export function AIAssistant() {
  const { db, t, lang, upsert, remove } = useApp();
  const isAr = lang === "ar";
  const [tab, setTab] = useState("gemini");
  const [studentId, setStudentId] = useState(db.students[0]?.id ?? "");
  const [apiKey, setApiKey] = useState(() => localStorage.getItem(KEY_STORE) ?? "");
  const [showKey, setShowKey] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AiResult | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);

  // Local AI chat
  const [chat, setChat] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState("");
  const chatEnd = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEnd.current?.scrollIntoView({ behavior: "smooth" });
  }, [chat]);

  const student = db.students.find((s) => s.id === studentId);

  const run = async () => {
    if (!student) return;
    setLoading(true);
    setResult(null);
    localStorage.setItem(KEY_STORE, apiKey);
    const res = await generateInsight(db, student, apiKey);
    setResult(res);
    setLoading(false);
  };

  const sendChat = () => {
    const text = chatInput.trim();
    if (!text) return;
    const userMsg: ChatMessage = { role: "user", text };
    const result = processLocalCommand(text, db, lang);
    const assistantMsg: ChatMessage = { role: "assistant", text: result.text, action: result.action ?? undefined };
    setChat((prev) => [...prev, userMsg, assistantMsg]);
    setChatInput("");
  };

  const executeAction = (action: { type: "upsert" | "remove"; coll: string; item?: any; id?: string }) => {
    if (action.type === "upsert" && action.item) {
      upsert(action.coll as any, action.item);
    } else if (action.type === "remove" && action.id) {
      remove(action.coll as any, action.id);
    }
    setChat((prev) => prev.map((m, i) =>
      i === prev.length - 1 ? { ...m, action: undefined, text: m.text + (isAr ? "\n\n✅ تم التنفيذ بنجاح!" : "\n\n✅ Done!") } : m
    ));
  };

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("ai.title")} subtitle={t("ai.subtitle")} />
      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { id: "gemini", label: isAr ? "Gemini (سحابي)" : "Gemini (Cloud)", icon: <Sparkles className="h-4 w-4" /> },
          { id: "local", label: isAr ? "المساعد المحلي" : "Local Assistant", icon: <MessageSquare className="h-4 w-4" /> },
        ]}
      />
      {tab === "gemini" && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          {/* config */}
          <Card className="space-y-4 p-5">
            <div className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-brand-500 to-violet-600 text-white">
                <Sparkles className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-semibold text-ink">Gemini</p>
                <p className="text-[11px] text-muted">1.5-flash</p>
              </div>
            </div>

            <Field label={t("ai.select")}>
              <Select value={studentId} onChange={(e) => { setStudentId(e.target.value); setResult(null); }}>
                {db.students.map((s) => <option key={s.id} value={s.id}>{s.name} — {s.id}</option>)}
              </Select>
            </Field>

            <Field label={t("ai.apiKey")} hint={t("ai.apiKeyHint")}>
              <div className="relative">
                <KeyRound className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
                <Input
                  type={showKey ? "text" : "password"}
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  placeholder="AIza…"
                  className="ps-9 pe-9 font-mono text-xs"
                />
                <button onClick={() => setShowKey(!showKey)} className="absolute inset-y-0 end-2 my-auto text-faint hover:text-ink">
                  {showKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </Field>

            <Button onClick={run} disabled={loading || !student} className="w-full">
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Wand2 className="h-4 w-4" />}
              {loading ? t("ai.analyzing") : result ? t("ai.regenerate") : t("ai.prompt")}
            </Button>

            {student && (
              <Button variant="secondary" className="w-full" onClick={() => generateStudentPdf(db, student)}>
                <FileText className="h-4 w-4" />
                {t("parent.exportReport")}
              </Button>
            )}
          </Card>

          {/* output */}
          <div className="space-y-4 lg:col-span-2">
            {!result && !loading && (
              <Card className="flex flex-col items-center justify-center gap-3 p-12 text-center">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-elevated text-faint">
                  <Bot className="h-7 w-7" />
                </div>
                <p className="max-w-sm text-sm text-muted">{t("ai.placeholder")}</p>
              </Card>
            )}

            {loading && (
              <Card className="flex flex-col items-center justify-center gap-3 p-12">
                <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
                <p className="text-sm text-muted">{t("ai.analyzing")}</p>
              </Card>
            )}

            {result && (
              <Card className="p-5">
                <div className="mb-3 flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-ink">{student?.name}</h3>
                  {result.usedApi ? (
                    <Badge tone="success">Gemini</Badge>
                  ) : (
                    <Badge tone="warning">{t("ai.mockWarn")}</Badge>
                  )}
                </div>
                <RichText text={result.text} />
              </Card>
            )}

            {student && (
              <Card className="overflow-hidden">
                <button
                  onClick={() => setShowPrompt(!showPrompt)}
                  className="flex w-full items-center justify-between px-5 py-3 text-start hover:bg-elevated/50"
                >
                  <span className="text-sm font-medium text-ink">{t("ai.builtPrompt")}</span>
                  {showPrompt ? <EyeOff className="h-4 w-4 text-faint" /> : <Eye className="h-4 w-4 text-faint" />}
                </button>
                {showPrompt && (
                  <pre className="max-h-72 overflow-auto whitespace-pre-wrap border-t border-line bg-elevated/40 p-4 text-[11px] leading-relaxed text-muted">
                    {buildPrompt(db, student)}
                  </pre>
                )}
              </Card>
            )}
          </div>
        </div>
      )}

      {tab === "local" && (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          {/* info panel */}
          <Card className="space-y-3 p-5">
            <div className="flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 text-white">
                <Cpu className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-semibold text-ink">{isAr ? "مساعد محلي" : "Local AI"}</p>
                <p className="text-[11px] text-muted">{isAr ? "يعمل بدون إنترنت" : "Works offline"}</p>
              </div>
            </div>
            <div className="rounded-xl bg-elevated/50 p-3 text-xs text-muted leading-relaxed">
              {isAr
                ? "المساعد المحلي يمكنه إدارة بيانات السنتر عبر المحادثة. جرب:\n\n• أضف طالب اسمه \"أحمد\"\n• احذف الطالب \"أحمد\"\n• تقرير عن الطالب \"أحمد\"\n• قائمة الطلاب\n• إحصائيات السنتر"
                : "The local assistant can manage center data through chat. Try:\n\n• Add student named \"Ahmed\"\n• Delete student \"Ahmed\"\n• Report for student \"Ahmed\"\n• List students\n• Center statistics"}
            </div>
          </Card>

          {/* chat panel */}
          <Card className="flex flex-col overflow-hidden lg:col-span-2">
            <div className="flex h-[450px] flex-col">
              <div className="flex-1 space-y-3 overflow-y-auto p-4">
                {chat.length === 0 && (
                  <div className="flex flex-col items-center justify-center gap-2 py-12 text-center">
                    <Bot className="h-10 w-10 text-faint" />
                    <p className="text-sm text-muted">{isAr ? "اكتب أمراً للبدء" : "Type a command to start"}</p>
                  </div>
                )}
                {chat.map((msg, i) => (
                  <div key={i} className={cn("flex", msg.role === "user" ? "justify-end" : "justify-start")}>
                    <div className={cn("max-w-[85%] rounded-2xl px-4 py-2.5 text-sm",
                      msg.role === "user"
                        ? "rounded-br-md bg-brand-600 text-white"
                        : "rounded-bl-md border border-line bg-surface text-ink"
                    )}>
                      <RichText text={msg.text} />
                      {msg.action && (
                        <button onClick={() => executeAction(msg.action!)}
                          className="mt-2 inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-[11px] font-bold text-white transition hover:bg-emerald-700"
                        >
                          {isAr ? "تأكيد" : "Confirm"}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                <div ref={chatEnd} />
              </div>
              <div className="flex items-center gap-2 border-t border-line p-3">
                <input value={chatInput} onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && sendChat()}
                  placeholder={isAr ? "اكتب أمراً..." : "Type a command..."}
                  className="h-10 flex-1 rounded-xl border border-line bg-surface px-3 text-sm text-ink placeholder:text-faint focus:border-brand-400 focus:outline-none"
                />
                <button onClick={sendChat} disabled={!chatInput.trim()}
                  className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 text-white transition hover:bg-brand-700 disabled:opacity-50">
                  <Send className="h-4 w-4" />
                </button>
              </div>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
