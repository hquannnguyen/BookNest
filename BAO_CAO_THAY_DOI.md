# BÁO CÁO THAY ĐỔI — Quản Lý Sách Cá Nhân
**Ngày:** 01/09/2026  
**Branch:** `develop`  
**Commit gốc:** `43b4254` — feat: QuanNH Android Kotlin base framework  
**Tổng thay đổi:** 12 file sửa · 5 file/thư mục mới · 177 dòng thêm · 79 dòng xóa

---

## 1. TỔNG QUAN

Sau commit khung base, nhóm thực hiện 2 nhóm thay đổi chính:

| # | Nhóm thay đổi | File ảnh hưởng |
|---|---|---|
| A | **Thêm đa ngôn ngữ (i18n)** — hỗ trợ Tiếng Việt và English | 12 file sửa + 5 file mới |
| B | **Fix lỗi encoding** — comment tiếng Việt bị vỡ font | 4 Activity `.kt` |

---

## 2. FILE MỚI (Untracked — cần git add)

### 2.1 `ui/base/BaseActivity.kt`
```
app/src/main/java/com/example/quanlysachcanhan/ui/base/BaseActivity.kt
```
- Abstract class kế thừa `AppCompatActivity`
- Override `attachBaseContext()` — tự động wrap Context với ngôn ngữ đã lưu
- **Tất cả Activity phải kế thừa class này** để i18n hoạt động

### 2.2 `utils/LocaleHelper.kt`
```
app/src/main/java/com/example/quanlysachcanhan/utils/LocaleHelper.kt
```
- `wrap(context, languageCode)` — trả về Context với locale đã cấu hình
- `setLocale(context, code)` — lưu ngôn ngữ vào PreferenceManager
- `getLanguage(context)` — đọc ngôn ngữ hiện tại

### 2.3 `res/values-vi/strings.xml`
```
app/src/main/res/values-vi/strings.xml
```
- 66 string keys tiếng Việt tường minh
- Android tự load khi `Locale = "vi"`

### 2.4 `res/values-en/strings.xml`
```
app/src/main/res/values-en/strings.xml
```
- 66 string keys tiếng Anh
- Android tự load khi `Locale = "en"`

### 2.5 `KeHoach_QuanLySach_2026.xlsx`
- File Excel kế hoạch dự án 5 sheet: TONG QUAN, QuanNH, KienTT, PhongVV, GANTT

---

## 3. FILE SỬA — UI LAYER (4 Activity)

### 3.1 `MainActivity.kt`

| Thay đổi | Chi tiết |
|---|---|
| Kế thừa | `AppCompatActivity` → `BaseActivity` |
| Fix encoding | Comment TODO QuanNH bị vỡ font → ASCII sạch |

### 3.2 `AddEditBookActivity.kt`

| Thay đổi | Chi tiết |
|---|---|
| Kế thừa | `AppCompatActivity` → `BaseActivity` |
| Fix encoding | Comment TODO KienTT IMAGE + 4 dòng mô tả → ASCII sạch |
| Fix string | `edtTitle.error = "Vui long..."` → `getString(R.string.error_title_empty)` |
| Fix string | `edtAuthor.error = "Vui long..."` → `getString(R.string.error_author_empty)` |
| Fix string | `Toast("Da them sach")` → `getString(R.string.msg_book_added)` |
| Fix string | `Toast("Da cap nhat")` → `getString(R.string.msg_book_updated)` |

> **Lưu ý:** Layout đã thêm `ImageView imgCover` (120×160dp) bên trên nút chọn ảnh để KienTT dùng.

### 3.3 `BookDetailActivity.kt`

| Thay đổi | Chi tiết |
|---|---|
| Kế thừa | `AppCompatActivity` → `BaseActivity` |
| Xóa import | `import androidx.appcompat.app.AppCompatActivity` (không còn cần) |
| Fix encoding | Comment TODO QuanNH + TODO KienTT QUOTE → ASCII sạch |

### 3.4 `StatisticsActivity.kt`

| Thay đổi | Chi tiết |
|---|---|
| Kế thừa | `AppCompatActivity` → `BaseActivity` |
| Fix encoding | Comment TODO PhongVV STATISTICS → ASCII sạch |
| Fix string | `tvYearLabel.text = "Sach da doc..."` → `getString(R.string.label_read_in_year, year)` |

---

## 4. FILE SỬA — UTILS

### 4.1 `PreferenceManager.kt`

| Thay đổi | Chi tiết |
|---|---|
| Thêm field | `var language: String` — đọc/ghi ngôn ngữ vào SharedPreferences |
| Thêm constant | `KEY_LANGUAGE = "language"` |
| Thêm constant | `DEFAULT_LANGUAGE = "vi"` |

---

## 5. FILE SỬA — LAYOUT XML (6 layout)

> **Nguyên tắc:** Toàn bộ `android:text` và `android:hint` hardcode → `@string/<key>`.

### 5.1 `activity_main.xml`

