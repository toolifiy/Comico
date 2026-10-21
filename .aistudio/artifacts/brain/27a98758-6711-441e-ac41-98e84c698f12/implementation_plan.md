# Comic & Book Studio — Pro CapCut / VN UI Redesign

A complete visual and interactive overhaul of the Comic & Book PDF Maker, adopting an authentic **CapCut & VN Video Editor** aesthetic: **Pure White** canvas, **Pitch Black** high-contrast surfaces, **Electric Cyan** accents, custom geometric typography, and a switchable **2-Column Comic Grid + Storyboard Strip** editor.

## User Review & Critical Decisions

> [!IMPORTANT]
> Confirmed design choices from your answers:
> - **Homescreen Hero**: CapCut-style large full-width gradient card with a prominent floating `+` icon and decorative canvas light arcs.
> - **Color Palette**: Pure White (`#FFFFFF`) background with Pitch Black (`#0D0F14`) and Electric Cyan (`#00D4FF` / `#00B8D9`) highlights.
> - **Comic Editor Layout**: Instant toggle between a **2-Column Visual Comic Grid** (for fast overview and page management) and a **Storyboard Strip View** (with inline between-page `+` insertion connectors).

- **Confirmed Decision 1**: CapCut-style gradient hero banner + quick action tool strip on the Homescreen.
- **Confirmed Decision 2**: Pure White, Pitch Black, and Electric Cyan high-contrast studio palette paired with custom bundled Google Fonts (`Outfit` & `Plus Jakarta Sans`).
- **Confirmed Decision 3**: Dual-mode Comic Editor (2-Column Grid + Storyboard Strip) with floating bottom studio dock.

---

## 1. Overview & Core Concept

- **What It Delivers**: Transforms the app from a basic utility into a sleek, tactile mobile creator studio modeled after CapCut and VN Editor.
- **Key Workflow Upgrade — "Quick Import & Create"**: In addition to creating a named project, tapping **"Quick Import"** on the home screen lets you pick multiple gallery images immediately—auto-creating a project and dropping you straight into the editor with all selected pages loaded!
- **Project Duplication & Search**: Filter saved history by project name, sort by recent/page count, or duplicate an existing comic draft to experiment with different page orders without touching the original.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Aesthetic Direction**: High-craft Light Mode Creator Studio (CapCut / VN DNA).
- **Color Tokens**:
  - `CanvasWhite` (`#FFFFFF`) — Main app background.
  - `StudioSurface` (`#F5F7FA`) — Secondary card & container fill.
  - `PitchBlack` (`#0B0D12`) — Primary typography, floating bottom dock, and high-contrast badges.
  - `ElectricCyan` (`#00C8F8` to `#0088FF`) — Hero gradient, active indicators, and primary CTAs.
  - `CrispBorder` (`#E4E8F0`) — Subtle 1dp structural hairlines.
- **Typography**:
  - Bundled locally via `font-util`: **Outfit** (Bold/ExtraBold for headers, page badges, and hero titles) paired with **Plus Jakarta Sans** (Medium/SemiBold for body copy, timestamps, and controls).

### Key User Flows & Screen Architecture

1. **Homescreen (CapCut / VN Studio Hub)**:
   - **Top Studio Bar**: Minimalist Pitch Black logo mark, bold title, and an expandable search bar to filter saved drafts.
   - **Hero "New Project" Card**: Full-width rounded card featuring a vibrant Electric Cyan-to-Deep-Azure linear gradient with custom Canvas decorative rings, a large elevated circular `+` button, and bold **"New project"** typography.
   - **4-Column Quick Studio Tools**:
     - **Quick Import**: Pick photos first -> auto-creates comic draft immediately.
     - **New Blank**: Start with custom title & notes.
     - **Grid / List View**: Switch how history projects are displayed.
     - **Reverse / Sort**: Toggle project sorting (Newest vs Most Pages).
   - **Saved History ("Drafts")**:
     - Viewable as either a **2-Column Cover Showcase** or a **VN-style Horizontal Row List**.
     - Each item shows a 3:4 or 16:9 cover preview, glassmorphic page count badge (`14 Pages`), relative edit time, one-tap **Rename**, **Duplicate**, and **Delete** controls.

2. **Comic Editor Studio (Switchable Grid & Strip)**:
   - **Top Studio Bar**: Back button, inline editable Project Title, Page Count pill, and a **Grid / Strip View Switcher** icon button.
   - **Mode A — 2-Column Visual Comic Grid (Default)**:
     - Displays comic pages side-by-side in a clean 2-column grid with aspect-ratio-true page previews.
     - Top-left Pitch Black badge (`01`, `02`, `03`).
     - Compact control bar on each card: `←` Move Left, `→` Move Right, `🔄` Replace, `➕` Insert After, and `🗑️` Delete.
   - **Mode B — Storyboard Strip View**:
     - Full-width cards with dedicated **"+ Insert Page Between #X & #Y"** pill connectors between every single image.
   - **Floating Pitch-Black Bottom Studio Dock**:
     - Sleek rounded floating dock at the bottom of the screen containing **"+ Add Pages"** (multi-picker) and the glowing Electric Cyan **"Export PDF"** button.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Editor Modes (2-Column Grid + Storyboard Strip)**
  - *Chosen Approach*: Provide an instant toggle in the editor top bar between a 2-column grid and a 1-column storyboard strip.
  - *Why*: When arranging a 30-page comic, a 2-column grid lets you see 6 pages on screen at once for fast visual ordering, while the 1-column strip makes between-page insertion effortless.
- **Decision 2: One-Tap "Quick Import" from Homescreen**
  - *Chosen Approach*: Allow selecting gallery images directly from the homescreen without typing a title first (auto-generates e.g. *"Comic Draft #1"* which can be renamed anytime).
  - *Why*: Matches CapCut's frictionless workflow where tapping "+ New Project" or "Quick Import" immediately gets you into editing.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI (Clean White)                │
│                                                                   │
│  ┌─────────────────────────────┐   ┌───────────────────────────┐  │
│  │   CapCut / VN Homescreen    │   │   Dual-Mode Comic Editor  │  │
│  │  • Electric Cyan Hero Card  │   │  • 2-Col Comic Grid View  │  │
│  │  • Quick Photo-First Import │   │  • 1-Col Storyboard Strip │  │
│  │  • Grid/List Draft History  │   │  • Between-Page '+' Nodes │  │
│  │  • Search & Duplicate       │   │  • Floating Black Dock    │  │
│  └──────────────┬──────────────┘   └─────────────┬─────────────┘  │
│                 └───────────────┬────────────────┘                │
│                                 ▼                                 │
│                  ComicStudioViewModel (StateFlow)                 │
│                                 │                                 │
│        ┌────────────────────────┼────────────────────────┐        │
│        ▼                        ▼                        ▼        │
│   BookRepository        ImageFileManager            PdfExporter   │
│  (Room SQLite DB)     (Private File Copy)      (Native Canvas PDF)│
└───────────────────────────────────────────────────────────────────┘
```

### Interactive State & New Capabilities
- **`quickCreateWithImages(uris: List<Uri>)`**: Creates a new `BookEntity` in Room, copies all selected `uris` to internal storage, populates `BookPageEntity` records in order, and navigates straight to `ScreenState.Editor(newBookId)`.
- **`duplicateBook(bookId: Long)`**: Clones an existing book's metadata and page references so users can safely test alternative comic arrangements.
- **`editorViewMode: StateFlow<EditorViewMode>`**: Toggles between `GRID_2_COL` and `STORYBOARD_STRIP` without losing scroll context.
