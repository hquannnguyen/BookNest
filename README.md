# Quản lý sách cá nhân — Base Project

Base Android Kotlin để nhóm phát triển các module độc lập.

## Kiến trúc

```text
UI (Activity)
   ↓
Repository
   ↓
DatabaseHelper
   ↓
SQLite
```

## Package

```text
com.example.quanlysachcanhan
├── data
├── model
├── adapter
├── ui
│   ├── main
│   ├── addedit
│   ├── detail
│   └── stats
└── utils
```

## Base đã có

- SQLiteOpenHelper + schema books, quotes
- BookRepository:
  - insert/update/delete
  - getById
  - search/filter/sort
  - count/thống kê cơ bản
- QuoteRepository CRUD cơ bản
- Model Book, Quote
- RecyclerView BookAdapter, QuoteAdapter
- MainActivity
- AddEditBookActivity
- BookDetailActivity
- StatisticsActivity
- SharedPreferences helper
- ShareUtils
- ImageStorageHelper
- ViewBinding
- Layout XML cơ bản

## TODO chia module

### QuanNH — Book CRUD + Search / Filter / Sort
- Hoàn thiện Add/Edit validation
- Hiển thị ảnh bìa trong danh sách và chi tiết
- Dialog xác nhận delete
- Hoàn thiện UI chi tiết sách
- Tìm kiếm theo tên sách hoặc tác giả
- Lọc theo thể loại và trạng thái đọc
- Sắp xếp theo tiêu đề, tác giả, đánh giá, trạng thái
- Lưu tùy chọn sắp xếp vào PreferenceManager

### KienTT 2 — Image + Quote + Share
- Photo Picker / Gallery
- Camera
- Lưu ảnh qua ImageStorageHelper
- Hiển thị ảnh sau khi chọn/chụp
- Add/Edit/Delete quote
- Danh sách nhiều trích dẫn theo từng sách
- Share book qua Intent.ACTION_SEND

### PhongVV — Statistics + Settings + UI hoàn thiện
- Thống kê tổng số sách
- Thống kê theo thể loại
- Thống kê theo trạng thái đọc
- Số sách đã đọc trong năm
- Biểu đồ đơn giản bằng ProgressBar hoặc MPAndroidChart
- Dark mode
- Settings UI
- Đồng bộ giao diện sáng/tối và hoàn thiện UI chung

## Quy ước

1. Không query SQLite trực tiếp trong Activity/Adapter.
2. Activity gọi Repository.
3. Adapter chỉ bind dữ liệu và phát callback.
4. Constants dùng chung trong `utils/Constants.kt`.
5. Không hard-code tên column SQLite trong UI.
6. Khi thêm column phải nâng DATABASE_VERSION và viết migration trong onUpgrade().
7. Branch feature theo module, merge qua develop.
