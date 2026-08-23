<div align="center">

# 🎲 RNDM - Tournament & Smart Draw Manager
### تطبيق إدارة البطولات والقرعة الذكية لنظام أندرويد

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=for-the-badge&logo=android)](https://www.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI%2FMVVM-orange.svg?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![Hilt](https://img.shields.io/badge/DI-Hilt-009688.svg?style=for-the-badge)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Database-Room%20ORM-blue.svg?style=for-the-badge)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

<p align="center">
  <b>RNDM</b> هو تطبيق أندرويد متكامل وحديث مصمم لإدارة وتنظيم البطولات الرياضية والإلكترونية وإجراء القرعات العشوائية الذكية بكل سهولة وسلاسة وبأعلى معايير تجربة المستخدم.
</p>

---

</div>

## ✨ المميزات الرئيسية | Key Features

### 🏆 نظام إدارة البطولات المتقدم (Tournament Engine)
- **نظام خروج المغلوب (Knockout Brackets):** توليد شجرة الأدوار الإقصائية تلقائياً وتتبع تقدم الفرق وتحديد المتأهلين والفائز بالبطولة.
- **نظام المجموعات (Group Stage):** إنشاء مجموعات وتوليد جداول المباريات وحساب النقاط وفارق الأهداف وترتيب المتأهلين تلقائياً.
- **إدارة وتسجيل المباريات:** تسجيل النتائج، ركلات الترجيح، تتبع حالة المباريات (مجدولة، جارية، منتهية).
- **نظام الأرشفة والاسترجاع (Archive System):** حفظ البطولات المنتهية مع سجلات إحصائية كاملة وإمكانية استرجاعها أو تكرارها.

### 🎯 نظام القرعة العشوائية الذكي (Smart Randomizer)
- **قرعة الفرق والمجموعات:** توزيع اللاعبين على فرق متكافئة بدقة وسرعة.
- **قرعة الأسماء والأرقام:** خيارات تخصيص متقدمة مع إمكانية منع التكرار وإضافة أوزان.
- **تأثيرات بصرية وصوتية تفاعلية:** تجربة إثارة فريدة أثناء سحب القرعة مع أنيميشن سلس وممتع.

### 🔄 نظام التحديثات التلقائية المباشر (In-App Auto Updater)
- فحص فوري للتحديثات من مستودع GitHub الرسمي.
- عرض سجل التغييرات ومميزات الإصدار الجديد (Release Notes).
- تحميل التحديث في الخلفية مع شريط تقدم مباشر وتثبيت فوري.

### 🎨 واجهة عصرية فائقة السلاسة (Modern UI/UX)
- تصميم يعتمد على أحدث معايير **Material Design 3**.
- دعم كامل للغة العربية والإنجليزية واتجاه الواجهة (RTL/LTR).
- تأثيرات حركية تفاعلية (Smooth Transitions & Micro-interactions).
- دعم المظهر الداكن والفاتح تلقائياً (Dark & Light Mode).

---

## 🛠 البنية المعمارية والتقنيات | Architecture & Tech Stack

تم بناء التطبيق باتباع أفضل الممارسات الموصى بها من قِبل Google وباستخدام مبادئ **Clean Architecture** و **MVI / MVVM**:

```
app/
├── core/                  # الأدوات المشتركة، الثيم، والمكونات الأساسية
│   ├── network/           # إعدادات الشبكة و Retrofit
│   ├── theme/             # Material3 Color Schemes, Typography, Shapes
│   ├── ui/                # المكونات الرسومية القابلة لإعادة الاستخدام
│   └── utils/             # الفئات المساعدة والدوال العامة
├── data/                  # طبقة البيانات (Data Layer)
│   ├── local/             # Room Database, DAOs, Entities, DataStore
│   ├── remote/            # API Services, Moshi Adapters, Update Checkers
│   └── repository/        # تنفيذ المستودعات (Repository Implementations)
├── domain/                # طبقة منطق العمل (Domain Layer)
│   ├── model/             # النماذج الأساسية المستقلة (Domain Models)
│   ├── repository/        # واجهات المستودعات (Repository Interfaces)
│   └── usecase/           # حالات الاستخدام ومنطق البطولات والقرعة
└── presentation/          # طبقة العرض والواجهات (UI / Presentation Layer)
    ├── draw/              # شاشات القرعة العشوائية
    ├── tournament/        # شاشات وتفاصيل البطولات والمباريات
    ├── statistics/        # شاشات الإحصائيات وسجل النشاط
    ├── settings/          # شاشات الإعدادات والتحكم
    └── update/            # واجهات التحديث التلقائي والـ Dialogs
```

### 🧩 المكونات والمكتبات المستخدمة:
- **UI & Foundation:** [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- **Architecture:** Clean Architecture + Unidirectional Data Flow (UDF)
- **Dependency Injection:** [Hilt](https://dagger.dev/hilt/)
- **Local Storage:** [Room Database](https://developer.android.com/training/data-storage/room) & [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Asynchronous:** Kotlin Coroutines & StateFlow / SharedFlow
- **Networking & Serialization:** [Retrofit 2](https://square.github.io/retrofit/), [OkHttp](https://square.github.io/okhttp/), [Moshi](https://github.com/square/moshi), Kotlinx Serialization
- **Navigation:** Jetpack Navigation Compose
- **Background Tasks:** AndroidX WorkManager

---

## 🚀 التشغيل والإعداد | Getting Started

### المتطلبات الأساسية:
- **Android Studio:** Hedgehog (2023.1.1) أو أحدث (يوصى بـ Ladybug / Koala).
- **JDK:** Java 17 أو أحدث.
- **Android SDK:** Min SDK 26 (Android 8.0) | Target SDK 34 (Android 14).

### خطوات التثبيت:
1. استنساخ المستودع:
   ```bash
   git clone https://github.com/khalilkorichi/rndm.git
   cd rndm
   ```
2. فتح المشروع داخل **Android Studio**.
3. عمل مزامنة مع ملفات Gradle (`Sync Project with Gradle Files`).
4. تشغيل التطبيق على المحاكي أو جهاز حقيقي بالضغط على **Run** (`Shift + F10`).

---

## 📋 متطلبات البناء عبر الطرفية | CLI Build

```bash
# تجميع نسخة التطوير (Debug APK)
./gradlew assembleDebug

# تجميع نسخة الإنتاج المحسنة (Release APK)
./gradlew assembleRelease

# تشغيل الفحوصات والاختبارات (Tests)
./gradlew test
```

---

## 🤝 المساهمة | Contributing

نرحب بمساهمات الجميع! إذا كنت ترغب في تحسين التطبيق أو إضافة ميزات جديدة:
1. قم بعمل **Fork** للمستودع.
2. أنشئ فرع جديد لميزتك (`git checkout -b feature/AmazingFeature`).
3. احفظ تعديلاتك (`git commit -m 'feat: Add some AmazingFeature'`).
4. ارفع الفرع (`git push origin feature/AmazingFeature`).
5. افتح **Pull Request**.

---

## 📄 الترخيص | License

هذا المشروع مرخص تحت رخصة **MIT** - راجع ملف [LICENSE](LICENSE) لمزيد من التفاصيل.

---

<div align="center">
  صُنع بكل ❤️ بواسطة <b>خليل قريشي (Khalil Korichi)</b>
</div>
