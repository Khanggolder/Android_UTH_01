# Android_UTH_01 - TaskManagementApp

## Thanh vien

| STT | MSSV | Ho va ten | Vai tro |
|---:|---|---|---|
| 1 | 083205012180 | Nguyễn Duy Khang | Nhom truong |
| 2 | 066205002941 | Nguyễn Hoài Nam | Thanh vien |
| 3 | 077205003436 | Nguyễn Hoàng Minh Khôi | Thanh vien |
| 4 | 080206016469 | Lê Trần Đăng Khôi | Thanh vien |
| 5 | 049206013293 | Đặng Lam Trường | Thanh vien |
| 6 | 079205031894 | Nguyễn Thành Đạt | Thanh vien |

## Gioi thieu

TaskManagementApp la ung dung Android ho tro quan ly cong viec ca nhan. Repository hien tai duoc thiet lap o muc nen tang de cac thanh vien clone ve va phat trien module rieng, chua implement hoan chinh cac chuc nang nghiep vu.

## Kien truc he thong

- Mo hinh: Android mobile application
- Kien truc code: MVVM + Repository
- UI: XML layout + ViewBinding
- Local data: Room
- Local preferences: DataStore
- Nhac viec: AlarmManager + BroadcastReceiver
- Thong bao: NotificationManager + quyen Android 13+
- Backup/restore: JSON + Storage Access Framework
- Protocol: Chua ap dung
- Port mac dinh: Khong co
- Cau truc message: Chua ap dung

## Cong nghe da chot

- Kotlin
- XML + ViewBinding
- MVVM + Repository
- Room
- DataStore
- Lifecycle ViewModel, LiveData/Flow
- AlarmManager + BroadcastReceiver
- NotificationManager + POST_NOTIFICATIONS cho Android 13+
- JSON + Storage Access Framework

Khong su dung Compose, Firebase, Retrofit, Hilt hoac framework phuc tap khac trong giai do nen tang nay.

## Cau truc source code

```text
Code/TaskManagementApp/app/src/main/java/com/uth/taskmanagement/
├── core
├── navigation
├── data
│   ├── model
│   ├── local
│   └── repository
├── ui
│   ├── tasklist
│   ├── taskform
│   ├── calendar
│   └── settings
├── notification
├── recurrence
├── security
├── backup
└── utils
```

Cac file nen hien co:

- `core/AppConstants.kt`
- `core/AppResult.kt`
- `navigation/AppNavigator.kt`
- `utils/DateTimeUtils.kt`
- `utils/ValidationUtils.kt`

## Yeu cau moi truong

- He dieu hanh: Windows
- Ngon ngu: Kotlin
- Cong cu: Android Studio, Gradle Wrapper, Android SDK theo cau hinh du an
- JDK: Uu tien JDK di kem Android Studio

## Clone va mo project

```text
git clone https://github.com/Khanggolder/Android_UTH_01.git
cd Android_UTH_01
```

Mo Android project tai thu muc:

```text
Code/TaskManagementApp
```

Trong Android Studio:

1. Chon Open.
2. Chon folder `Code/TaskManagementApp`.
3. Doi Gradle sync xong.
4. Chay app module tren emulator hoac thiet bi that.

## Huong dan chay

### Server

```text
Khong co server rieng trong phien ban hien tai.
```

### Client

```text
cd Code/TaskManagementApp
.\gradlew.bat assembleDebug
```

## Git flow cho nhom

- Khong code truc tiep tren `main`.
- Moi task tao mot feature branch rieng, vi du `feature/task-list-ui`.
- Moi ngay merge `main` vao branch ca nhan de cap nhat thay doi moi.
- Khi co conflict, uu tien lay code tu `main`, sau do them lai phan code ca nhan neu can.
- Chi merge ve `main` khi project sync/build duoc va test passed.
- Commit message ngan gon, noi ro module dang lam.

## Cau hinh

Khong commit password, secret hoac file cau hinh may ca nhan. File `local.properties`, build output va cache IDE/Gradle da duoc bo qua trong Git.

## Chuc nang du kien

- [ ] Quan ly danh sach cong viec
- [ ] Them, sua, xoa cong viec
- [ ] Phan loai va theo doi trang thai cong viec
- [ ] Nhac viec bang alarm va notification
- [ ] Cau hinh ung dung bang DataStore
- [ ] Backup/restore bang JSON va Storage Access Framework

## Chua implement trong nen tang nay

- CRUD task that
- DAO/Database chi tiet
- Notification that
- PIN/bao mat that
- Calendar that
- Backup/restore that

## Kiem thu

- Functional test: Chua bo sung
- Test du lieu khong hop le: Chua bo sung
- Test mat ket noi: Khong ap dung trong phien ban hien tai
- Stress test: Chua bo sung
- Performance test: Chua bo sung

Bang chung kiem thu luu tai `Extra/`.

## Demo

- Video: [Public hoac Unlisted URL]
- Slide: `PPTX/`
- Bao cao: `DOCX/`

## Gioi han

Phien ban hien tai chi tao nen tang project, cau truc package va dependency can thiet de team phat trien tiep. App chi can mo duoc MainActivity va chua co chuc nang quan ly cong viec hoan chinh.
