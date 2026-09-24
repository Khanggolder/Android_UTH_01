# Android_UTH_01 - Task Management App

Ứng dụng Android hỗ trợ quản lý công việc cá nhân, lưu dữ liệu trực tiếp trên thiết bị và có thể sử dụng khi không có kết nối mạng.

## Thành viên

| STT | MSSV | Họ và tên | Vai trò |
|---:|---|---|---|
| 1 | 083205012180 | Nguyễn Duy Khang(NT) | Cross-module Coding • Bug Fixing • Code Review |
| 2 | 066205002941 | Nguyễn Hoài Nam | Entity • DAO • Migration • Project Timeline |
| 3 | 077205003436 | Nguyễn Hoàng Minh Khôi | Task Form • CRUD • Validation |
| 4 | 080206016469 | Lê Trần Đăng Khôi | Filter • Sort • Empty / Overdue / Error |
| 5 | 049206013293 | Đặng Lam Trường | Notification • Recurring • Receiver |
| 6 | 079205031894 | Nguyễn Thành Đạt | Calendar • Security • Backup / Restore |

## Giới thiệu

Task Management App được xây dựng cho đề tài `ANDROID_UTH_01` của môn Lập trình thiết bị di động.

Ứng dụng tập trung vào các chức năng quản lý công việc cá nhân như tạo và cập nhật task, lọc và sắp xếp công việc, nhắc việc, lịch, timeline, khóa ứng dụng bằng PIN, file đính kèm và backup/restore dữ liệu.

Dữ liệu được lưu cục bộ trên thiết bị bằng Room. Phiên bản hiện tại không sử dụng backend, cloud hoặc hệ thống đăng nhập nhiều tài khoản.

## Kiến trúc hệ thống

- Kiến trúc: MVVM + Repository
- Giao diện: XML + ViewBinding
- Lưu trữ dữ liệu: Room Database
- Lưu cấu hình bảo mật: DataStore
- Quản lý trạng thái: ViewModel + StateFlow
- Nhắc việc: AlarmManager + BroadcastReceiver + NotificationManager
- File và backup: Storage Access Framework + FileProvider
- Mô hình hoạt động: Local/offline Android application
- Protocol: Không sử dụng
- Port mặc định: Không sử dụng
- Cấu trúc message: Không sử dụng

Room Database hiện ở **version 5**, gồm các entity chính:

- `TaskEntity`
- `UserEntity`
- `TaskAttachmentEntity`

Các migration hiện có:

- `1 -> 2`: thêm `startDateTime`
- `2 -> 3`: thêm bảng user, thông tin user của task và attachment
- `3 -> 4`: bổ sung thông tin quản lý file attachment
- `4 -> 5`: bổ sung ràng buộc giữa task và assignee, đồng thời bảo toàn dữ liệu cũ

## Công nghệ sử dụng

- Kotlin
- Android XML
- ViewBinding
- MVVM
- Repository Pattern
- Room
- DataStore
- StateFlow / LiveData
- AlarmManager
- BroadcastReceiver
- NotificationManager
- Storage Access Framework
- FileProvider
- JUnit / AndroidX Test / Espresso

Thông số project hiện tại:

- `minSdk`: 26
- `compileSdk`: 36
- `targetSdk`: 36
- Kotlin: 2.2.10
- Android Gradle Plugin: 9.3.1
- Gradle Wrapper: 9.5.0
- JVM target: 11

## Cấu trúc project

```text
Android_UTH_01/
├── Code/
│   └── TaskManagementApp/
├── DOCX/
├── Extra/
├── PPTX/
└── README.md
```

Một số package chính trong ứng dụng:

```text
com.uth.taskmanagement/
├── attachment/
├── backup/
├── core/
├── data/
│   ├── local/
│   ├── model/
│   └── repository/
├── notification/
├── recurrence/
├── security/
├── ui/
│   ├── attachment/
│   ├── calendar/
│   ├── settings/
│   ├── taskform/
│   ├── tasklist/
│   └── timeline/
└── utils/
```

## Yêu cầu môi trường

- Android Studio
- Android SDK
- Thiết bị thật hoặc Android Emulator từ API 26 trở lên
- JDK tương thích với Android Studio/Gradle của project
- Git

Khuyến nghị sử dụng JDK đi kèm Android Studio. Trong quá trình phát triển, nhóm sử dụng JDK 21 để chạy Gradle.

