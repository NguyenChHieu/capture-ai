import { useEffect, useState } from "react";
import {
  auth,
  captures,
  categories,
  items,
  me,
  pollCaptureUntilDone,
  setToken,
  getToken,
  type Capture,
  type Category,
  type Item,
} from "./api";
import "./App.css";

type View = "captures" | "review" | "categories" | "search" | "settings";

export default function App() {
  const [token, setTok] = useState(getToken());
  const [view, setView] = useState<View>("captures");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  if (!token) {
    return (
      <div className="auth">
        <h1>Capture Organizer</h1>
        <p>Save scroll content → AI organizes into categories.</p>
        {error && <p className="error">{error}</p>}
        <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button
          onClick={async () => {
            try {
              const { token: t } = await auth.login(email, password);
              setToken(t);
              setTok(t);
            } catch (e) {
              setError(String(e));
            }
          }}
        >
          Log in
        </button>
        <button
          className="secondary"
          onClick={async () => {
            try {
              const { token: t } = await auth.register(email, password);
              setToken(t);
              setTok(t);
            } catch (e) {
              setError(String(e));
            }
          }}
        >
          Register
        </button>
      </div>
    );
  }

  return (
    <div className="layout">
      <nav>
        <strong>Capture Organizer</strong>
        <button className={view === "captures" ? "active" : ""} onClick={() => setView("captures")}>
          Captures
        </button>
        <button className={view === "review" ? "active" : ""} onClick={() => setView("review")}>
          Review
        </button>
        <button className={view === "categories" ? "active" : ""} onClick={() => setView("categories")}>
          Categories
        </button>
        <button className={view === "search" ? "active" : ""} onClick={() => setView("search")}>
          Search
        </button>
        <button className={view === "settings" ? "active" : ""} onClick={() => setView("settings")}>
          Settings
        </button>
        <button
          className="secondary"
          onClick={() => {
            setToken(null);
            setTok(null);
          }}
        >
          Log out
        </button>
      </nav>
      <main>
        {view === "captures" && <CapturesView />}
        {view === "review" && <ReviewView />}
        {view === "categories" && <CategoriesView />}
        {view === "search" && <SearchView />}
        {view === "settings" && <SettingsView />}
      </main>
    </div>
  );
}

function CapturesView() {
  const [list, setList] = useState<Capture[]>([]);
  const [text, setText] = useState("");
  const [processing, setProcessing] = useState<string | null>(null);
  const [status, setStatus] = useState<string>("");

  const refresh = () => captures.list().then(setList);
  useEffect(() => {
    refresh();
  }, []);

  return (
    <div>
      <h2>New capture</h2>
      <textarea rows={6} placeholder="Paste chat text, notes, or tips…" value={text} onChange={(e) => setText(e.target.value)} />
      <button
        disabled={!text.trim() || !!processing}
        onClick={async () => {
          const cap = await captures.create({ source: "web_paste", rawText: text, clientCaptureId: crypto.randomUUID() });
          setText("");
          setProcessing(cap.id);
          setStatus(cap.status);
          await pollCaptureUntilDone(cap.id, setStatus);
          setProcessing(null);
          refresh();
        }}
      >
        Save & organize
      </button>
      {processing && <p className="muted">Processing: {status}</p>}
      <h2>Recent</h2>
      <ul className="list">
        {list.map((c) => (
          <li key={c.id}>
            <span className={`pill ${c.status}`}>{c.status}</span> {c.source} — {(c.rawText ?? "").slice(0, 80)}
            {c.items.length > 0 && <small> ({c.items.length} items)</small>}
          </li>
        ))}
      </ul>
    </div>
  );
}

function ReviewView() {
  const [reviewItems, setReviewItems] = useState<Item[]>([]);
  const [cats, setCats] = useState<Category[]>([]);

  const refresh = async () => {
    setReviewItems(await items.list("PENDING_REVIEW"));
    setCats(await categories.list());
  };
  useEffect(() => {
    refresh();
  }, []);

  return (
    <div>
      <h2>Review inbox</h2>
      {reviewItems.length === 0 && <p className="muted">Nothing pending — auto-filed or empty captures.</p>}
      {reviewItems.map((item) => (
        <div key={item.id} className="card">
          <h3>{item.title}</h3>
          <p>{item.body}</p>
          {item.proposedCategories.length > 0 && (
            <p className="muted">Proposed: {item.proposedCategories.join(", ")}</p>
          )}
          <div className="row">
            <button onClick={() => items.accept(item.id).then(refresh)}>Accept</button>
            {cats.slice(0, 5).map((c) => (
              <button key={c.id} className="secondary" onClick={() => items.assignCategory(item.id, c.id).then(refresh)}>
                {c.name}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function CategoriesView() {
  const [cats, setCats] = useState<Category[]>([]);
  const [name, setName] = useState("");
  useEffect(() => {
    categories.list().then(setCats);
  }, []);
  return (
    <div>
      <h2>Categories</h2>
      <ul className="list">
        {cats.map((c) => (
          <li key={c.id}>{c.name}</li>
        ))}
      </ul>
      <input value={name} onChange={(e) => setName(e.target.value)} placeholder="New category" />
      <button onClick={() => categories.create(name).then(() => categories.list().then(setCats))}>Add</button>
    </div>
  );
}

function SearchView() {
  const [q, setQ] = useState("");
  const [results, setResults] = useState<Item[]>([]);
  return (
    <div>
      <h2>Search</h2>
      <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search items…" />
      <button onClick={() => items.search(q).then(setResults)}>Search</button>
      <ul className="list">
        {results.map((i) => (
          <li key={i.id}>
            <strong>{i.title}</strong>
            <p>{i.body.slice(0, 120)}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}

function SettingsView() {
  const [pat, setPat] = useState<string | null>(null);
  const [quota, setQuota] = useState<string>("");
  useEffect(() => {
    me.quota().then((q) => setQuota(`${q.tokensUsedToday}/${q.dailyTokenBudget} tokens today`));
  }, []);
  return (
    <div>
      <h2>Settings</h2>
      <p className="muted">AI quota: {quota}</p>
      <p className="muted">Personal access token for extension / iOS Shortcut (shown once).</p>
      <button
        onClick={async () => {
          const res = await me.createPat("default");
          setPat(res.token);
        }}
      >
        Generate token
      </button>
      {pat && (
        <code className="token">{pat}</code>
      )}
      <h3>Privacy</h3>
      <p className="muted">See docs/PRIVACY.md — LLM subprocessors when AI enabled.</p>
      <button
        className="danger"
        onClick={async () => {
          if (confirm("Delete account and all data?")) {
            await me.deleteAccount();
            setToken(null);
            location.reload();
          }
        }}
      >
        Delete account
      </button>
      <button className="secondary" onClick={() => me.exportAll().then((d) => downloadJson(d, "export.json"))}>
        Export JSON
      </button>
    </div>
  );
}

function downloadJson(data: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
}
