# DmsExplorer – Windows Explorer integration

Brings the document system into Windows Explorer: select a PDF and the **preview pane** shows the document together with actions – **fast reader**, **notes** and **open in DMS**. The same actions are available in the **context menu**.

> Prototype – see the roadmap at the end for what is still missing.

## Features

| | |
|---|---|
| 🖼️ **Preview pane** | PDF pages rendered with the PDF engine built into Windows, action bar on top, first line of your note shown as a yellow strip |
| ⚡ **Fast reader** | RSVP speed reading directly on the local PDF: highlighted focus letter, 100–1000 wpm, pauses at punctuation, position remembered per document |
| 📝 **Notes** | one note per document with autosave; it follows the file when it is renamed or moved (keyed by the SHA-256 of the content). *Share to DMS* turns it into a comment on the DMS document that stays in sync |
| ↗ **Open in DMS** | finds the file in the DMS by its content hash and opens its page; if it is not there yet, offers to upload it |
| 🖱️ **Context menu** | *Document System → Fast reader / Notes / Open preview window / Open in DMS* |

Keys in the fast reader: `Space` start/pause · `←` `→` step · `↑` `↓` speed · `Home` restart · `Esc` close.

## How it works

```mermaid
flowchart LR
    E[Explorer] -->|selects PDF| PH["prevhost.exe<br/>DmsExplorer.PreviewHandler<br/>(COM, base .NET runtime)"]
    PH -->|starts with --parent hwnd| APP["DmsExplorer.exe preview<br/>(self-contained WinForms)"]
    APP -->|embeds its window| E
    APP -->|buttons| R[Fast reader window]
    APP -->|buttons| N[Notes window]
    APP -->|button| O[Open in DMS]
    O -->|"GET /api/documents/by-hash/{sha256}<br/>POST /api/documents/upload<br/>Bearer access token"| API[DMS REST API]
    O -->|opens| W[DMS web UI]
    R & N --> D[(%APPDATA%\DmsExplorer)]
```

- **Preview handler** – a tiny COM component (`IPreviewHandler`) without UI dependencies. Explorer loads it into `prevhost.exe` using the machine-wide .NET runtime; it starts the companion app and passes the preview pane window.
- **Companion app** – self-contained, so it does not depend on which .NET runtimes are installed. It turns its window into a child of the preview pane and follows its size; it exits when the pane goes away.
- **Core** – notes, reader progress, settings, PDF text extraction ([PdfPig](https://github.com/UglyToad/PdfPig)) and RSVP timing, covered by unit tests.

## Install

Requirements: Windows 10/11 x64, [.NET 8 SDK](https://dotnet.microsoft.com/download) to build, and a machine-wide .NET 8+ runtime (`C:\Program Files\dotnet`) for the preview handler.

```powershell
cd desktop
.\install.ps1
```

The script builds everything as your user, then asks **once for administrator rights** to copy the files to `C:\Program Files\DmsExplorer` and register the preview handler and context menu machine-wide. Every registry step is written to `%TEMP%\DmsExplorer-install.log`.

> **Why administrator rights?** Current Windows versions refuse per-user COM registrations from unknown programs and protect the `.pdf` association (User Choice Protection Driver). A machine-wide registration is the supported way for preview handlers.

Then open Explorer, turn on the preview pane (`Alt+P`) and select a PDF. The context menu entry is under *Show more options* on Windows 11.

The install replaces the PDF preview (usually Edge's) for this PC. Uninstall restores it:

```powershell
.\install.ps1 -Uninstall
```

Notes and settings stay in `%APPDATA%\DmsExplorer` (`settings.json` holds the DMS URL, default `http://localhost:8080`).

## Notes in the DMS

A note is local until you click **Share to DMS**. It is then posted as a comment on the matching DMS document (the document is uploaded first if you agree) and the two stay linked:

| Situation | What happens |
|---|---|
| you edit the note | the comment is updated after the autosave |
| the comment was edited in the web UI | the note takes the new text when it is opened |
| both were changed differently | you choose which version to keep |
| the comment was deleted in the DMS | the link is removed, the note stays local |

The rule compares both sides with the text they last agreed on, so it does not depend on clocks. Other comments on the document are listed below the note. Deleting the note locally keeps the DMS comment.

## Connect to the DMS

*Open in DMS* needs a personal access token of your DMS account:

1. Log in to the DMS web UI and create a token (`POST /api/tokens` with `{ "name": "Laptop" }`; a page in the web UI will follow).
2. Run `DmsExplorer connect` (or just click *Open in DMS* – the dialog appears when no token is saved), enter the DMS address and paste the token.

The connection is tested before anything is saved. The token is stored encrypted for your Windows account (DPAPI, `%APPDATA%\DmsExplorer\token.bin`); *Remove token* in the same dialog deletes it. Revoke it in the DMS when a device is lost.

## Development

```powershell
dotnet test desktop/DmsExplorer.sln            # core unit tests
dotnet run --project desktop/src/DmsExplorer.App -- preview C:\path\file.pdf
dotnet run --project desktop/src/DmsExplorer.App -- reader  C:\path\file.pdf
dotnet run --project desktop/src/DmsExplorer.App -- notes   C:\path\file.pdf
dotnet run --project desktop/src/DmsExplorer.App -- open    C:\path\file.pdf
dotnet run --project desktop/src/DmsExplorer.App -- connect
```

Without `--parent` the preview opens as a normal window, which is the easiest way to work on the UI.

```text
desktop/
├── src/
│   ├── DmsExplorer.Core/            notes, settings, reader progress, PDF text, RSVP timing, DMS API client
│   ├── DmsExplorer.App/             companion app: preview, fast reader, notes, connect, open in DMS (WinForms)
│   └── DmsExplorer.PreviewHandler/  COM preview handler loaded by Explorer
├── tests/DmsExplorer.Core.Tests/    xUnit tests with generated PDFs
└── install.ps1                      build, install, register / uninstall
```

## Roadmap

1. ✅ Preview pane, fast reader, local notes, context menu
2. ✅ Find a document by content hash · ✅ access tokens · ✅ open in DMS / upload from Explorer · ✅ notes synced with DMS comments · web UI page for tokens
3. Fast reader and notes on the OCR text of scanned PDFs (from the DMS worker)
4. Signed MSIX package and settings window
