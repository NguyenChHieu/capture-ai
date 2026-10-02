# iPhone capture (Shortcut + web)

## Copy text from Messenger / Instagram

1. Long-press message → **Copy**
2. Open the web app → **New capture** → Paste, or run the **Save to Capture Organizer** Shortcut

## Screenshot

1. Take screenshot → Photos → **Share** → Shortcut (after setup)

## Shortcut setup

1. Web app → **Settings** → copy **Personal access token**
2. Install `docs/shortcuts/Save-to-Capture-Organizer.shortcut` (create in Shortcuts app using template below)
3. Set API base URL to your deployment (e.g. `https://api.example.com`)

### Shortcut actions (manual build)

1. Receive **text** or **images** from Share Sheet
2. If text: `POST {API}/v1/captures` with header `Authorization: Bearer {PAT}` and JSON body
3. If image: multipart upload to same endpoint with `source=ios_shortcut`

## Phase B

Native **Share Extension** (SwiftUI) — see `docs/IOS_SHARE_EXTENSION_PHASE_B.md`.