| Thuộc tính cũ (hardcode) | Thay bằng |
|---|---|
| `hint="Tim theo ten sach..."` | `@string/hint_search` |
| `text="Loc"` | `@string/btn_filter` |
| `text="Sap xep"` | `@string/btn_sort` |
| `text="Thong ke"` | `@string/btn_statistics` |
| `text="Chua co sach"` | `@string/label_empty_books` |
| `contentDescription="Them sach"` | `@string/fab_add_book` |

### 5.2 `activity_add_edit_book.xml`

| Thuộc tính cũ | Thay bằng |
|---|---|
| `hint="Ten sach"` | `@string/hint_title` |
| `hint="Tac gia"` | `@string/hint_author` |
| `hint="Ghi chu ca nhan"` | `@string/hint_note` |
| `text="Chon anh bia"` | `@string/btn_choose_cover` |
| `text="Luu sach"` | `@string/btn_save` |

> **Thay đổi cấu trúc:** Thêm `ImageView imgCover` (120x160dp) + `Button btnChooseCover` ở đầu layout.

### 5.3 `activity_book_detail.xml`

| Thuộc tính cũ | Thay bằng |
|---|---|
| `text="Sua"` | `@string/btn_edit` |
| `text="Chia se"` | `@string/btn_share` |
| `text="Xoa"` | `@string/btn_delete` |
| `text="+ Them trich dan"` | `@string/btn_add_quote` |

### 5.4 `activity_statistics.xml`

| Thuộc tính cũ | Thay bằng |
|---|---|
| `text="Tong so sach"` | `@string/label_total_books` |
| `text="Chua doc"` | `@string/label_unread` |
| `text="Dang doc"` | `@string/label_reading` |
| `text="Da doc"` | `@string/label_read` |

### 5.5 `item_book.xml`

| Thuộc tính cũ | Thay bằng |
|---|---|
| `contentDescription="Anh bia"` | `@string/content_description_cover` |

### 5.6 `item_quote.xml`

| Thuộc tính cũ | Thay bằng |
|---|---|
| `contentDescription="Xoa trich dan"` | `@string/content_description_delete_quote` |

---

## 6. STRINGS.XML — 66 KEY

```
app/src/main/res/values/strings.xml
```

Từ **1 key** (`app_name`) tăng lên **66 key**, nhóm theo chức năng:

| Nhóm | Keys |
|---|---|
| App | `app_name` |
| MainActivity | `hint_search`, `btn_filter`, `btn_sort`, `btn_statistics`, `label_empty_books`, `fab_add_book` + 1 |
| AddEditBook | `hint_title`, `hint_author`, `hint_note`, `btn_choose_cover`, `btn_save`, `title_add_book`, `title_edit_book`, `error_title_empty`, `error_author_empty`, `msg_book_added`, `msg_book_updated` |
| BookDetail | `btn_edit`, `btn_share`, `btn_delete`, `btn_add_quote`, `dialog_delete_title`, `dialog_delete_message`, `dialog_delete_confirm`, `dialog_delete_cancel`, `msg_book_deleted` |
| Quote | `dialog_add_quote_title`, `dialog_edit_quote_title`, `dialog_delete_quote_message`, `hint_quote_content`, `msg_quote_added`, `msg_quote_deleted` |
| Statistics | `title_statistics`, `label_total_books`, `label_unread`, `label_reading`, `label_read`, `label_read_in_year` |
| Filter/Sort | `dialog_filter_title`, `dialog_sort_title`, `filter_all`, `sort_title`, `sort_author`, `sort_rating`, `sort_status` |
| ReadingStatus | `status_unread`, `status_reading`, `status_read` |
| Category | `category_science`, `category_literature`, `category_economics`, `category_children`, `category_life_skills`, `category_it`, `category_psychology`, `category_history`, `category_other` |
| Share | `share_chooser_title` |
| Settings | `title_settings`, `pref_dark_mode`, `pref_language`, `lang_vietnamese`, `lang_english` |

---

## 7. CẤU TRÚC FILE SAU THAY ĐỔI

```
res/
├── values/                        [SỬA: 1 → 66 keys]
│   └── strings.xml
├── values-vi/                     [MỚI]
│   └── strings.xml                66 keys Tiếng Việt
└── values-en/                     [MỚI]
    └── strings.xml                66 keys English

ui/
├── base/
│   └── BaseActivity.kt            [MỚI]
├── main/MainActivity.kt           [SỬA]
├── addedit/AddEditBookActivity.kt [SỬA]
├── detail/BookDetailActivity.kt   [SỬA]
└── stats/StatisticsActivity.kt    [SỬA]

utils/
├── LocaleHelper.kt                [MỚI]
└── PreferenceManager.kt           [SỬA: thêm var language]
```

---

## 8. VIỆC CẦN LÀM TIẾP

### PhongVV — implement nút đổi ngôn ngữ trong Settings
```kotlin
// Khi user chọn ngôn ngữ
LocaleHelper.setLocale(this, "en")  // hoặc "vi"
recreate()                           // áp dụng ngay không cần restart
```

### Commit đề xuất
```bash
git add .
git commit -m "feat: i18n support vi/en + fix encoding + refactor string resources"
```

---

*Báo cáo tự động tạo ngày 01/09/2026*
