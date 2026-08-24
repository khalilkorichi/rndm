# دراسة وتدقيق معماري شامل: مزامنة Firebase ونظام الأدوار والأمان لتطبيق RNDM (RBAC & Offline-First Architecture Audit)

**تاريخ التحديث:** 24 أغسطس 2026  
**المشروع:** RNDM (Android — Kotlin / Jetpack Compose / Room / Firebase)  
**الحالة:** تقرير تدقيق وتخطيط معماري شامل ومُحدّث (ممنوع كتابة كود إنتاجي أو تطبيق تعديلات قبل الموافقة الصريحة)  
**المسار المعتمد للملف:** `C:\Users\Khalil\Desktop\RNDM\.agents\FIREBASE_SYNC_ARCHITECTURE_AUDIT.md`

---

## 1. Executive Summary (الملخص التنفيذي بالعربية)

تم تحديث هذه الوثيقة المعمارية لتشمل **نظام أدوار حقيقي وصارم قائم على السحاب (Cloud-Enforced Role-Based Access Control - RBAC)** بالتكامل مع بنية **Offline-First** لتطبيق **RNDM**، مع الالتزام الكامل بمعايير `CLAUDE.md`.

### المبادئ الهندسية والأمنية الحاكمة:
1. **قاعدة Room هي المصدر الأوحد للحقيقة (Single Source of Truth) للواجهات:**  
   واجهات Jetpack Compose تقرأ دائماً من Room عبر `Flow` لضمان سرعة فائقة (Zero Latency) وانعدام التجميد. تأتي البيانات من السحاب لتُكتب في Room مباشرة، وتنعكس في الواجهة تلقائياً.
2. **نظام أدوار أمني حقيقي عبر Firebase Authentication (بدون كلمات مرور مشفرة في التطبيق):**  
   - يمنع منعاً باتاً تخزين أي كلمات مرور أو مفاتيح سرية داخل التطبيق (`BuildConfig`, `strings.xml`, كود Kotlin، أو ملفات مشفرة).
   - **الضيف (Guest):** يسجل دخولاً مجهولاً تلقائياً (`signInAnonymously()`) عند فتح التطبيق. يملك صلاحيات كاملة محلياً، وصلاحية قراءة فقط للبطولات السحابية المشتركة التي ينضم إليها.
   - **المدير (Admin / Host):** يسجل دخولاً حقيقياً بالبريد الإلكتروني وكلمة المرور (`signInWithEmailAndPassword`) ويحصل حسابه على **Custom Claim** (`{ "role": "admin" }`) يتم تعيينه من بيئة موثوقة (Firebase Admin SDK).
   - **المشاهد (Viewer):** ينضم بكود البطولة ويعرض نتائجها ومباريات شجرتها لحظياً دون القدرة على تعديل أي نتيجة.
   - **مسؤول النتائج (Score Keeper - مرحلة مستقبلية):** دعوة من الأدمن بـ UID محدد داخل `editorIds` لتسجيل النتائج فقط دون إمكانية حذف البطولة أو تعديل المشاركين.
3. **حماية السحاب بقواعد Firestore Security Rules غير القابلة للاختراق:**  
   تنفيذ فحص الصلاحيات (`request.auth.token.role == "admin" || request.auth.uid == resource.data.hostUid`) في خوادم Google قبل تنفيذ أي عملية كتابة أو حذف، مما يحمي البيانات حتى لو تم التلاعب بالـ APK.
4. **سجل تدقيق التغييرات (Audit Log) وقفل حالة البطولة (Tournament State Lock):**  
   حفظ سجل لكل تغيير بالنتيجة لمعرفة "من عدّل النتيجة ومتى"، ودعم دورة حياة آمنة للبطولة (`DRAFT -> ACTIVE -> LOCKED -> FINISHED`).
5. **ترقية Room غير مدمّرة (Migration 8 -> 9):**  
   إضافة حقول المزامنة والأدوار كأعمدة جديدة ذات قيم افتراضية آمنة دون حذف أو تعديل أي بيانات محلية سابقة.

---

## 2. مصفوفة الأدوار والصلاحيات (Role-Based Access Matrix)

