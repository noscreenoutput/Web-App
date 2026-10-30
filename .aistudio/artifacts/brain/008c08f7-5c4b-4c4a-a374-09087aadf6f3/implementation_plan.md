# Egui WASM Launcher for Android

Sebuah launcher native Android berbasis Jetpack Compose dan Android System WebView yang dirancang khusus untuk menjalankan aplikasi WebAssembly (WASM) berbasis **egui** (Rust GUI library) dan web-native graphics (WebGL/WebGPU) secara aman, efisien, dan responsif terhadap sentuhan.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan jawaban klarifikasi yang telah Anda berikan:
> 1. **Sumber Aplikasi**: Pengguna mengimpor paket aplikasi WASM/HTML dari penyimpanan lokal perangkat (berupa arsip .zip atau bundle file HTML/WASM/JS).
> 2. **Jembatan Native (Bridge API)**: Jembatan `AndroidBridge` menyediakan akses File Picker perangkat (`Intent.ACTION_OPEN_DOCUMENT` / `CREATE_DOCUMENT`), sandboxed local storage untuk tiap aplikasi, dan integrasi Clipboard.
> 3. **Tampilan & Kontrol**: Layar penuh dengan opsi imersif yang dapat diatur saat impor aplikasi, dilengkapi tombol akses cepat di sisi kanan untuk menampilkan bilah kontrol pengubah ukuran (viewport scaling), toggle live console log HUD, dan tombol reload/exit.

- **Keputusan Terkonfirmasi 1**: Paket aplikasi diimpor dalam bentuk file arsip ZIP atau berkas HTML/WASM yang kemudian diekstrak ke direktori terisolasi (`filesDir/wasm_apps/{appId}/`) demi keamanan dan isolasi penyimpanan.
- **Keputusan Terkonfirmasi 2**: Local asset serving menggunakan `WebViewAssetLoader` / local HTTP scheme (`https://appassets.androidplatform.net/`) agar WebAssembly (`WebAssembly.instantiateStreaming`), WebGL, dan Workers dapat berjalan tanpa blokade keamanan CORS browser.
- **Keputusan Terkonfirmasi 3**: Dukungan penyediaan contoh bawaan ("egui Showcase Demo") siap uji agar pengguna dapat langsung memverifikasi launcher tanpa harus menyiapkan file ZIP eksternal terlebih dahulu.

---

## 1. Overview & Core Concept

- **Apa yang Dilakukan**: Aplikasi ini bertindak sebagai runtime dan launcher mandiri untuk aplikasi WASM GUI (terutama egui Rust). Pengguna dapat mengelola daftar aplikasi WASM, mengimpor bundel baru, menyesuaikan preferensi tampilan (mode imersif, skala resolusi), dan menjalankannya di atas Android System WebView dengan akselerasi perangkat keras GPU.
- **Target Pengguna**: Pengembang Rust/WASM, antusias egui, dan pengguna yang ingin menjalankan aplikasi alat bantu (tools, CAD/design viewers, dashboard, game ringan) berformat WebAssembly di Android secara offline tanpa browser eksternal.
- **Nilai Utama**:
  - Eksekusi WASM lokal berkecepatan tinggi tanpa koneksi internet.
  - Akses storage & file picker native lewat jembatan JavaScript yang aman.
  - Floating runtime HUD di sebelah kanan untuk debugging live console logs dan penyesuaian resolusi viewport.

---

## 2. User Experience & Visual Design

### Alur Pengguna (User Flows)
1. **Layar Utama (Launcher Dashboard)**:
   - Menampilkan daftar kartu aplikasi WASM yang telah terpasang dengan nama, ukuran file, tanggal impor, dan status mode (Immersive vs Standard).
   - Tombol Floating Action Button (FAB) atau tombol aksi utama di Top Bar: **"Impor Paket WASM (.zip)"**.
   - Kartu demo pra-instal ("egui Interactive Showcase") siap klik untuk uji coba interaksi sentuh, slider, window docking, dan rendering grafis.
2. **Dialog Impor & Konfigurasi Paket**:
   - Memilih file `.zip` melalui File Picker sistem Android.
   - Menampilkan deteksi file (`index.html`, `.wasm`, `.js`, `.css`).
   - Formulir konfigurasi: Nama Aplikasi, Ikon/Warna Identitas, dan Opsi Mode Imersif (Sembunyikan status bar / navigation bar saat berjalan).
