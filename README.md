# Android_UTH_01 - TaskManagementApp

## Thanh vien

| STT | MSSV | Ho va ten | Vai tro |
|---:|---|---|---|
| 1 | 083205012180 | Nguyen Duy Khang | Nhom truong |
| 2 | 066205002941 | Nguyen Hoai Nam | Thanh vien |
| 3 | 077205003436 | Nguyen Hoang Minh Khoi | Thanh vien |
| 4 | 080206016469 | Le Tran Dang Khoi | Thanh vien |
| 5 | 049206013293 | Dang Lam Truong | Thanh vien |
| 6 | 079205031894 | Nguyen Thanh Dat | Thanh vien |

## Gioi thieu

TaskManagementApp la ung dung Android ho tro quan ly cong viec ca nhan. Repo hien tai da co app shell chinh, navigation va cac module nen tang de team tiep tuc hoan thien chuc nang.

## Cong nghe da chot

- Kotlin
- XML + ViewBinding
- MVVM + Repository
- Room
- DataStore
- Lifecycle ViewModel, LiveData/Flow
- AlarmManager + BroadcastReceiver
- NotificationManager + quyen POST_NOTIFICATIONS cho Android 13+
- JSON + Storage Access Framework

Khong su dung Compose, Firebase, Retrofit, Hilt hoac framework phuc tap khac.

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
3. Dat Gradle user home ve `C:\Users\ad\.gradle` neu Android Studio hoi.
4. Chon Gradle JDK/JVM version 21 hoac Embedded JDK cua Android Studio.
5. Sync Gradle va chay app module.

Chay bang terminal:

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

## Cau hinh Git

Khong commit password, secret hoac file cau hinh may ca nhan. File `local.properties`, build output va cache IDE/Gradle da duoc bo qua trong Git.

## Chuc nang du kien

- [ ] Quan ly danh sach cong viec
- [ ] Them, sua, xoa cong viec
- [ ] Phan loai va theo doi trang thai cong viec
- [ ] Nhac viec bang alarm va notification
- [ ] Cau hinh ung dung bang DataStore
- [ ] Backup/restore bang JSON va Storage Access Framework