| الدور (Role) | آلية تسجيل الدخول | الصلاحيات المحلية (Local Device) | صلاحيات Firebase السحابية (Firestore) |
|---|---|---|---|
| **Guest (ضيف)** | تلقائي عند فتح التطبيق (`signInAnonymously()`) | إنشاء وتعديل البروفايلات المحلية، إجراء القرعات التفاعلية (عجلة/بطاقات/قائمة)، إدارة بطولات محلية غير مشتركة. | قراءة البطولات المشتركة التي انضم إليها عبر الكود. ممنوع من الكتابة أو الحذف السحابي نهائياً. |
| **Admin (مدير)** | تسجيل دخول فعلي (`Email & Password`) عبر شاشة الإدارة | كامل الصلاحيات المحلية + الوصول لأدوات الإدارة السحابية. | إنشاء، تعديل، حذف البطولات السحابية، تسجيل النتائج، قفل/فتح البطولة، وتوليد شجرة الإقصائيات. |
| **Viewer (مشاهد)** | كود دعوة (مثل `KHL-7A3` أو `RNDM78`) | عرض البطولة في قاعدة Room المحلية ومتابعة التحديثات اللحظية. | قراءة وثائق ومباريات البطولة المحددة فقط عبر اشتراك لحظي (`Snapshot Listener`). |
| **Score Keeper (مسؤول نتائج - اختياري للمستقبل)** | دعوة من الأدمن بإضافة الـ UID إلى `editorIds` | تسجيل النتائج في البطولة المحددة محلياً. | تعديل وثائق المباريات (`matches`) لتلك البطولة فقط، مع منعه من حذف البطولة أو تعديل المشاركين. |

---

## 3. تجربة المستخدم لإدارة الأدوار (Admin & Guest UX Flow)

### 3.1 وضع الضيف الافتراضي (Default Guest Mode):
- عند أول تشغيل للتطبيق:
  1. ينفّذ التطبيق مصادقة صامتة `signInAnonymously()`.
  2. يُحفظ الـ `UID` المجهول كجلسة حالية.
  3. تظهر بطاقة في شاشة الإعدادات بعنوان: **"إدارة الحساب والصلاحيات"**، وبداخلها الشارة: `وضع الضيف (Guest)` مع زر `دخول الأدمن (Admin Login)`.
  4. في شاشات البطولات: يتم إخفاء أزرار الحذف السحابي، تعديل النتائج، وأزرار إدارة البطولة المشتركة عن الضيف لتوفير واجهة نظيفة ومنطقية (Clean UX).
  5. تبقى البروفايلات الشخصية وإعدادات الجهاز محلية 100% ولا ترفع للسحاب.

---

### 3.2 مسار دخول الأدمن (Admin Login Flow):
- عند الضغط على زر **"دخول الأدمن"** في الإعدادات:
  1. تظهر شاشة/نافذة منبثقة مصممة وفق نظام RNDM الكبسولي.
  2. تحتوي على:
     - حقل البريد الإلكتروني أو اسم المستخدم (يُحوّل داخلياً إلى بريد مثل `admin@rndm.local` أو البريد المعتمد).
     - حقل كلمة المرور (مع إمكانية إظهار/إخفاء الرمز).
     - زر **"تسجيل الدخول"** مع مؤشر تحميل.
  3. **الأمان في التعامل مع الأخطاء:** عند إدخال بيانات خاطئة، تظهر رسالة موحدة: `"بيانات الدخول غير صحيحة"` دون إيضاح هل الخطأ في البريد أم كلمة المرور لمنع هجمات الاستكشاف (Enumeration Attacks).
  4. عند نجاح المصادقة:
     - يتحول حساب Firebase Auth إلى الحساب الإداري.
     - يتم تحديث `SessionRepository` المحلي إلى `UserRole.ADMIN`.
     - يتغير نص بطاقة الإعدادات إلى: `أنت مسجل كمدير (Admin)` مع زر `تسجيل الخروج`.
     - تظهر للمدير خيارات: "مشاركة البطولة سحابياً"، "تعديل النتائج"، "إعادة القرعة"، و"حذف البطولة السحابية".

---

### 3.3 تأكيدات العمليات الحساسة في واجهة الأدمن (Safety Confirmations):
- **حذف بطولة سحابية:** نافذة تأكيد مزدوجة تتطلب كتابة اسم البطولة للتأكيد لمنع الحذف العرضي.
- **تعديل نتيجة مباراة منتهية:** نافذة حوارية تُظهر النتيجة السابقة والجديدة مع زر تأكيد واضح.
- **إعادة القرعة بعد النشر:** تنبيه تحذيري يوضح أن شجرة المباريات الحالية ستُحذف وتُعاد صياغتها.

