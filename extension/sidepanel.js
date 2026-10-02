const DEFAULT_API = "http://localhost:8080";

document.getElementById("saveSelection").addEventListener("click", async () => {
  const status = document.getElementById("status");
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) return;
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    func: () => window.getSelection()?.toString() ?? "",
  });
  if (!result?.trim()) {
    status.textContent = "No text selected on this tab.";
    return;
  }
  const { apiUrl, pat } = await chrome.storage.local.get(["apiUrl", "pat"]);
  if (!pat) {
    status.textContent = "Set PAT in extension popup first.";
    return;
  }
  const base = apiUrl || DEFAULT_API;
  const res = await fetch(`${base}/v1/captures`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${pat}` },
    body: JSON.stringify({
      source: "browser_extension_sidepanel",
      rawText: result,
      sourceUrl: tab.url,
      conversationTitle: tab.title,
      clientCaptureId: crypto.randomUUID(),
    }),
  });
  status.textContent = res.ok ? "Saved — processing on server." : `Error ${res.status}`;
});
