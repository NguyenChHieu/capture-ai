# Save to Capture Organizer (build in Shortcuts app)

1. Receive **Text** or **Images** from Share Sheet
2. Get contents of **Shortcut Input**
3. If input is text:
   - Get value for **API_URL** (Text) — default `http://localhost:8080`
   - Get value for **PAT** (Text) — your `co_…` token from web Settings
   - Get contents of URL `{API_URL}/v1/captures`
   - Method POST, headers `Authorization: Bearer {PAT}`, `Content-Type: application/json`
   - Request body JSON: `{"source":"ios_shortcut","rawText":"<text>","clientCaptureId":"<UUID>"}`
4. Show notification "Saved"

See docs/MOBILE-IOS.md for Messenger/Instagram copy flows.