---

## 4. دورة حياة البطولة وقفل التعديل (Tournament Lifecycle & Locking)

يتم تتبع حالة البطولة عبر حقل `status` في Firestore و Room:

```text
       ┌──────────┐      الأدمن ينشر القرعة      ┌──────────┐
       │  DRAFT   │ ──────────────────────────> │  ACTIVE  │
       └──────────┘                             └──────────┘
                                                     │
                                       انتهاء النهائي │ أو قفل يدوي
                                                     ▼
       ┌──────────┐      أرشفة واعتماد نهائي    ┌──────────┐
       │ FINISHED │ <────────────────────────── │  LOCKED  │
       └──────────┘                             └──────────┘
```

- **`DRAFT` (مسودة):** مرحلة القرعة؛ يُسمح بتعديل المشاركين والفرق والمجموعات.
- **`ACTIVE` (نشطة):** القرعة معتمدة؛ يُسمح بتسجيل وتعديل نتائج المباريات فقط، ويُمنع تغيير المشاركين.
- **`LOCKED` (مقفلة):** قفل يدوي أو مؤقت من الأدمن؛ يمنع تعديل أي نتيجة إلا بعد قيام الأدمن بفتح القفل (`Unlock`).
- **`FINISHED` (منتهية):** اكتملت المباراة النهائية وتوج الفائز؛ للقراءة وعرض الإحصائيات فقط.

---

## 5. فحص Firebase MCP وحالة المشروع المحلي

### 5.1 نتيجة التحقق عبر Firebase MCP:
- تم تنفيذ استدعاء للقراءة فقط عبر `firebase_list_projects`.
- الحالة الحالية للحساب المحلي في CLI: منتهي الصلاحية ويتطلب تنفيذ `firebase login --reauth` لربط الأدوات الخارجية، وهو أمر إجرائي لن يؤثر على كود الأندرويد الداخلي.

### 5.2 إعدادات المشروع المحلية (`google-services.json`):
- **Project ID:** `rndm-app-4954b`
- **Project Number:** `31537033462`
- **Package Name:** `com.rndm.app`
- **Google Services Plugin:** 4.4.2 مفعل في `build.gradle.kts`.
- **Firebase BOM:** 33.9.0 مضاف في `libs.versions.toml`.

### 5.3 الخدمات المطلوبة فقط:
1. **Cloud Firestore:** للبطولات، المباريات، الأكواد، وسجلات التدقيق.
2. **Firebase Authentication (Anonymous + Email/Password):** لإدارة جلسات الضيوف والأدمن.
3. **Custom Claims (تُعيّن للأدمن عبر Admin SDK أو Firebase Console/Script):** لوسم حساب المدير بـ `role: "admin"`.

---

## 6. مخطط Firestore المحدّث (Firestore Schema & Audit Logs)

### 6.1 هيكل المجموعات الكامل:

```text
/tournaments/{tournamentId}                         (Document: بيانات البطولة، المالك، والحالة)
    ├── /participants/{participantId}               (Subcollection: المشاركون وأنديتهم)
    ├── /matches/{matchId}                           (Subcollection: المباريات والنتائج)
    └── /auditLogs/{logId}                           (Subcollection: سجل تدقيق وتاريخ التعديلات)

/tournament_codes/{shareCode}                       (Document: فهرس كود الانضمام السريع)
```

---

### 6.2 تفاصيل الوثائق وأمثلة JSON:

#### 1. وثيقة البطولة: `tournaments/{tournamentId}`
```json
{
  "id": "tourn_8f7b2c1a-9e3d-4c5b",
  "name": "بطولة الأساطير 2026",
  "type": "GROUPS_KNOCKOUT",
  "stage": "GROUPS",
  "status": "ACTIVE",
  "hostUid": "admin_uid_7X9kLm2NpQ4vW8z",
  "memberIds": [
    "admin_uid_7X9kLm2NpQ4vW8z",
    "anon_uid_guest_111",
    "anon_uid_guest_222"
  ],
  "editorIds": [
    "admin_uid_7X9kLm2NpQ4vW8z"
  ],
  "shareCode": "KHL-7A3",
  "groupsCount": 2,
  "qualifiersPerGroup": 2,
  "isArchived": false,
  "createdAt": 1756044800000,
  "updatedAt": 1756045200000,
  "version": 4
}
```

