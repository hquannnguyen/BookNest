# 📚 HƯỚNG DẪN KIẾN TRÚC & Ý NGHĨA CÁC FILE TRONG DỰ ÁN BOOKNEST

> Tài liệu này dành cho lập trình viên muốn hiểu rõ cấu trúc dự án **BookNest**, nắm được **"Code UI ở đâu?"**, **"Code tính năng ở đâu?"**, và **quy trình chuẩn khi muốn thêm/sửa một chức năng** trong Android Studio.

---

## 📑 MỤC LỤC
1. [Nguyên lý cốt lõi: UI và Logic hoạt động thế nào trong Android?](#1-nguyên-lý-cốt-lõi-ui-và-logic-hoạt-động-thế-nào-trong-android)
2. [Bản đồ tra cứu nhanh: "Muốn code tính năng X thì vào file nào?"](#2-bản-đồ-tra-cứu-nhanh-muốn-code-tính-năng-x-thì-vào-file-nào)
3. [Cấu trúc chi tiết từng thư mục và ý nghĩa từng file](#3-cấu-trúc-chi-tiết-từng-thư-mục-và-ý-nghĩa-từng-file)
   - [A. Thư mục res/ (Tài nguyên & Giao diện XML)](#a-thư-mục-res-tài-nguyên--giao-diện-xml)
   - [B. Thư mục java/.../ui/ (Màn hình & Xử lý sự kiện)](#b-thư-mục-javaui-màn-hình--xử-lý-sự-kiện)
   - [C. Thư mục java/.../adapter/ (Cầu nối danh sách RecyclerView)](#c-thư-mục-javaadapter-cầu-nối-danh-sách-recyclerview)
   - [D. Thư mục java/.../data/ (Cơ sở dữ liệu SQLite & Xử lý dữ liệu)](#d-thư-mục-javadata-cơ-sở-dữ-liệu-sqlite--xử-lý-dữ-liệu)
   - [E. Thư mục java/.../model/ (Khuôn mẫu dữ liệu - Data Classes)](#e-thư-mục-javamodel-khuôn-mẫu-dữ-liệu---data-classes)
   - [F. Thư mục java/.../utils/ (Tiện ích hỗ trợ dùng chung)](#f-thư-mục-javautils-tiện-ích-hỗ-trợ-dùng-chung)
   - [G. Các file cấu hình cấp ứng dụng (Manifests & Gradle)](#g-các-file-cấu-hình-cấp-ứng-dụng-manifests--gradle)
4. [Quy trình 4 bước khi bạn muốn code thêm một tính năng mới](#4-quy-trình-4-bước-khi-bạn-muốn-code-thêm-một-tính-năng-mới)

---

## 1. NGUYÊN LÝ CỐT LÕI: UI VÀ LOGIC HOẠT ĐỘNG THẾ NÀO TRONG ANDROID?

Trong ứng dụng Android truyền thống (View System), **Giao diện (UI)** và **Chức năng (Logic)** luôn đi theo cặp:

```
┌─────────────────────────┐               ┌───────────────────────────────┐
│     GIAO DIỆN TĨNH      │               │       LOGIC & SỰ KIỆN         │
│     (res/layout/*.xml)  │ ◄───────────► │   (java/.../ui/*Activity.kt)   │
│  - Thiết kế nút bấm     │  ViewBinding  │  - Bắt sự kiện bấm nút        │
│  - Ô nhập chữ, Text     │               │  - Lấy dữ liệu hiển thị lên UI│
│  - Màu sắc, canh lề     │               │  - Chuyển màn hình            │
└─────────────────────────┘               └──────────────┬────────────────┘
                                                         │ Gọi xử lý
                                                         ▼
                                          ┌───────────────────────────────┐
                                          │      DỮ LIỆU & DATABASE       │
                                          │ (java/.../data/*Repository.kt)│
                                          │  - Thêm / Sửa / Xóa / Tìm kiếm│
                                          │  - Lưu vào SQLite Database    │
                                          └───────────────────────────────┘
```

* **UI vẽ ở đâu?** -> Vẽ bằng các file **XML** trong thư mục `res/layout/`.
* **Logic viết ở đâu?** -> Viết bằng code **Kotlin (`.kt`)** trong thư mục `ui/`, `data/`, `adapter/`.
* **Kết nối bằng cách nào?** -> Dự án dùng **ViewBinding**. Mỗi file `activity_main.xml` sẽ tự động sinh ra một class `ActivityMainBinding` bên Kotlin để bạn gọi trực tiếp ID của nút bấm, ô chữ mà không cần `findViewById`.

---

## 2. BẢN ĐỒ TRA CỨU NHANH: "MUỐN CODE TÍNH NĂNG X THÌ VÀO FILE NÀO?"

| Bạn muốn làm gì? | Code Giao diện (UI) ở file nào? | Code Logic / Chức năng ở file nào? |
| :--- | :--- | :--- |
| **Sửa Màn hình chính** (danh sách sách, tìm kiếm, lọc thể loại/trạng thái) | `res/layout/activity_main.xml`<br>`res/layout/item_book.xml` (từng thẻ sách) | `ui/main/MainActivity.kt`<br>`adapter/BookAdapter.kt` |
| **Sửa Chức năng Thêm / Chỉnh sửa sách** (chọn ảnh bìa, nhập tên, tác giả, trạng thái, thể loại) | `res/layout/activity_add_edit_book.xml` | `ui/addedit/AddEditBookActivity.kt`<br>`utils/ImageStorageHelper.kt` (xử lý ảnh) |
| **Sửa Màn hình Chi tiết sách** (thông tin sách, thanh tiến độ đọc, danh sách trích dẫn) | `res/layout/activity_book_detail.xml`<br>`res/layout/item_quote.xml` (từng trích dẫn) | `ui/detail/BookDetailActivity.kt`<br>`adapter/QuoteAdapter.kt` |
| **Sửa Chức năng Trích dẫn & Chia sẻ ảnh** (thêm quote, chia sẻ ảnh quote đẹp) | `res/layout/dialog_add_quote.xml`<br>`res/layout/item_quote_editor.xml` | `utils/ShareUtils.kt` (tạo ảnh chia sẻ)<br>`data/QuoteRepository.kt` |
| **Sửa Màn hình Bộ sưu tập (Collections)** (tạo bộ sưu tập, chọn sách đưa vào bộ sưu tập) | `res/layout/activity_collections.xml`<br>`res/layout/activity_collection_detail.xml`<br>`res/layout/dialog_create_collection.xml` | `ui/collections/CollectionsActivity.kt`<br>`ui/collections/CollectionDetailActivity.kt`<br>`ui/collections/CreateCollectionDialog.kt`<br>`adapter/CollectionAdapter.kt` |
| **Sửa Màn hình Thống kê** (biểu đồ tròn thể loại, đếm số trang, tỉ lệ hoàn thành) | `res/layout/activity_statistics.xml`<br>`res/layout/item_category_stat.xml` | `ui/stats/StatisticsActivity.kt`<br>`ui/stats/CategoryDonutView.kt` (vẽ biểu đồ tròn) |
| **Sửa Màn hình Cài đặt** (chế độ sáng/tối, đổi tên/ảnh hồ sơ, đổi ngôn ngữ Anh/Việt) | `res/layout/activity_settings.xml`<br>`res/layout/dialog_edit_profile.xml` | `ui/settings/SettingsActivity.kt`<br>`utils/PreferenceManager.kt`<br>`utils/LocaleHelper.kt` |
| **Sửa Chức năng Thông báo nhắc đọc sách** | Trong `activity_settings.xml` | `utils/ReadingReminderHelper.kt`<br>`utils/ReadingReminderReceiver.kt` |
| **Sửa/Thêm Bảng hoặc Cột Database (SQLite)** | Không có UI (dữ liệu ngầm) | `data/DatabaseHelper.kt`<br>`data/*Repository.kt` |

---

## 3. CẤU TRÚC CHI TIẾT TỪNG THƯ MỤC VÀ Ý NGHĨA TỪNG FILE

### A. Thư mục `res/` (Tài nguyên & Giao diện XML)

Nơi quy định **hình dáng, màu sắc, ngôn ngữ và icon** của toàn bộ app.

#### 1. `res/layout/` (Các file thiết kế màn hình và item)
* **Màn hình chính & Quản lý sách:**
  * `activity_main.xml`: Giao diện trang chủ (thanh tìm kiếm, bộ lọc chip thể loại/trạng thái, danh sách sách).
  * `item_book.xml`: Thiết kế cho **1 thẻ sách** hiển thị trong danh sách (ảnh bìa, tiêu đề, tác giả, tag trạng thái, số sao).
  * `activity_add_edit_book.xml`: Giao diện form nhập liệu khi thêm sách mới hoặc sửa sách.
  * `activity_book_detail.xml`: Giao diện xem chi tiết một cuốn sách (ảnh lớn, tóm tắt, đánh giá, danh sách quote).
* **Trích dẫn (Quotes):**
  * `item_quote.xml`: Thiết kế cho 1 trích dẫn hiển thị trong màn hình chi tiết sách.
  * `item_quote_editor.xml`: Khung giao diện dùng để "chụp ảnh trích dẫn" kèm bìa sách để chia sẻ lên Facebook/Zalo.
  * `dialog_add_quote.xml`: Hộp thoại popup khi bấm "Thêm trích dẫn".
* **Bộ sưu tập (Collections):**
  * `activity_collections.xml`: Giao diện danh sách các bộ sưu tập đã tạo.
  * `item_collection.xml`: Thiết kế cho 1 thẻ thư mục bộ sưu tập.
  * `activity_collection_detail.xml`: Giao diện bên trong 1 bộ sưu tập cụ thể.
  * `item_collection_detail_book.xml`: Thẻ sách nằm trong bộ sưu tập.
  * `item_collection_book_picker.xml`: Danh sách sách kèm checkbox để người dùng chọn sách cho vào bộ sưu tập.
  * `dialog_create_collection.xml`: Popup nhập tên và mô tả bộ sưu tập mới.
* **Thống kê & Cài đặt:**
  * `activity_statistics.xml`: Màn hình thống kê đọc sách (tổng số sách, số trang, thanh tiến độ, biểu đồ).
  * `item_category_stat.xml`: 1 dòng thống kê thể loại (tên thể loại, số lượng, thanh phần trăm).
  * `activity_settings.xml`: Màn hình cài đặt (Dark Mode, hẹn giờ nhắc nhở, chọn ngôn ngữ, tài khoản).
  * `dialog_edit_profile.xml`: Popup sửa tên và ảnh đại diện của người dùng.
* **Dialog dùng chung:**
  * `dialog_custom_alert.xml`: Hộp thoại thông báo chung (thay thế AlertDialog mặc định).
  * `dialog_custom_selection.xml`: Hộp thoại chọn thể loại / trạng thái dạng danh sách.
  * `item_dialog_option.xml`: 1 dòng lựa chọn trong popup.
  * `dialog_delete_book.xml`: Hộp thoại xác nhận xóa sách.

#### 2. `res/values/`, `res/values-en/`, `res/values-night/` (Giá trị dùng chung)
* `colors.xml`: Bảng mã màu của app (màu chủ đạo Navy, màu phụ Oak, màu nền, màu chữ,...).
* `values-night/colors.xml`: Bảng mã màu dành riêng cho **Chế độ Ban đêm (Dark Mode)**.
* `strings.xml`: Toàn bộ văn bản hiển thị trong app bằng **Tiếng Việt**.
* `values-en/strings.xml`: Toàn bộ văn bản hiển thị trong app bằng **Tiếng Anh**. Khi đổi ngôn ngữ, app sẽ tự động trỏ vào file này.
* `themes.xml` & `values-night/themes.xml`: Cấu hình theme Material 3 cho chế độ sáng và tối.

#### 3. `res/drawable/`
* Chứa toàn bộ các icon vector `.xml` (icon tìm kiếm, icon camera, ngôi sao đánh giá, lá cờ Việt/Anh,...) và các hình nền bo góc, thanh tiến độ (`progress_reading_detail.xml`,...).

---

### B. Thư mục `java/.../ui/` (Màn hình & Xử lý sự kiện)

Nơi chứa các **Activity** (mỗi Activity đại diện cho một màn hình) và các **Dialog**:

* **`ui/main/MainActivity.kt`**: 
  * Quản lý màn hình chính.
  * Xử lý: Tìm kiếm theo từ khóa, lọc theo thể loại/trạng thái, sắp xếp theo tên/ngày, bấm FAB (+) để thêm sách, mở menu chuyển sang Thống kê, Bộ sưu tập, Cài đặt.
* **`ui/addedit/AddEditBookActivity.kt`**:
  * Xử lý thêm mới hoặc chỉnh sửa sách.
  * Xử lý: Chọn ảnh từ thư viện/chụp ảnh, kiểm tra tính hợp lệ dữ liệu (validate), lưu sách vào Database thông qua `BookRepository`.
* **`ui/detail/BookDetailActivity.kt`**:
  * Hiển thị đầy đủ thông tin sách, cập nhật tiến độ đọc (trang hiện tại / tổng trang).
  * Hiển thị danh sách trích dẫn, bấm nút chia sẻ trích dẫn, bấm nút sửa/xóa sách.
* **`ui/collections/`**:
  * `CollectionsActivity.kt`: Quản lý danh sách các bộ sưu tập, mở dialog tạo mới.
  * `CollectionDetailActivity.kt`: Xem các sách trong một bộ sưu tập, thêm sách vào hoặc xóa sách ra.
  * `CreateCollectionDialog.kt`: Xử lý logic popup tạo bộ sưu tập mới.
* **`ui/stats/`**:
  * `StatisticsActivity.kt`: Lấy dữ liệu từ database, tính toán tổng số sách đã đọc, đang đọc, chưa đọc, tổng số trang, và tỉ lệ thể loại.
  * `CategoryDonutView.kt`: Một Custom View tự vẽ bằng Canvas của Android để tạo biểu đồ tròn (Donut Chart) đẹp mắt hiển thị phân bổ thể loại.
* **`ui/settings/SettingsActivity.kt`**:
  * Xử lý bật/tắt Dark Mode (thông qua `AppCompatDelegate`).
  * Xử lý đổi ngôn ngữ tiếng Việt / tiếng Anh.
  * Xử lý bật/tắt và chọn giờ thông báo nhắc đọc sách hàng ngày.
  * Xử lý sửa tên và avatar của người dùng.

---

### C. Thư mục `java/.../adapter/` (Cầu nối danh sách RecyclerView)

Khi hiển thị danh sách (như danh sách sách, danh sách quote), Android dùng `RecyclerView`. Các file **Adapter** có nhiệm vụ lấy dữ liệu từ `List` và đổ vào từng thẻ XML tương ứng:

* **`BookAdapter.kt`**: Đổ danh sách `Book` vào file `item_book.xml`. Bắt sự kiện người dùng bấm vào một cuốn sách để mở màn hình chi tiết.
* **`QuoteAdapter.kt`**: Đổ danh sách trích dẫn `Quote` vào file `item_quote.xml`. Xử lý nút xóa trích dẫn hoặc nút chia sẻ ảnh.
* **`CollectionAdapter.kt`**: Đổ danh sách `BookCollection` vào `item_collection.xml`.
* **`CollectionDetailBookAdapter.kt`**: Hiển thị danh sách sách nằm trong chi tiết bộ sưu tập kèm nút xóa khỏi bộ sưu tập.
* **`BookPickerAdapter.kt`**: Hiển thị danh sách sách kèm Checkbox để người dùng tích chọn khi muốn thêm sách vào bộ sưu tập.

---

### D. Thư mục `java/.../data/` (Cơ sở dữ liệu SQLite & Xử lý dữ liệu)

Nơi giao tiếp trực tiếp với cơ sở dữ liệu SQLite cục bộ trên điện thoại:

* **`DatabaseHelper.kt`**:
  * File quan trọng nhất của DB. Chứa câu lệnh `CREATE TABLE` tạo các bảng: `books`, `quotes`, `collections`, `collection_books`.
  * Quản lý phiên bản DB (`DATABASE_VERSION`), xử lý nâng cấp bảng khi thay đổi cấu trúc (`onUpgrade`).
* **`BookRepository.kt`**:
  * Chứa các hàm nghiệp vụ của Sách: `insertBook()`, `updateBook()`, `deleteBook()`, `getAllBooks()`, `searchBooks()`, `filterBooks()`, `getBookById()`.
* **`QuoteRepository.kt`**:
  * Chứa các hàm nghiệp vụ của Trích dẫn: `insertQuote()`, `getQuotesByBookId()`, `deleteQuote()`.
* **`CollectionRepository.kt`**:
  * Chứa các hàm nghiệp vụ của Bộ sưu tập: tạo bộ sưu tập, thêm sách vào bộ sưu tập, xóa sách khỏi bộ sưu tập, lấy sách theo bộ sưu tập.

---

### E. Thư mục `java/.../model/` (Khuôn mẫu dữ liệu - Data Classes)

Nơi định nghĩa cấu trúc của đối tượng dữ liệu trong code:

* **`Book.kt`**: Định nghĩa đối tượng Sách (`id`, `title`, `author`, `category`, `status`, `rating`, `coverImageUri`, `totalPages`, `currentPage`, `notes`,...).
* **`Quote.kt`**: Định nghĩa đối tượng Trích dẫn (`id`, `bookId`, `quoteText`, `pageNumber`, `createdDate`).
* **`BookCollection.kt`**: Định nghĩa đối tượng Bộ sưu tập (`id`, `name`, `description`, `bookCount`, `coverImageUri`).

---

### F. Thư mục `java/.../utils/` (Tiện ích hỗ trợ dùng chung)

Nơi chứa các hàm helper độc lập giúp code trong Activity gọn gàng:

* **`PreferenceManager.kt`**: Quản lý lưu trữ cài đặt bằng `SharedPreferences` (lưu trạng thái Dark Mode, ngôn ngữ hiện tại, giờ hẹn thông báo, tên và ảnh avatar người dùng).
* **`LocaleHelper.kt`**: Tiện ích chuyển đổi ngôn ngữ của toàn bộ ứng dụng (`vi` hoặc `en`) trong thời gian thực mà không cần khởi động lại máy.
* **`ReadingReminderHelper.kt`** & **`ReadingReminderReceiver.kt`**: Dùng `AlarmManager` và `NotificationManager` để đặt lịch hẹn giờ và bắn thông báo nhắc người dùng đọc sách mỗi ngày.
* **`ImageStorageHelper.kt`**: Nhận ảnh người dùng chọn từ bộ sưu tập máy, copy vào thư mục riêng của app và trả về đường dẫn an toàn để lưu vào Database.
* **`ShareUtils.kt`**: Nhận nội dung trích dẫn và thông tin sách, vẽ thành một tấm ảnh Bitmap đẹp mắt rồi mở menu Share của Android (qua `FileProvider`).
* **`NavigationHelper.kt`**: Hỗ trợ chuyển hướng mượt mà giữa các màn hình.
* **`Constants.kt`**: Chứa các hằng số dùng chung trong toàn bộ app.

---

### G. Các file cấu hình cấp ứng dụng (Manifests & Gradle)

* **`AndroidManifest.xml`** (`app/src/main/`):
  * "Giấy khai sinh" của app.
  * Khai báo quyền (truy cập bộ nhớ, báo thức, thông báo).
  * Khai báo tất cả các `Activity` và `Receiver` (màn hình nào muốn mở được thì bắt buộc phải khai báo ở đây).
  * Đặt `MainActivity` làm màn hình mở đầu (`LAUNCHER`).
* **`build.gradle.kts (Module :app)`**:
  * Khai báo phiên bản SDK (`compileSdk = 35`, `minSdk = 24`).
  * Khai báo các thư viện phụ thuộc (`RecyclerView`, `Material Components`, `ViewBinding`,...).
* **`gradle.properties`**:
  * Cấu hình môi trường build của Gradle (đã được cấu hình trỏ đúng JDK của Android Studio).

---

## 4. QUY TRÌNH 4 BƯỚC KHI BẠN MUỐN CODE THÊM MỘT TÍNH NĂNG MỚI

Khi bạn muốn thêm một tính năng (Ví dụ: *"Thêm màn hình Tác giả yêu thích"* hoặc *"Thêm trường Nhà xuất bản cho sách"*), hãy làm theo 4 bước sau:

```
Bước 1: Sửa/Thêm Dữ liệu  ──►  Bước 2: Vẽ Giao diện (XML)  ──►  Bước 3: Khai báo Manifest  ──►  Bước 4: Viết Code Logic (Kotlin)
(Model & DatabaseHelper)      (res/layout/*.xml)               (AndroidManifest.xml)        (Activity / Adapter / Repository)
```

1. **Bước 1: Chuẩn bị Dữ liệu**
   - Vào `model/` thêm thuộc tính mới vào Data Class.
   - Nếu cần lưu vào DB: Vào `data/DatabaseHelper.kt` thêm cột hoặc bảng mới, rồi vào `data/*Repository.kt` viết hàm thêm/đọc dữ liệu.
2. **Bước 2: Thiết kế Giao diện**
   - Vào `res/layout/` tạo file XML mới (hoặc sửa file XML có sẵn). Đặt `android:id` rõ ràng cho các nút và ô chữ cần tương tác.
3. **Bước 3: Đăng ký Màn hình**
   - Nếu bạn tạo một màn hình mới (`NewActivity`), mở file `AndroidManifest.xml` và thêm `<activity android:name=".ui.yourpackage.NewActivity" />`.
4. **Bước 4: Viết Code Logic**
   - Tạo file Kotlin trong thư mục `ui/`.
   - Kết nối với giao diện thông qua ViewBinding.
   - Bắt sự kiện bấm nút (`setOnClickListener`), gọi Repository để lưu/đọc dữ liệu và cập nhật giao diện.

---
*Tài liệu được cập nhật và tổng hợp tự động cho dự án **BookNest**.*