## Cài đặt

Clone repository:

```bash
git clone https://github.com/Khanggolder/Android_UTH_01.git
cd Android_UTH_01/Code/TaskManagementApp
```

Mở thư mục sau bằng Android Studio:

```text
Code/TaskManagementApp
```

Sau đó:

1. Chờ Android Studio hoàn tất Gradle Sync.
2. Chọn emulator hoặc thiết bị Android.
3. Chạy module `app`.

Project không yêu cầu cấu hình backend, IP hoặc port.

## Hướng dẫn chạy

### Android Studio

1. Chọn **Open**.
2. Mở thư mục `Code/TaskManagementApp`.
3. Chờ Gradle Sync hoàn tất.
4. Chọn thiết bị hoặc emulator.
5. Nhấn **Run**.

### Terminal

Windows:

```bat
cd Code\TaskManagementApp
gradlew.bat assembleDebug
```

macOS/Linux:

```bash
cd Code/TaskManagementApp
./gradlew assembleDebug
```

APK debug được tạo trong:

```text
Code/TaskManagementApp/app/build/outputs/apk/debug/
```

## Cấu hình và quyền

Ứng dụng không cần IP, port hoặc API key.

Một số quyền/chức năng hệ thống được sử dụng:

- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- Exact alarm
- Storage Access Framework
- FileProvider

## Chức năng

- [x] Tạo, sửa và xóa công việc
- [x] Đánh dấu công việc hoàn thành
- [x] Lưu title, description, start time, due time, priority và status
- [x] Lọc task theo status và priority
- [x] Sắp xếp task theo thời hạn
- [x] Hiển thị empty state, overdue state và error state
- [x] Nhắc việc bằng alarm và notification
- [x] Recurring reminder theo ngày, tuần và tháng
- [x] Khôi phục reminder sau reboot hoặc thay đổi thời gian/timezone
- [x] Calendar theo ngày
- [x] Project Timeline
- [x] PIN lock
- [x] Không lưu PIN dạng plaintext
- [x] File đính kèm cho task
- [x] Export/restore JSON
- [x] Portable backup bằng ZIP kèm attachment
- [x] Room Database migration
- [x] Chuẩn bị data model cho user/assignee

Phần user/assignee hiện mới ở mức data model và repository, chưa phải hệ thống multi-user hoàn chỉnh.

## Kiểm thử

Bộ kiểm thử hiện tại có **56 test case**, từ `TC-01` đến `TC-56`.

Các nhóm kiểm thử chính gồm:

- CRUD và validation
- Filter, sort và UI state
- Notification và recurring reminder
- Room persistence và migration
- Calendar
- Timeline
- PIN security
- Backup/restore
- Attachment
- Integration và regression
- Performance

Trong bảng tổng hợp testcase hiện tại, cả 56 testcase đều đã được ghi nhận `Pass` ở 3 lần chạy.

Ngoài manual test, repository cũng có unit test và instrumented test cho Room, migration, backup, attachment và một số business rule chính.

Bằng chứng kiểm thử được lưu tại:

```text
Extra/
```

## Git workflow

- Không làm việc trực tiếp trên `main`.
- Mỗi chức năng hoặc task nên có branch riêng.
- Trước khi merge cần pull thay đổi mới nhất và xử lý conflict.
- Chỉ merge khi project build được và chức năng đã được test.
- Mỗi thành viên commit bằng tài khoản cá nhân.
- Commit message cần mô tả rõ thay đổi, tránh dùng các nội dung chung chung như `update`, `fix` hoặc `final`.

## Demo

- Video: https://youtu.be/8bWJepTKTuU?si=_PA49gNK26JoaCDq
- Slide: `PPTX/`
- Báo cáo: `DOCX/`
- Test evidence: `Extra/`

## Giới hạn hiện tại

- Ứng dụng hoạt động chủ yếu với dữ liệu cục bộ.
- Chưa có backend hoặc cloud sync.
- Chưa có đăng nhập và quản lý nhiều tài khoản.
- User/assignee mới được chuẩn bị ở tầng dữ liệu, chưa có luồng giao việc nhiều người hoàn chỉnh.
- Notification và exact alarm còn phụ thuộc vào quyền và chính sách của từng phiên bản Android.