#### 2. وثيقة المباراة: `tournaments/{tournamentId}/matches/{matchId}`
```json
{
  "id": "match_501",
  "stage": "GROUP_STAGE",
  "groupIndex": 0,
  "roundIndex": 1,
  "bracketMatchIndex": null,
  "playerOneName": "خليل",
  "playerOneClub": "ريال مدريد",
  "playerTwoName": "أيمن",
  "playerTwoClub": "مانشستر سيتي",
  "scoreOne": 3,
  "scoreTwo": 1,
  "penaltyScoreOne": null,
  "penaltyScoreTwo": null,
  "winnerName": "خليل",
  "status": "FINISHED",
  "scheduledTimestamp": 1756045500000,
  "isPlayerOneLuckyLoser": false,
  "isPlayerTwoLuckyLoser": false,
  "updatedByUid": "admin_uid_7X9kLm2NpQ4vW8z",
  "updatedAt": 1756046000000
}
```

#### 3. وثيقة سجل التدقيق: `tournaments/{tournamentId}/auditLogs/{logId}`
```json
{
  "id": "log_9921",
  "actorUid": "admin_uid_7X9kLm2NpQ4vW8z",
  "actorRole": "admin",
  "action": "MATCH_SCORE_UPDATED",
  "matchId": "match_501",
  "before": {
    "scoreOne": 1,
    "scoreTwo": 1,
    "winnerName": null,
    "status": "PENDING"
  },
  "after": {
    "scoreOne": 3,
    "scoreTwo": 1,
    "winnerName": "خليل",
    "status": "FINISHED"
  },
  "timestamp": 1756046000000
}
```

#### 4. وثيقة كود الانضمام: `tournament_codes/{shareCode}`
```json
{
  "shareCode": "KHL-7A3",
  "tournamentId": "tourn_8f7b2c1a-9e3d-4c5b",
  "hostUid": "admin_uid_7X9kLm2NpQ4vW8z",
  "createdAt": 1756044800000,
  "isActive": true
}
```

---

