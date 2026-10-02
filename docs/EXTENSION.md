# Browser extension (MV3)

## Auth

Use a **Personal access token** from the web settings page. Stored in `chrome.storage.local` only — never in page context.

## Permissions

- `contextMenus`, `storage`, `activeTab`
- Host permission for your API origin only in production builds

## Limitations

- Cannot read native Messenger/Instagram apps
- Web DOM selectors may break when Meta updates UI; selection-based capture is primary

## Security

- Do not log capture body in extension console in production builds
- CSP: no inline scripts in popup