3. **Layar Runner (WASM Runtime Viewport)**:
   - WebView layar penuh dengan hardware acceleration (WebGL 1.0/2.0 aktif).
   - Tombol apung (floating pill / FAB) minimalis di tepi kanan layar.
   - Saat tombol tepi kanan ditekan, membuka **Runner Quick Menu**:
     - **Toggle Console Log HUD**: Menampilkan terminal transparan di bagian bawah/samping dengan filter Log, Info, Warn, Error, dan tombol Copy Log.
     - **Viewport Scaling**: Tombol pilihan skala (Fit Screen, 100% Native, 75%, 125%) untuk aplikasi dengan kepadatan DPI berbeda.
     - **Toggle Immersive**: Menghidupkan/mematikan bilah status sistem secara instan.
     - **Reload WASM**: Memuat ulang runtime.
     - **Tutup / Kembali ke Launcher**: Navigasi kembali ke daftar aplikasi dengan aman.

### Panduan Visual & Tema (Material Design 3)
- **Tema Warna**:
  - Gaya modern bertema *Cyber-Slate / Tech Studio*:
  - Dominan `surface` bernuansa gelap lembut (`#121316` / `#1E2024`) untuk menonjolkan kanvas egui.
  - Aksen primer *Rust Amber / Bright Cyan* (`#E07A5F` / `#38BDF8`) yang identik dengan komunitas Rust & WebAssembly.
- **Tipografi**:
  - Judul: Sans-serif modern, kontras jelas, tegas.
  - Console Log & Status Data: Monospace bersih untuk kemudahan membaca pesan debug.
- **Kepatuhan M3 & Instruksi Desain**:
  - Mengikuti batasan `INSTRUCT.MD`: Tidak ada card berlapis tanpa arti, tidak ada gradien berlebihan, menggunakan komponen M3 standar (`ListItem`, `ModalBottomSheet`, `OutlinedTextField`, `Button`, `TopAppBar`).

---

## 3. Key Product Decisions & Trade-Offs

### Keputusan 1: Mekanisme Serving Konten Lokal (WebViewAssetLoader)
- **Pendekatan Terpilih**: Menggunakan `androidx.webkit.WebViewAssetLoader` dengan domain virtual `https://appassets.androidplatform.net/assets/` atau `InternalStoragePathHandler`.
- **Alasan**: File WASM yang dimuat dari `file:///android_asset/` sering diblokir oleh kebijakan keamanan modern Chromium (CORS & MIME type restriction `application/wasm`). Dengan virtual domain HTTPS lokal, API seperti `WebAssembly.instantiateStreaming` dan `fetch` lokal bekerja normal tanpa server HTTP lokal di background yang membebani baterai.
- **Alternatif yang Dipertimbangkan**: Embedded NanoHTTPD server (lebih boros daya, rentan port collision).

### Keputusan 2: Arsitektur Jembatan Native (JavaScript Interface)
- **Pendekatan Terpilih**: `@JavascriptInterface` bernama `AndroidBridge` yang mengekspos:
  - `openFilePicker(callbackId, mimeType)`
  - `saveFile(callbackId, fileName, mimeType, base64Content)`
  - `copyClipboard(text)` / `getClipboard(callbackId)`
  - `writeStorage(key, value)` / `readStorage(key, callbackId)`
  - `sendConsoleLog(level, message)`
- **Alasan**: Menyediakan koneksi dua arah antara frontend WASM egui dan API penyimpanan perangkat Android secara non-blocking melalui event bus / callback javascript `window.onNativeResult(callbackId, data)`.

### Keputusan 3: Penanganan Interaksi Sentuh (Touch Events) untuk egui
- **Pendekatan Terpilih**: Penyesuaian viewport meta tag dan pointer event capture pada container HTML template agar egui dapat membedakan sentuhan (single tap, drag, pinch-to-zoom) dengan akurat tanpa terganggu gesture zoom default browser.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │ LauncherDashboardView │   │  WasmRunnerScreen    │  │
│  │ - Installed App List  │   │  - Accelerated View  │  │
│  │ - Import ZIP Sheet    │   │  - Right Edge HUD    │  │
│  │ - App Info & Settings │   │  - Console Log Panel │  │
│  └───────────┬───────────┘   └──────────┬───────────┘  │
│              │                          │              │
│              ▼                          ▼              │
│  ┌───────────────────────┐   ┌──────────────────────┐  │
│  │   LauncherViewModel   │   │ AndroidBridge (JS)   │  │
│  │ - Room DB Repository  │   │ - File Picker Intent │  │
│  │ - ZipExtractorService │   │ - Local Storage I/O  │  │
│  │ - App Lifecycle State │   │ - Clipboard Manager  │  │
│  └───────────┬───────────┘   └──────────┬───────────┘  │
└──────────────┼──────────────────────────┼──────────────┘
               ▼                          ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│     Room AppDatabase     │   │   Sandbox File Storage   │