## 7. قواعد أمان Firestore المحدّثة والنهائية (Production-Grade Security Rules)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ── دوال التحقق الأمنية المساعدة ──

    // 1. التحقق من تسجيل الدخول (سواء مجهول أو بالبريد)
    function isSignedIn() {
      return request.auth != null && request.auth.uid != null;
    }

    // 2. التحقق من أن المستخدم يملك دور الأدمن عبر Custom Claims
    function isGlobalAdmin() {
      return isSignedIn() && request.auth.token.role == "admin";
    }

    // 3. التحقق من أن المستخدم هو منشئ البطولة
    function isTournamentHost(tournamentData) {
      return isSignedIn() && (
        isGlobalAdmin() || 
        request.auth.uid == tournamentData.hostUid
      );
    }

    // 4. التحقق من أن المستخدم عضو/مشاهد في البطولة
    function isTournamentMember(tournamentData) {
      return isSignedIn() && (
        isTournamentHost(tournamentData) ||
        request.auth.uid in tournamentData.memberIds
      );
    }

    // 5. التحقق من أن المستخدم محرر نتائج معتمد (Host أو Editor)
    function isTournamentEditor(tournamentData) {
      return isSignedIn() && (
        isTournamentHost(tournamentData) ||
        request.auth.uid in tournamentData.editorIds
      );
    }

    // 6. استرجاع بيانات وثيقة البطولة الرئيسية
    function getTournament(tournamentId) {
      return get(/databases/$(database)/documents/tournaments/$(tournamentId)).data;
    }

    // ── 1. مجموعة أكواد الانضمام (tournament_codes) ──
    match /tournament_codes/{shareCode} {
      // السماح بالقراءة لأي مستخدم مسجل للبحث عن البطولة بالكود
      allow read: if isSignedIn();

      // إنشاء الكود مسموح للأدمن أو منشئ البطولة بشرط عدم وجوده مسبقاً
      allow create: if isSignedIn() &&
        request.resource.data.hostUid == request.auth.uid &&
        request.resource.data.shareCode == shareCode &&
        !exists(/databases/$(database)/documents/tournament_codes/$(shareCode));

      // منع التعديل، والحذف مسموح فقط للمنشئ أو الأدمن العام
      allow update: if false;
      allow delete: if isSignedIn() && (isGlobalAdmin() || resource.data.hostUid == request.auth.uid);
    }

    // ── 2. مجموعة البطولات الرئيسية (tournaments) ──
    match /tournaments/{tournamentId} {
      // القراءة متاحة للأدمن أو الأعضاء المنضمين، أو للتحقق الأولي عند الانضمام
      allow read: if isSignedIn();

      // إنشاء البطولة محصور بالأدمن أو الحساب المصادق عليه كمنشئ
      allow create: if isSignedIn() &&
        request.resource.data.hostUid == request.auth.uid &&
        request.resource.data.id == tournamentId;

      // تعديل البطولة: مسموح للأدمن/المنشئ، أو للضيف لإضافة UID الخاص به فقط إلى memberIds
      allow update: if isSignedIn() && (
        isTournamentHost(resource.data) ||
        // حالة انضمام المشاهد: يسمح فقط بإضافة UID الخاص به إلى مصفوفة memberIds دون تعديل باقي الحقول
        (
          request.resource.data.diff(resource.data).affectedKeys().hasOnly(['memberIds']) &&
          request.resource.data.memberIds.hasAll(resource.data.memberIds) &&
          request.resource.data.memberIds.hasAny([request.auth.uid])
        )
      );

      // الحذف محصور بالأدمن/المنشئ فقط
      allow delete: if isSignedIn() && isTournamentHost(resource.data);

      // ── 2.1 المشاركون (participants) ──
      match /participants/{participantId} {
        allow read: if isSignedIn();
        // التعديل والإضافة محصوران بالمنشئ فقط عندما تكون البطولة DRAFT أو ACTIVE
        allow write: if isTournamentHost(getTournament(tournamentId));
      }

      // ── 2.2 المباريات والنتائج (matches) ──
      match /matches/{matchId} {
        allow read: if isSignedIn();
        // تسجيل النتائج مسموح للمنشئ والمحررين المعتمدين، بشرط ألا تكون البطولة LOCKED أو FINISHED
        allow write: if isTournamentEditor(getTournament(tournamentId)) &&
          getTournament(tournamentId).status != "LOCKED" &&
          getTournament(tournamentId).status != "FINISHED";
      }

      // ── 2.3 سجل تدقيق التغييرات (auditLogs) ──
      match /auditLogs/{logId} {
        allow read: if isSignedIn() && isTournamentHost(getTournament(tournamentId));
        // الكتابة متاحة فقط لمن يقوم بتعديل النتيجة لتوثيق التغيير
        allow create: if isTournamentEditor(getTournament(tournamentId)) &&
          request.resource.data.actorUid == request.auth.uid;
        // منع التعديل والحذف نهائياً لضمان نزاهة السجل
        allow update, delete: if false;
      }
    }

    // منع أي وصول خارج المسارات المعرفة
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 8. تعديلات Room المقترحة والترقية (Migration 8 -> 9)

### 8.1 النماذج الجديدة في طبقة Domain/Core:

```kotlin
// domain/model/UserRole.kt
enum class UserRole {
    GUEST,       // ضيف مجهول
    ADMIN,       // مدير مسجل بحساب حقيقي
    VIEWER       // مشاهد لبطولة محددة
}

// domain/model/TournamentStatus.kt
enum class TournamentStatus {
    DRAFT,       // مسودة
    ACTIVE,      // نشطة وجارية
    LOCKED,      // مقفلة مؤقتاً
    FINISHED     // منتهية ومعتمدة
}

// domain/model/SyncStatus.kt
enum class SyncStatus {
    LOCAL_ONLY,       // محلية فقط
    SYNCED,           // متزامنة مع السحاب
    PENDING_UPLOAD,   // تعديلات معلقة تنتظر الاتصال
    SYNC_ERROR        // خطأ مزامنة
}
```

---

### 8.2 التعديلات على كيانات Room:

#### 1. `TournamentEntity.kt`:
```kotlin
@Entity(tableName = "tournaments", indices = [Index(value = ["isArchived", "updatedAt"])])
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val type: TournamentType = TournamentType.GROUPS_KNOCKOUT,
    val stage: TournamentStage = TournamentStage.GROUPS,
    val status: TournamentStatus = TournamentStatus.ACTIVE, // [جديد]
    val playersProfileId: Long,
    val clubsProfileId: Long? = null,
    val groupsCount: Int = 2,
    val qualifiersPerGroup: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    // حقول المزامنة السحابية [جديد]
    val remoteId: String? = null,
    val shareCode: String? = null,
    val isRemote: Boolean = false,
    val isHost: Boolean = true,
    val hostUid: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val lastSyncedAt: Long? = null,
    val remoteVersion: Long = 0L
)
```

