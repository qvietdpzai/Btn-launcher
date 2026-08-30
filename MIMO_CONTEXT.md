# BtnLauncher — Project Context for Mimo Code

> File này tóm tắt lịch sử và trạng thái dự án để AI/editor (Mimo) hiểu nhanh.

---

## 1. TỔNG QUAN

| Project | Repo GitHub | Nền tảng | Ngôn ngữ |
|---------|-------------|----------|----------|
| BtnLauncher (Mobile) | `qvietdpzai/Btn-Launcher` | Android (Minecraft Java Launcher) | Java + C/JNI (NDK) |
| BtnLauncher2 (PC) | `qvietdpzai/Btnlaucher2-pc` | Windows/macOS/Linux (Electron) | JS/Electron |

**Mục tiêu:** Làm trang web tải cả 2 launcher từ GitHub Releases.

---

## 2. LỊCH SỬ PHIÊN LÀM VIỆC (session này)

### Bước 1: Chuẩn bị môi trường
- Cài `git`, `openjdk17`, `rsync`, `curl`, `python3` trên Termux/Alpine
- Clone repo `qvietdpzai/Btn-Launcher` về `~/btn-launcher`

### Bước 2: Cố gắng modernize UI + Vulkan (THẤT BẠI → build fail)
- Thêm `VulkanSurfaceProvider.java`, `SurfaceProviderFactory.java`, `FramePacer.java`
- Thêm `vulkan_bridge.c` + `find_package(Vulkan REQUIRED)` trong `CMakeLists.txt`
- **Lý do fail:** `find_package(Vulkan REQUIRED)` yêu cầu Vulkan SDK không có sẵn trên GitHub Actions Runner → build C++/NDK lỗi

### Bước 3: Revert + version bump
- `git revert` toàn bộ phần Vulkan
- Bump `versionCode 140`, `versionName "1.4.0"` trong `app_pojavlauncher/build.gradle`

### Bước 4: UI an toàn (chỉ XML) — thành công
- `styles.xml`: `AppTheme` kế thừa `Theme.Material3.DayNight.NoActionBar`
- `colors_m3.xml`: bảng màu Material 3 (cyan `#17A2B8`)
- `background_card.xml`: thêm ripple effect
- `fragment_launcher.xml`: sidebar có elevation

### Bước 5: UI rõ rệt hơn (PILL BUTTON) — build fail
- Đổi `mine_button_unfocused.xml` → pill gradient
- **Lý do fail:** file `background_app_gradient.xml` BỊ THIẾU (lệnh tạo bị ngắt)

### Bước 6: Fix + hoàn thiện
- Tạo lại `background_app_gradient.xml`
- `mine_button_unfocused.xml`: pill bo 28dp + gradient
- `sidebar_item_bg.xml`: nền surface_variant
- `fragment_launcher.xml`: background gradient, logo header surface_variant
- **Kết quả:** v1.4.0 build thành công, user tải về được

### Bước 7: Trang web tải (Netlify)
- Tạo `docs/index.html`, `docs/style.css`, `docs/script.js`
- Tự động lấy release mới nhất từ GitHub API
- `netlify.toml` cấu hình publish từ `docs/`
- Fix bug: `const os` → `let os` (tab Windows/Mac không switching)
- Fix: `detectOS()` ưu tiên Windows/Mac trước Android (UA Android chứa "Linux")

---

## 3. TRẠNG THÁI HIỆN TẠI (commit cuối: `c506bd0`)

### Btn-Launcher (Mobile) v1.4.0
- ✅ Build thành công trên GitHub Actions
- ✅ UI: Material 3 theme, pill play button, gradient bg, ripple cards
- ✅ APK: `BtnLauncher-v1.4.0.apk` (81MB)
- ⚠️ Chưa có: Vulkan rendering (đã thử nhưng break build)

### Website (`docs/`)
- ✅ Tự động fetch release từ GitHub API
- ✅ 2 card: Mobile (APK) + PC (Windows/macOS/Linux)
- ✅ OS auto-detect + tab switching (đã fix)
- Deploy: Netlify (base=docs, publish=docs) hoặc GitHub Pages

---

## 4. CẤU TRÚC QUAN TRỌNG

```
Btn-Launcher/
├── app_pojavlauncher/
│   ├── src/main/
│   │   ├── java/net/kdt/pojavlaunch/
│   │   │   ├── fragments/MainMenuFragment.java   # Logic menu chính
│   │   │   └── render/                            # Rendering (SurfaceView/TextureView)
│   │   ├── jni/CMakeLists.txt                     # Native build (KHÔNG thêm Vulkan)
│   │   └── res/
│   │       ├── layout/fragment_launcher.xml       # Giao diện menu
│   │       ├── values/styles.xml                  # AppTheme (Material 3)
│   │       ├── values/colors_m3.xml               # M3 colors
│   │       └── drawable/
│   │           ├── mine_button_unfocused.xml      # Play button (pill gradient)
│   │           ├── background_card.xml            # Card (ripple)
│   │           └── background_app_gradient.xml    # App bg gradient
│   └── build.gradle                               # versionCode 140, versionName 1.4.0
├── docs/                                           # WEBSITE (Netlify)
│   ├── index.html
│   ├── style.css
│   ├── script.js
│   └── .nojekyll
├── netlify.toml                                    # Cấu hình deploy
└── MIMO_CONTEXT.md                                 # File này
```

---

## 5. LƯU Ý QUAN TRỌNG CHO MIMO

### ĐỪNG LÀM (sẽ break build):
- ❌ Thêm `find_package(Vulkan REQUIRED)` vào CMakeLists.txt
- ❌ Thêm file `.c` native mà không test本地
- ❌ Đổi `mcVersionSpinner` (custom view) thành `AutoCompleteTextView` — sẽ lỗi compile

### NÊN LÀM:
- ✅ Chỉ sửa XML resource để modernize UI (an toàn)
- ✅ Giữ nguyên logic Java/native hiện tại
- ✅ Test build trên GitHub Actions trước khi claim xong

### Quy tắc đặt tên color:
- Dùng `@color/m3_*` cho Material 3
- Dùng `@color/background_*` cho legacy

### Deploy website:
- Netlify: Import từ Git → Base `docs`, Publish `docs`
- Hoặc kéo thả thư mục `docs/` vào netlify.com/drop

---

## 6. CÁC LỆNH GIT THƯỜNG DÙNG

```bash
git add .
git commit -m "message"
git push origin main
git tag v1.4.0 && git push origin v1.4.0   # trigger Release Stable
```

---

## 7. LINK QUAN TRỌNG
- Mobile releases: https://github.com/qvietdpzai/Btn-Launcher/releases
- PC releases: https://github.com/qvietdpzai/Btnlaucher2-pc/releases
- Actions: https://github.com/qvietdpzai/Btn-Launcher/actions