│  - AppEntity (id, title, │   │   context.filesDir/apps/ │
│    entryHtml, path,      │   │   ├── app_1/             │
│    isImmersive, size)    │   │   │   ├── index.html     │
│                          │   │   │   ├── app_bg.wasm    │
│                          │   │   │   └── app.js         │
└──────────────────────────┘   └──────────────────────────┘
```

### Komponen Utama yang Akan Dibangun:
1. **Model & Database (Room)**:
   - `WasmAppEntity`: Menyimpan ID, nama, deskripsi, tanggal instalasi, ukuran berkas, path folder lokal, ikon/warna, pengaturan mode imersif, dan resolusi default.
   - `AppDatabase` & `WasmAppDao`: Menyediakan reactive `Flow<List<WasmAppEntity>>`.
   - `WasmAppRepository`: Mengelola query dan operasi hapus/tambah aplikasi.
2. **File & ZIP Extraction Service**:
   - `ZipPackageManager`: Membaca stream URI file ZIP yang dipilih pengguna, memvalidasi keberadaan `index.html` dan `.wasm`, mengekstrak berkas ke direktori pribadi aplikasi yang aman.
   - Penyiapan paket contoh (Built-in Demo Package) dengan interaktivitas egui yang dapat langsung diekstrak saat pertama kali dibuka jika diinginkan pengguna.
3. **JavaScript Native Bridge**:
   - `AndroidBridge.kt`: Terkoneksi dengan `ActivityResultLauncher` di Activity untuk file picker (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`), clipboard Android, dan logging.
4. **WasmRunnerScreen & Overlay UI**:
   - Custom `AndroidView(factory = { ... })` yang mengonfigurasi `WebView`, `WebSettings` (JavaScript enabled, DOM Storage, WebGL flag), dan `WebViewClient` / `WebChromeClient`.
   - Menginjeksi jembatan log `console.log`, `console.error`, `console.warn` ke State Jetpack Compose.
   - Tombol overlay di sebelah kanan (`Box` dengan `Alignment.CenterEnd`) yang dapat membuka menu melayang:
     - Switch ukuran/skala viewport.
     - Toggle Live Console Log Terminal (Bottom Sheet / Collapsible Card).
     - Tombol Kembali / Reload.
5. **Dukungan Touch & WebGL**:
   - Skrip pembungkus ringan untuk menjamin event sentuh diteruskan ke egui Canvas tanpa lag gesture default browser.

---

## Tahapan Implementasi (Execution Plan)

1. **Konfigurasi Dependensi & Manifest**:
   - Periksa `libs.versions.toml` dan `build.gradle.kts`: aktifkan `androidx.webkit:webkit`.
   - Perbarui `AndroidManifest.xml` dengan label, hardware acceleration `android:hardwareAccelerated="true"`, dan metadata yang sesuai.
2. **Lapisan Data (Room Database & Storage Manager)**:
   - Buat `WasmAppEntity`, `WasmAppDao`, `AppDatabase`, dan `WasmAppRepository`.
   - Buat `ZipPackageManager` untuk unzip dan manajemen berkas lokal di `context.filesDir`.
3. **Jembatan Komunikasi Native-WASM (`AndroidBridge`)**:
   - Definisikan interface komunikasi JS ke Kotlin: file picker, storage key-value, clipboard, dan logging.
   - Siapkan template HTML fallback & injection loader egui.
4. **Antarmuka Utama (Launcher Dashboard)**:
   - Buat `LauncherScreen` dengan daftar aplikasi M3 (`ListItem`), status info, FAB untuk impor file ZIP, dan dialog konfigurasi impor.
   - Tambahkan menu detail aplikasi untuk melihat ukuran direktori dan menghapus aplikasi.
5. **Antarmuka Eksekutor (WasmRunnerScreen) & Floating Controls**:
   - Buat `WasmRunnerScreen` dengan Android System WebView terakselerasi GPU.
   - Buat floating button di sisi kanan layar untuk membuka Runner Controls:
     - Zoom / Viewport Scaler.
     - Live Monospace Console Log Terminal (Filterable).
     - Pengaturan imersif on-the-fly.
     - Tombol kembali ke Launcher.
6. **Verifikasi & Build Test**:
   - Jalankan `compile_applet` untuk memastikan seluruh kode terkompilasi bersih tanpa error.