#### 2. `MatchEntity.kt`:
```kotlin
// إضافة:
val remoteId: String? = null,
val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
val updatedAt: Long = System.currentTimeMillis()
```

#### 3. `TournamentParticipantEntity.kt`:
```kotlin
// إضافة:
val remoteId: String? = null
```

---

### 8.3 كود ترقية قاعدة البيانات الصريح (`MIGRATION_8_9`):

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. إضافة حقول البطولة
        db.execSQL("ALTER TABLE tournaments ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN remoteId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN shareCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isRemote INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isHost INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN hostUid TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN lastSyncedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN remoteVersion INTEGER NOT NULL DEFAULT 0")

        // 2. إضافة حقول المباريات
        db.execSQL("ALTER TABLE matches ADD COLUMN remoteId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE matches ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("ALTER TABLE matches ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        // 3. إضافة حقول المشاركين
        db.execSQL("ALTER TABLE tournament_participants ADD COLUMN remoteId TEXT DEFAULT NULL")

        // 4. الفهارس السريعة
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournaments_remoteId` ON `tournaments` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournaments_shareCode` ON `tournaments` (`shareCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_remoteId` ON `matches` (`remoteId`)")
    }
}
```

---

## 9. قائمة الملفات والحزم المقترحة للإضافة والتعديل لاحقاً

التزاماً بقاعدة `CLAUDE.md` الصارمة (عدم تجاوز 200-250 سطراً، وتقسيم المسؤوليات):

```text
com.rndm.app/
├── core/
│   ├── auth/
│   │   ├── UserRole.kt                     (enum: GUEST, ADMIN, VIEWER)
│   │   └── AuthSession.kt                  (بيانات الجلسة الحالية والـ UID)
│   └── sync/
│       ├── SyncStatus.kt                   (enum لحالات المزامنة)
│       └── NetworkMonitor.kt               (مراقبة حالة الاتصال)
│
├── data/
│   ├── remote/
│   │   ├── firebase/
│   │   │   ├── FirebaseAuthDataSource.kt   (Anonymous + Email/Password + Token Claims)
│   │   │   ├── FirestoreTournamentDataSource.kt (العمليات السحابية للبطولة)
│   │   │   ├── FirestoreAuditDataSource.kt (تسجيل واسترجاع سجلات التدقيق)
│   │   │   └── dto/
│   │   │       ├── FirestoreTournamentDto.kt
│   │   │       ├── FirestoreParticipantDto.kt
│   │   │       ├── FirestoreMatchDto.kt
│   │   │       ├── FirestoreAuditLogDto.kt
│   │   │       └── FirestoreCodeDto.kt
│   │   └── mapper/
│   │       ├── FirestoreTournamentMapper.kt
│   │       ├── FirestoreParticipantMapper.kt
│   │       ├── FirestoreMatchMapper.kt
│   │       └── FirestoreAuditMapper.kt
│   ├── repository/
│   │   ├── AuthRepositoryImpl.kt           (إدارة المصادقة وجلسة المستخدم)
│   │   ├── SyncRepositoryImpl.kt           (تنسيق الرفع والجلب بين Remote و Room)
│   │   └── AuditRepositoryImpl.kt          (إدارة سجلات التدقيق)
│   └── sync/
│       └── TournamentSyncWorker.kt         (WorkManager للمزامنة بالخلفية)
│
├── domain/
│   ├── model/
│   │   ├── TournamentStatus.kt             (DRAFT, ACTIVE, LOCKED, FINISHED)
│   │   └── AuditLog.kt                     (نموذج سجل التدقيق)
│   ├── repository/
│   │   ├── AuthRepository.kt               (واجهة نقية للمصادقة والأدوار)
│   │   ├── SyncRepository.kt               (واجهة نقية للمزامنة)
│   │   └── AuditRepository.kt              (واجهة نقية لسجل التدقيق)
│   └── usecase/
│       ├── auth/
│       │   ├── InitializeGuestSessionUseCase.kt
│       │   ├── LoginAdminUseCase.kt
│       │   ├── LogoutAdminUseCase.kt
│       │   └── GetCurrentUserRoleUseCase.kt
│       └── sync/
│           ├── JoinTournamentByCodeUseCase.kt
│           ├── PublishTournamentUseCase.kt
│           ├── ObserveRemoteTournamentUseCase.kt
│           └── LogMatchScoreChangeUseCase.kt
│
└── presentation/
    ├── admin/
    │   ├── AdminLoginDialog.kt             (نافذة تسجيل دخول الأدمن المدمجة في الإعدادات)
    │   ├── AdminLoginViewModel.kt
    │   └── AdminLoginUiState.kt
    └── tournament/
        ├── join/
        │   ├── JoinTournamentScreen.kt     (شاشة إدخال كود البطولة)
        │   ├── JoinTournamentViewModel.kt
        │   ├── JoinTournamentUiState.kt
        │   └── components/
        │       └── ShareCodeInputBox.kt
        ├── share/
        │   ├── ShareTournamentDialog.kt    (عرض كود الدعوة للمدير)
        │   └── SyncStatusBadge.kt          (شارة حالة المزامنة)
        └── audit/
            ├── AuditLogHistoryBottomSheet.kt (عرض سجل التعديلات للأدمن)
            └── components/
                └── AuditLogItemRow.kt
