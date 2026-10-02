import { createCaptureSchema } from "@capture-organizer/shared/src/schemas";

const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export function getToken(): string | null {
  return localStorage.getItem("token");
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem("token", token);
  else localStorage.removeItem("token");
}

async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init.headers as Record<string, string>),
  };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, { ...init, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export type CaptureStatus = "QUEUED" | "PROCESSING" | "READY" | "FAILED" | "NO_ITEMS";
export type ItemStatus = "PENDING_REVIEW" | "FILED" | "ARCHIVED";

export interface Capture {
  id: string;
  source: string;
  rawText?: string;
  status: CaptureStatus;
  errorMessage?: string;
  createdAt: string;
  items: { id: string; title: string; status: ItemStatus }[];
}

export interface Item {
  id: string;
  captureId: string;
  title: string;
  body: string;
  status: ItemStatus;
  confidence?: number;
  categoryIds: string[];
  proposedCategories: string[];
}

export interface Category {
  id: string;
  name: string;
  icon?: string;
  sortOrder: number;
}

export const auth = {
  register: (email: string, password: string) =>
    api<{ token: string }>("/v1/auth/register", { method: "POST", body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    api<{ token: string }>("/v1/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
};

export const captures = {
  list: () => api<Capture[]>("/v1/captures"),
  create: (body: {
    source: string;
    rawText?: string;
    sourceUrl?: string;
    conversationTitle?: string;
    clientCaptureId?: string;
  }) => {
    const payload = createCaptureSchema.parse(body);
    return api<Capture>("/v1/captures", { method: "POST", body: JSON.stringify(payload) });
  },
  status: (id: string) =>
    api<{ id: string; status: CaptureStatus; errorMessage?: string; updatedAt: string }>(`/v1/captures/${id}/status`),
};

export const items = {
  list: (status?: ItemStatus) => api<Item[]>(`/v1/items${status ? `?status=${status}` : ""}`),
  search: (q: string) => api<Item[]>(`/v1/items/search?q=${encodeURIComponent(q)}`),
  accept: (id: string) => api<Item>(`/v1/items/${id}/accept`, { method: "POST" }),
  assignCategory: (id: string, categoryId: string) =>
    api<Item>(`/v1/items/${id}/categories`, { method: "POST", body: JSON.stringify({ categoryId }) }),
};

export const categories = {
  list: () => api<Category[]>("/v1/categories"),
  create: (name: string) => api<Category>("/v1/categories", { method: "POST", body: JSON.stringify({ name }) }),
};

export const me = {
  createPat: (name: string) =>
    api<{ id: string; token: string; prefix: string; name: string }>("/v1/me/tokens", {
      method: "POST",
      body: JSON.stringify({ name }),
    }),
  listPats: () => api<{ id: string; name: string; prefix: string; createdAt: string }[]>("/v1/me/tokens"),
  exportAll: () => api<unknown>("/v1/me/export"),
  deleteAccount: () => api<void>("/v1/me", { method: "DELETE" }),
  quota: () => api<{ dailyTokenBudget: number; tokensUsedToday: number; maxCaptureChars: number }>("/v1/me/quota"),
};

export async function pollCaptureUntilDone(id: string, onTick?: (status: CaptureStatus) => void): Promise<CaptureStatus> {
  const terminal: CaptureStatus[] = ["READY", "FAILED", "NO_ITEMS"];
  for (let i = 0; i < 60; i++) {
    const s = await captures.status(id);
    onTick?.(s.status);
    if (terminal.includes(s.status)) return s.status;
    await new Promise((r) => setTimeout(r, 2000));
  }
  return "PROCESSING";
}
