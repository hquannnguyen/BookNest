# 📚 BÁO CÁO KIỂM TOÁN & ĐÁNH GIÁ HIỆN TRẠNG HỆ THỐNG BOOKNEST
*(Ứng dụng Quản lý Tủ sách Cá nhân & Không gian Đọc sách Chiêm nghiệm)*

---

- **Dự án**: BookNest (`QUANLYSACHCANHAN`)
- **Nền tảng**: Android Native (Kotlin)
- **Kiến trúc**: Repository Pattern, Material 3 Design System, SQLite Database (Schema v4)
- **Ngày lập báo cáo**: 08/09/2026
- **Trạng thái biên dịch**: ✅ **BUILD SUCCESSFUL** (0 errors)

---

## MỤC LỤC

1. [TỔNG QUAN KIẾN TRÚC HỆ THỐNG](#1-tổng-quan-kiến-trúc-hệ-thống)
2. [CÁC PHÂN HỆ ĐÃ XÂY DỰNG HOÀN THIỆN (ĐỒNG BỘ 100% FRONTEND & BACKEND)](#2-các-phân-hệ-đã-xây-dựng-hoàn-thiện-đồng-bộ-100-frontend--backend)
3. [MA TRẬN ĐỐI CHIẾU TÍNH NĂNG (FEATURE COMPATIBILITY MATRIX)](#3-ma-trận-đối-chiếu-tính-năng-feature-compatibility-matrix)
4. [NHỮNG PHẦN FRONTEND CÓ MÀ BACKEND CHƯA ĐÁP ỨNG (MOCK / PLACEHOLDER)](#4-những-phần-frontend-có-mà-backend-chưa-đáp-ứng-mock--placeholder)
5. [NHỮNG PHẦN BACKEND ĐÃ CÓ MÀ FRONTEND CHƯA KHAI THÁC HẾT](#5-những-phần-backend-đã-có-mà-frontend-chưa-khai-thác-hết)
6. [NHỮNG TÍNH NĂNG CÒN THIẾU CỦA HỆ THỐNG QUẢN LÝ SÁCH TOÀN DIỆN](#6-những-tính-năng-còn-thiếu-của-hệ-thống-quản-lý-sách-toàn-diện)
7. [ĐỀ XUẤT LỘ TRÌNH TRIỂN KHAI TIẾP THEO (ACTIONABLE ROADMAP)](#7-đề-xuất-lộ-trình-triển-khai-tiếp-theo-actionable-roadmap)

---

## 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG

### 1.1. Công nghệ cốt lõi
- **Ngôn ngữ**: Kotlin (100%), Coroutines-ready.
- **Giao diện**: XML Layouts với `ViewBinding`, tuân thủ chặt chẽ phong cách thiết kế **Literary Sanctuary / Material 3** (màu HSL sang trọng, bo tròn góc thẻ bento card, hỗ trợ tương thích Dark/Light Theme).
- **Cơ sở dữ liệu cục bộ**: `SQLiteOpenHelper` phiên bản **Database Version 4** với cấu trúc 4 bảng:
  - `books`: Quản lý thông tin sách, tiến độ trang đọc, rating, ảnh bìa, ngày tạo/cập nhật/hoàn thành.
  - `quotes`: Quản lý trích dẫn tâm đắc, liên kết khóa ngoại `FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE`.
  - `collections`: Quản lý danh mục các bộ sưu tập, tên, mô tả, mã màu nhận diện hex, tên biểu tượng vector.
  - `collection_books`: Bảng liên kết nhiều - nhiều (N - N) giữa bộ sưu tập và sách với khóa chính phức hợp `(collection_id, book_id)` và ràng buộc `ON DELETE CASCADE`.
- **Cấu hình & Tùy biến**: `SharedPreferences` thông qua lớp đóng gói `PreferenceManager`.
- **Hệ thống tác vụ ngầm**: `AlarmManager` kết hợp `BroadcastReceiver` và `NotificationChannel` phục vụ chức năng nhắc nhở đọc sách hàng ngày theo lịch hẹn.

---

## 2. CÁC PHÂN HỆ ĐÃ XÂY DỰNG HOÀN THIỆN (ĐỒNG BỘ 100% FRONTEND & BACKEND)

### 2.1. Màn hình Thư viện chính (`MainActivity`)
- **Frontend**:
  - Thanh tìm kiếm ghim cố định (Pinned Header) không bị đẩy mất khi cuộn danh sách.
  - Banner trích dẫn cảm hứng tự động xoay vòng sau mỗi **15 giây** với hiệu ứng chuyển cảnh trôi nổi (float & fade) mượt mà; hỗ trợ chạm vào banner để đổi câu trích dẫn tức thì.
  - Dải lọc 4 trạng thái đọc dạng pill (*Tất cả, Chưa đọc, Đang đọc, Đã đọc*).
  - Lưới hiển thị danh sách sách 2 cột chuẩn thiết kế Stitch hiện đại.
  - Hộp thoại chọn thể loại lọc và hộp thoại sắp xếp (*Tựa đề, Tác giả, Đánh giá sao, Trạng thái*).
  - Thanh điều hướng đáy (Bottom Navigation) đồng bộ 4 tab: *Thư viện, Bộ sưu tập, Thống kê, Cài đặt*.
  - Header tích hợp nút chuyển đổi nhanh ngôn ngữ (VI/EN) và chuông thông báo nhắc nhở kèm chấm trạng thái.
- **Backend**:
  - `BookRepository.getBooks()`: Xử lý tìm kiếm `LIKE` theo cả tiêu đề và tác giả, kết hợp bộ lọc trạng thái, thể loại và câu lệnh `ORDER BY` tối ưu chỉ mục (Index).
  - `PreferenceManager`: Lưu trữ câu trích dẫn banner hiện tại, thời điểm trích dẫn và cấu hình sắp xếp mặc định.

### 2.2. Màn hình Thêm / Chỉnh sửa sách (`AddEditBookActivity`)
- **Frontend**:
  - Khung tải ảnh bìa tỉ lệ 2:3 chuẩn bìa sách (chọn ảnh từ thư viện thiết bị).
  - Nhập liệu đầy đủ: Tiêu đề, Tác giả, Thể loại (Spinner danh mục chuẩn), Trạng thái đọc (Spinner).
  - Hệ thống chấm điểm **5 trái tim** trực quan.
  - Ô nhập số trang hiện tại (`currentPage`) và tổng số trang (`totalPages`).
  - Trình soạn thảo trích dẫn động: Cho phép thêm / xóa nhiều trích dẫn trực tiếp (Nội dung, Trang sách, Chương sách, Tag thể loại).
  - Ô ghi chú cá nhân (Personal Notes) và nút "Thêm sách / Lưu thay đổi" ghim cố định ở đáy.
- **Backend**:
  - `ImageStorageHelper`: Tự động sao chép tệp ảnh vào thư mục riêng tư của ứng dụng (`App Internal Storage`), đảm bảo ảnh bìa không bị mất khi ảnh gốc trong máy bị xóa.
  - `BookRepository.insert()` / `update()`: Lưu toàn vẹn dữ liệu, tự động đồng bộ `currentPage = totalPages` khi đánh dấu "Đã đọc", tự động ghi nhận thời điểm hoàn thành `finishedAt`.
  - `QuoteRepository`: Thêm/cập nhật danh sách trích dẫn đồng bộ theo `bookId`.

### 2.3. Màn hình Chi tiết tác phẩm (`BookDetailActivity`)
- **Frontend**:
  - Trưng bày bìa sách khổ lớn kèm ruy-băng đánh dấu trang (Ribbon Bookmark).
  - Hiển thị đầy đủ điểm đánh giá trái tim, huy hiệu trạng thái, số trang và thanh tiến độ đọc.
  - Hai tab chuyển đổi mượt mà: **"Trích dẫn tâm đắc"** và **"Ghi chú cá nhân"**.
  - Thao tác xóa từng câu trích dẫn trực tiếp trong danh sách.
  - Nút chia sẻ cá nhân từng trích dẫn hoặc chia sẻ toàn bộ tác phẩm (kèm danh sách trích dẫn) qua `Intent.ACTION_SEND`.
  - Nút sửa bìa nhanh, nút chỉnh sửa toàn diện và nút xóa sách kèm hộp thoại xác nhận an toàn.
- **Backend**:
  - `BookRepository.getById()` & `delete()`: Xóa sách sẽ tự động kích hoạt `ON DELETE CASCADE` trong SQLite xóa sạch các trích dẫn liên quan, dọn rác cơ sở dữ liệu.
  - `QuoteRepository.getByBookId()` & `delete()`: Quản lý trích dẫn thời gian thực.

### 2.4. Màn hình Danh sách Bộ sưu tập (`CollectionsActivity`)
- **Frontend**:
  - Thẻ bento card cho từng bộ sưu tập với biểu tượng chủ đề và màu sắc nhận diện riêng.
  - Hiệu ứng bìa sách xếp chồng (Overlapping book covers) trực quan.
  - Thanh tiến độ % hoàn thành các đầu sách trong từng bộ sưu tập.
  - Thẻ thống kê tổng quan: Tổng số bộ sưu tập và tổng số sách trong các bộ.
  - Bộ lọc chip: *Tất cả, Đang đọc, Đã xong*.
  - Nút FAB tạo bộ sưu tập mới và menu thao tác nhanh (*Chỉnh sửa, Xóa*).
- **Backend**:
  - `CollectionRepository.getAllWithStats()`: Truy vấn danh sách bộ sưu tập kết hợp tính toán số lượng sách `READ`, `READING`, `UNREAD` và danh sách URL/path ảnh bìa.
  - Cơ chế tự động gieo dữ liệu mẫu (`seedSampleCollectionsIfEmpty`) khi người dùng mới cài đặt ứng dụng.

### 2.5. Hộp thoại Tạo & Sửa Bộ sưu tập (`CreateCollectionDialog`)
- **Frontend**:
  - Nhập tên và mô tả bộ sưu tập.
  - Bảng chọn 6 tông màu nhận diện trầm/ấm sang trọng.
  - Bộ chọn 6 icon vector chủ đề văn học.
  - Bộ chọn sách (Book Picker) tích hợp ô tìm kiếm riêng, các chip lọc trạng thái và danh sách cuộn độc lập.
  - Xử lý bàn phím thông minh (`WindowInsets`), không che khuất ô nhập liệu.
  - Nút bấm tạo / cập nhật ngang dài (Full-width action button) nổi bật ở đáy.
- **Backend**:
  - `CollectionRepository.insert()` / `update()`: Sử dụng SQLite Transaction (`beginTransaction`, `setTransactionSuccessful`, `endTransaction`) đảm bảo tính toàn vẹn dữ liệu khi ghi vào 2 bảng `collections` và `collection_books`.

### 2.6. Màn hình Chi tiết Bộ sưu tập (`CollectionDetailActivity`)
- **Frontend**:
  - Hero Card ấn tượng hiển thị màu sắc và icon chủ đạo của bộ sưu tập.
  - 3 chỉ số then chốt: **Tổng số sách**, **Đã xong**, **Khối lượng trang (tr)** kèm thanh tiến độ %.
  - Thẻ câu trích dẫn tâm đắc nổi bật nhất trong bộ sưu tập (tự động trích xuất từ các tác phẩm thuộc bộ).
  - Dải 4 pill lọc sách: *Tất cả, Đang đọc, Đã xong, Chưa đọc*.
  - Popup sắp xếp đa dạng: *Tiến độ đọc, Tựa đề (A-Z), Đánh giá sao, Khối lượng trang*.
  - Danh sách thẻ sách chi tiết: Hiển thị rating, số trang, số lượng trích dẫn, tùy chọn "Bỏ khỏi bộ sưu tập" hoặc "Xem chi tiết sách".
  - Nút "+ Thêm sách vào bộ", chức năng chia sẻ danh sách các tác phẩm trong bộ.
- **Backend**:
  - `CollectionRepository.getBooksInCollection()`: Truy vấn INNER JOIN giữa `collection_books` và `books`.
  - `QuoteRepository.getRandomQuoteForBookIds()`: Lấy ngẫu nhiên câu trích dẫn của riêng các đầu sách nằm trong bộ sưu tập.
  - `QuoteRepository.getQuoteCountPerBook()`: Đếm số lượng trích dẫn của từng cuốn sách phục vụ badge hiển thị.
  - `CollectionRepository.removeBookFromCollection()`: Bỏ liên kết sách mà không xóa mất sách trong kho chính.

### 2.7. Màn hình Thống kê tủ sách (`StatisticsActivity`)
- **Frontend**:
  - Hàng 3 thẻ chỉ số chính: Tổng số sách, Sách đã đọc xong, Sách đang đọc gối đầu giường.
  - Biểu đồ tròn Donut Chart vẽ bằng Canvas tùy biến (`CategoryDonutView`) hiển thị tỷ lệ các thể loại.
  - Bảng chú giải phân bổ thể loại động kèm số cuốn và phần trăm chính xác.
  - Thẻ làm nổi bật thể loại được độc giả yêu thích nhất kèm lời bình cảm hứng.
  - Phân bổ trạng thái đọc qua 3 thanh tiến độ trực quan.
  - Thẻ Bento "Trích dẫn & Cảm hứng" ngẫu nhiên từ kho tàng cá nhân.
- **Backend**:
  - `BookRepository.getCategoryCounts()`, `countAll()`, `countByStatus()`.
  - `QuoteRepository.getRandomQuote()`, `countAll()`.

### 2.8. Màn hình Cài đặt & Cá nhân hóa (`SettingsActivity`)
- **Frontend**:
  - Hồ sơ độc giả: Đổi tên, email / châm ngôn sống, cập nhật hoặc xóa ảnh đại diện.
  - Giao diện ban đêm: Bật/tắt Dark Mode tức thì qua `AppCompatDelegate`.
  - Cài đặt đa ngôn ngữ: Chuyển đổi Tiếng Việt / Tiếng Anh thời gian thực.
  - Nhắc nhở đọc sách: Bật/tắt và chọn giờ nhắc nhở qua `TimePickerDialog`.
  - Tùy chọn kiểu sắp xếp mặc định của toàn ứng dụng.
  - Thông tin giới thiệu ứng dụng BookNest và Chính sách quyền riêng tư.
- **Backend**:
  - `PreferenceManager`: Lưu trữ toàn bộ trạng thái cấu hình.
  - `ReadingReminderHelper` & `ReadingReminderReceiver`: Đăng ký lịch báo thức qua `AlarmManager` và phát thông báo qua `NotificationChannel`.

---

## 3. MA TRẬN ĐỐI CHIẾU TÍNH NĂNG (FEATURE COMPATIBILITY MATRIX)

| Tính năng | Giao diện (Frontend) | Nghiệp vụ / Dữ liệu (Backend) | Mức độ đồng bộ |
| :--- | :---: | :---: | :---: |
| Tìm kiếm sách thời gian thực | Có (Ghim Header) | Có (`LIKE %query%`) | 🟢 100% Hoàn hảo |
| Xoay vòng trích dẫn Banner (15s) | Có (Animation) | Có (`SharedPreferences` Cache) | 🟢 100% Hoàn hảo |
| CRUD Sách (Thêm, Sửa, Xóa, Chi tiết) | Có | Có (`books` Table) | 🟢 100% Hoàn hảo |
| Chấm điểm 5 Trái Tim | Có | Có (`rating` FLOAT) | 🟢 100% Hoàn hảo |
| Quản lý Trích dẫn động đa trường | Có | Có (`quotes` Table, FK Cascade) | 🟢 100% Hoàn hảo |
| Tạo / Sửa Bộ sưu tập & Chọn sách | Có (Picker + Filter) | Có (`collections` & `collection_books`) | 🟢 100% Hoàn hảo |
| Biểu đồ Donut Chart Thống kê | Có (Custom View) | Có (`GROUP BY category`) | 🟢 100% Hoàn hảo |
| Hẹn giờ nhắc nhở đọc sách | Có (TimePicker) | Có (`AlarmManager` + `Receiver`) | 🟢 100% Hoàn hảo |
| Chuyển đổi Dark / Light Theme | Có | Có (`night_mode` Pref) | 🟢 100% Hoàn hảo |
| Đa ngôn ngữ (VI / EN) | Có | Có (`LocaleHelper`) | 🟢 100% Hoàn hảo |
| **Đồng bộ hóa dữ liệu đám mây** | **Có (Button & Dialog)** | **Chưa có (Chỉ lưu timestamp)** | 🟡 **Mock UI** |
| **Sao lưu dữ liệu ra file** | **Có (Button & Dialog)** | **Chưa có (Chưa xuất file thực tế)** | 🟡 **Mock UI** |
| **Chia sẻ Bộ sưu tập dạng PDF/CSV** | **Có (Menu Share)** | **Chỉ có Plain Text qua Intent** | 🟡 **Cơ bản** |
| **Thống kê sách đọc trong năm** | **Chưa có UI hiển thị** | **Đã có hàm `countReadBooksInYear`** | 🔵 **Backend dư thừa** |
| **Lọc trích dẫn theo Thẻ / Tag** | **Chưa có UI bộ lọc Tag** | **Đã lưu trường `tag`, `chapter`** | 🔵 **Backend dư thừa** |

---

## 4. NHỮNG PHẦN FRONTEND CÓ MÀ BACKEND CHƯA ĐÁP ỨNG (MOCK / PLACEHOLDER)

Dưới đây là các vị trí trên giao diện người dùng nhìn thấy các nút chức năng hoặc hộp thoại thông báo, nhưng mã nguồn xử lý thực tế phía sau chỉ là mô phỏng tạm thời:

### 4.1. Nút "Đồng bộ hóa dữ liệu" (`btnSyncData`) trong Cài đặt
- **Thực tế trên giao diện**: Người dùng chạm vào hàng "Đồng bộ hóa", ứng dụng mở hộp thoại thông báo: *"Đồng bộ thành công • Tổng số sách: X • Cơ sở dữ liệu: SQLite Offline Encrypted"*.
- **Hạn chế Backend**: Chưa tích hợp dịch vụ điện toán đám mây thực tế (chưa có Firebase, Supabase hay Google Drive API). Hiện tại chỉ cập nhật một biến thời gian cục bộ `preferenceManager.lastSyncTime = System.currentTimeMillis()`.

### 4.2. Nút "Sao lưu dữ liệu" (`btnBackupData`) trong Cài đặt
- **Thực tế trên giao diện**: Người dùng chạm vào nút "Sao lưu", ứng dụng mở thông báo: *"Đã lưu trữ toàn bộ X tác phẩm và các trích dẫn yêu thích vào bộ nhớ an toàn của thiết bị"*.
- **Hạn chế Backend**: Chưa có module xuất tệp cơ sở dữ liệu `personal_books.db` hoặc xuất cấu trúc JSON/CSV ra thư mục công cộng (`Downloads` / `Documents`) của thiết bị, và chưa có cơ chế **Phục hồi (Restore)** để nạp lại dữ liệu khi đổi điện thoại.

### 4.3. Nút "Đặt lại cài đặt" (`btnResetSettings`)
- **Thực tế trên giao diện**: Hộp thoại cảnh báo đặt lại ứng dụng về trạng thái ban đầu.
- **Hạn chế Backend**: Hiện tại thao tác này mới chỉ đưa các biến trong `SharedPreferences` về giá trị mặc định (theme, tên, email), chưa có lựa chọn xóa trắng toàn bộ dữ liệu SQLite (Factory Reset Database) nếu người dùng muốn dọn sạch tủ sách để bắt đầu lại.

### 4.4. Xuất / Chia sẻ Bộ sưu tập dạng tài liệu chuyên nghiệp
- **Thực tế trên giao diện**: Trong menu tùy chọn của Bộ sưu tập có chức năng "Chia sẻ bộ sưu tập".
- **Hạn chế Backend**: Mới chỉ tạo một đoạn văn bản thô (Plain text) danh sách các đầu sách gửi sang ứng dụng khác qua `Intent.ACTION_SEND`, chưa có engine đồ họa kết xuất file PDF hoặc bảng tính CSV đính kèm.

---

## 5. NHỮNG PHẦN BACKEND ĐÃ CÓ MÀ FRONTEND CHƯA KHAI THÁC HẾT

Dưới đây là các trường dữ liệu và phương thức nghiệp vụ đã được định nghĩa, tính toán hoàn chỉnh trong tầng Data/Repository nhưng giao diện chưa đưa ra hiển thị cho người dùng:

### 5.1. Cột `finished_at` và hàm tính sách đọc theo năm
- **Backend sẵn sàng**: Bảng `books` có trường `finished_at INTEGER` tự động lưu timestamp chính xác khi sách chuyển sang trạng thái "Đã đọc". Trong `BookRepository.kt` đã viết sẵn hàm `countReadBooksInYear(year: Int): Int`.
- **Frontend chưa tận dụng**:
  - Màn hình Chi tiết sách (`BookDetailActivity`) chưa có mục hiển thị ngày hoàn thành (ví dụ: *"Đã đọc xong ngày 15/08/2026"*).
  - Màn hình Thống kê (`StatisticsActivity`) chưa đưa con số *"Số sách đã hoàn thành trong năm 2026"* lên giao diện để tạo động lực cho độc giả.

### 5.2. Cột `tag` và `chapter` của bảng `quotes`
- **Backend sẵn sàng**: Bảng `quotes` lưu trữ độc lập và đầy đủ 4 trường: `content`, `page`, `chapter`, `tag`.
- **Frontend chưa tận dụng**: Người dùng có thể nhập thẻ tag (ví dụ: `#triethoc`, `#tamly`) khi thêm sách, tuy nhiên ở màn hình Chi tiết sách và Thư viện chưa có bộ lọc trích dẫn theo Thẻ (Click vào tag để xem tất cả câu trích dẫn cùng chủ đề).

### 5.3. Thống kê tiến độ gộp các bộ sưu tập (`OverviewStats`)
- **Backend sẵn sàng**: `CollectionRepository.getOverviewStats()` đã tính toán sẵn: `totalCollections`, `totalBooksInCollections`, `readBooksInCollections` và `percent` (% hoàn thành gộp của toàn bộ các bộ sưu tập).
- **Frontend chưa tận dụng**: Màn hình `CollectionsActivity` mới chỉ hiển thị số lượng bộ và số lượng sách, chưa vẽ thanh phần trăm tiến độ tổng thể của toàn bộ các bộ sưu tập.

### 5.4. Cột `updated_at` của bảng `books`
- **Backend sẵn sàng**: Được tự động gán timestamp mỗi khi người dùng chỉnh sửa sách.
- **Frontend chưa tận dụng**: Màn hình Chi tiết sách chưa hiển thị dòng thông tin *"Chỉnh sửa lần cuối lúc..."*.

---

## 6. NHỮNG TÍNH NĂNG CÒN THIẾU CỦA HỆ THỐNG QUẢN LÝ SÁCH TOÀN DIỆN

So với các ứng dụng Reading Tracker cao cấp trên thị trường (Goodreads, StoryGraph, Bookly), hệ thống hiện tại còn có thể mở rộng các tính năng sau:

1. **Bộ đếm thời gian phiên đọc (Reading Stopwatch / Session Tracker)**:
   - Hiện tại: Người dùng chỉ cập nhật số trang thủ công.
   - Tiềm năng: Nút bấm "Bắt đầu đọc", đếm giờ khi ngồi đọc sách (ví dụ: đọc 30 phút, đọc được 25 trang) để tính tốc độ đọc trung bình (trang/phút).
2. **Thử thách & Mục tiêu đọc sách hàng năm (Yearly Reading Challenge)**:
   - Cho phép người dùng đặt mục tiêu (ví dụ: "Mục tiêu 2026: Đọc 25 cuốn sách") kèm thanh tiến độ hiển thị trực quan.
3. **Trung tâm Sổ tay Trích dẫn (Quotes Hub)**:
   - Hiện tại trích dẫn nằm rải rác bên trong từng cuốn sách.
   - Tiềm năng: Một tab hoặc màn hình tập hợp tất cả các câu trích dẫn của toàn bộ tủ sách, cho phép tìm kiếm theo từ khóa trích dẫn hoặc lướt xem dạng feed cảm hứng.
4. **Tìm kiếm toàn cục (Global Search)**:
   - Thanh tìm kiếm trang chủ mở rộng tìm xuyên suốt cả: Tên sách, Tác giả, Nội dung câu trích dẫn và Tên bộ sưu tập.
5. **Cơ chế Xuất / Nhập tệp sao lưu thực tế (Real JSON Export/Import)**:
   - Cho phép người dùng xuất file `booknest_backup_20260908.json` lưu về Google Drive hoặc máy tính, và nhập lại file này để phục hồi toàn bộ dữ liệu.

---

## 7. ĐỀ XUẤT LỘ TRÌNH TRIỂN KHAI TIẾP THEO (ACTIONABLE ROADMAP)

### Giai đoạn 1: Khai thác ngay tài nguyên Backend sẵn có (Ưu tiên cao - Thực hiện nhanh)
1. **Hiển thị ngày đọc xong**: Bổ sung hiển thị `finishedAt` định dạng `dd/MM/yyyy` trên màn hình [BookDetailActivity.kt](file:///d:/LapTrinhApp/QUANLYSACHCANHAN/app/src/main/java/com/example/quanlysachcanhan/ui/detail/BookDetailActivity.kt).
2. **Thống kê năm 2026**: Đưa chỉ số `countReadBooksInYear(currentYear)` vào màn hình [StatisticsActivity.kt](file:///d:/LapTrinhApp/QUANLYSACHCANHAN/app/src/main/java/com/example/quanlysachcanhan/ui/stats/StatisticsActivity.kt) dưới dạng card "Mục tiêu & Thành tích năm".
3. **Hiển thị Tag & Chương trích dẫn**: Cập nhật [item_quote.xml](file:///d:/LapTrinhApp/QUANLYSACHCANHAN/app/src/main/res/layout/item_quote.xml) để hiển thị thẻ chip `#tag` và vị trí chương sách đẹp mắt hơn.

### Giai đoạn 2: Hoàn thiện nghiệp vụ cho các nút UI hiện có (Ưu tiên trung bình)
1. **Sao lưu & Phục hồi tệp tin (Backup & Restore)**: Xây dựng trình xuất / nhập file JSON nén chứa toàn bộ sách, trích dẫn và bộ sưu tập trong [SettingsActivity.kt](file:///d:/LapTrinhApp/QUANLYSACHCANHAN/app/src/main/java/com/example/quanlysachcanhan/ui/settings/SettingsActivity.kt).
2. **Xóa trắng dữ liệu (Factory Reset)**: Bổ sung tùy chọn dọn sạch Database trong dialog Đặt lại cài đặt kèm xác nhận mật khẩu/mã ngẫu nhiên để tránh bấm nhầm.

### Giai đoạn 3: Tính năng nâng cao gia tăng trải nghiệm (Dài hạn)
1. Xây dựng màn hình **Kho trích dẫn tâm đắc (Quotes Sanctuary)** với bộ lọc thẻ cảm xúc.
2. Xây dựng tính năng **Bấm giờ đọc sách (Reading Timer)** và ghi nhật ký tiến độ từng ngày.

---
*Báo cáo được lập tự động dựa trên mã nguồn thực tế của dự án BookNest.*