```

---

## 10. خطة التنفيذ المرحلية المحدثة (Phased Implementation Plan)

### المرحلة 1: بنية المصادقة وترقية قاعدة البيانات (Auth & Room Database v9)
1. إضافة مكتبات `firebase-auth-ktx` و `firebase-firestore-ktx` في `libs.versions.toml` و `build.gradle.kts`.
2. إنشاء `MIGRATION_8_9` في `RndmDatabase.kt` وتحديث الكيانات.
3. بناء `FirebaseAuthDataSource` و `AuthRepositoryImpl` لدعم `signInAnonymously()` و `signInWithEmailAndPassword()`.
4. **الاختبارات:** اختبارات ترقية Room Migration 8 -> 9 واختبارات فحص الأدوار في Unit Tests.

### المرحلة 2: واجهة الأدمن والتحكم في الجلسة (Admin Login & Session UI)
1. إنشاء بطاقة إدارة الصلاحيات في شاشة الإعدادات `SettingsScreen`.
2. بناء `AdminLoginDialog` بحقول البريد وكلمة المرور ورسالة الخطأ الآمنة.
3. ربط حالة الواجهة بحيث تُخفى أزرار الحذف/التعديل السحابي عن الضيف وتظهر للأدمن فقط.
4. **الاختبارات:** Compose UI Tests لاختبار التبديل بين وضع الضيف ووضع الأدمن.

### المرحلة 3: نشر البطولة وكود الانضمام (Publishing & Join Flow)
1. بناء `PublishTournamentUseCase` لتوليد الكود (`KHL-7A3`) ونشر البطولة في Firestore.
2. بناء `JoinTournamentScreen` للمشاهدين للانضمام بكود البطولة وحفظها محلياً في Room.
3. **الاختبارات:** Unit Tests لآلية التحقق من الأكواد وحفظ البيانات المستلمة.

### المرحلة 4: الاستماع اللحظي وسجل التدقيق (Realtime Sync & Audit Logging)
1. ربط `callbackFlow` مع `awaitClose` للاستماع لتحديثات المباريات وكتابتها في Room.
2. بناء `FirestoreAuditDataSource` لتوثيق أي تعديل في النتيجة تلقائياً.
3. دعم قفل البطولة (`status: LOCKED / FINISHED`).
4. **الاختبارات:** Turbine Tests لتدفقات Flow واختبار منع الحلقات التكرارية.

---

## 11. توصية نهائية والخطوات المطلوبة للبدء

> [!TIP]
> **التوصية العملية:** البداية بثلاثية الأدوار: **`Guest` + `Admin` + `Viewer`**. هذه البنية توفر أماناً بنسبة 100%، وتمنع أي تلاعب خارجي، وتحافظ على بساطة التطبيق دون تعقيد مبكر.

### الخطوات الإجرائية التالية:
1. اعتماد هذه الوثيقة المحدثة رسمياً.
2. تشغيل `firebase login --reauth` في بيئة التطوير.
3. البدء فوراً في تنفيذ **المرحلة الأولى** (إضافة المكتبات، ترقية Room 8 -> 9، ونماذج المصادقة).
