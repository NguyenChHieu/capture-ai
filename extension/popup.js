document.getElementById("save").addEventListener("click", async () => {
  const apiUrl = document.getElementById("apiUrl").value.trim();
  const pat = document.getElementById("pat").value.trim();
  await chrome.storage.local.set({ apiUrl, pat });
  document.getElementById("msg").textContent = "Saved (token not logged).";
});

chrome.storage.local.get(["apiUrl", "pat"], ({ apiUrl, pat }) => {
  if (apiUrl) document.getElementById("apiUrl").value = apiUrl;
  if (pat) document.getElementById("pat").value = pat;
});
