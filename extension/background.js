const DEFAULT_API = "http://localhost:8080";

chrome.runtime.onInstalled.addListener(() => {
  chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
  chrome.contextMenus.create({
    id: "save-selection",
    title: "Save selection to Capture Organizer",
    contexts: ["selection"],
  });
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== "save-selection" || !info.selectionText) return;
  const { apiUrl, pat } = await chrome.storage.local.get(["apiUrl", "pat"]);
  const base = apiUrl || DEFAULT_API;
  const token = pat;
  if (!token) {
    console.warn("Capture Organizer: set PAT in extension popup");
    return;
  }
  await fetch(`${base}/v1/captures`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      source: "browser_extension",
      rawText: info.selectionText,
      sourceUrl: tab?.url,
      conversationTitle: tab?.title,
      clientCaptureId: crypto.randomUUID(),
    }),
  });
});
