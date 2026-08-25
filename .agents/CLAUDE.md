# CLAUDE.md — قواعد البرمجة الملزمة لمشروع RNDM

هذا الملف موجّه لأي وكيل ذكاء اصطناعي (ANTIGRAVITY / Claude / Cursor) يعمل على مشروع **RNDM**. يجب الالتزام الحرفي بكل قاعدة هنا قبل كتابة أي سطر كود. أي كود يخالف هذه القواعد يُعتبر غير مقبول ويجب إعادة كتابته.

## 1. الـ Stack التقني الإلزامي (لا استثناءات)

- **اللغة:** Kotlin 100%. ممنوع أي ملف Java جديد.
- **واجهة المستخدم:** Jetpack Compose فقط. **ممنوع منعاً باتاً** إنشاء أي ملف XML للـ Layouts (لا `activity_*.xml`, لا `fragment_*.xml`, لا `item_*.xml`). الاستثناء الوحيد المسموح: `AndroidManifest.xml`، و vector drawables بصيغة XML للأيقونات فقط (`res/drawable/ic_*.xml`).
- **نمط الهندسة المعمارية:** Clean Architecture بثلاث طبقات صارمة: `data` / `domain` / `presentation`. يُمنع أي اعتماد عكسي (مثلاً: `domain` لا يجوز أن يستورد أي شيء من `data` أو `presentation`).
- **نمط إدارة الحالة:** MVI مبسّط فوق MVVM. كل شاشة تملك:
  - `UiState` واحد (data class واحدة تمثل كل حالة الشاشة، لا حالات متفرقة متعددة).
  - `StateFlow<UiState>` للحالة المستمرة.
  - `Channel`/`SharedFlow` للأحداث اللحظية أحادية الحدوث (Snackbar, Navigation, Toast) — ممنوع استخدام `LiveData` نهائياً.
  - `sealed interface UiEvent` أو `Intent` لكل الأفعال الصادرة من الواجهة نحو الـ ViewModel.
- **قاعدة البيانات:** Room فقط. ممنوع SQLite الخام أو SharedPreferences للبيانات المركّبة (استخدم Proto DataStore للإعدادات البسيطة فقط).
- **الحقن (DI):** Hilt فقط. كل Repository, UseCase, ViewModel يجب أن تُحقن تبعياتها عبر Constructor Injection، لا `object` Singletons يدوية.
- **التزامن:** Kotlin Coroutines + Flow فقط. ممنوع RxJava أو AsyncTask أو Threads يدوية.
- **التنقل:** Navigation Compose (`androidx.navigation:navigation-compose`) مع `sealed class` أو `@Serializable data object` لتعريف الوجهات (type-safe navigation). ممنوع تمرير الوجهات كسلاسل نصية حرة (`"screen/{id}"` مباشرة بدون كلاس مغلّف).
- **الأنماط البصرية:** Material 3 (`androidx.compose.material3`) فقط. ممنوع استخدام مكونات Material 2.

## 2. قاعدة الملفات: ممنوع الحشو (No God Files)

هذه أهم قاعدة في المشروع. **لا يجوز أبداً** وضع أكثر من مكوّن منطقي واحد كبير في ملف واحد.

- كل Composable شاشة كاملة (Screen) في ملفه الخاص، باسم مطابق: `HomeScreen.kt`, `ProfileListScreen.kt`.
- كل ViewModel في ملفه الخاص: `HomeViewModel.kt`.
- كل UseCase في ملفه الخاص، باسم الفعل: `CreateProfileUseCase.kt`, `GetActiveTournamentUseCase.kt` — **ممنوع** ملف واحد اسمه `UseCases.kt` يجمع كل الحالات.
- كل Entity/Model في ملفه الخاص، لا ملف واحد `Models.kt` يجمع كل الـ data classes.
- المكوّنات المشتركة القابلة لإعادة الاستخدام (أزرار، بطاقات، Shimmer) توضع في `core/ui/components/` كل واحد بملفه.
- الحد الأقصى المقترح لأي ملف Kotlin واحد هو ~200-250 سطر. إذا تجاوز الملف هذا الحد، هذا مؤشر على وجوب تفكيكه إلى ملفات أصغر بمسؤوليات منفصلة (Single Responsibility Principle).
- ممنوع وضع منطق الأعمال (Business Logic) داخل الـ Composable مباشرة. الـ Composable يعرض الحالة فقط ويستدعي دوال الـ ViewModel.

## 3. الفهرسة والتنظيم (Package Structure)

يجب الالتزام حرفياً بالبنية المحددة في `architecture.md` المرفق. كل ميزة (Feature) لها مجلد مستقل تحت `presentation/`، ويحتوي فقط على: `Screen.kt`, `ViewModel.kt`, `UiState.kt`, `components/` (فرعي إن وجدت مكونات خاصة بالشاشة فقط ولا تُستخدم في مكان آخر).

عند إضافة ميزة جديدة مستقبلاً: أنشئ حزمة (package) جديدة بنفس القالب، لا تُقحم كود الميزة الجديدة داخل حزمة ميزة موجودة.

## 4. الأداء (Performance) — إلزامي من أول commit

- كل `data class` تمثل حالة UI يجب أن تكون `@Immutable` أو `@Stable`.
- كل قائمة داخل State يجب أن تكون من نوع غير قابل للتغيير (`ImmutableList` من `kotlinx.collections.immutable` أو `List` عادية `val` غير قابلة للتعديل بالمرجع).
- كل `LazyColumn`/`LazyVerticalGrid`/`LazyVerticalStaggeredGrid` يجب أن تستخدم `key = { item.id }` لكل عنصر، بدون استثناء.
- استخدم `derivedStateOf` لأي قيمة محسوبة من حالة أخرى داخل Composable (مثل زاوية دوران العجلة أثناء الحركة).
- لا تستدعِ دوال تخصيص object جديدة (`Modifier.background(Color(...))` مع ألوان hardcoded) داخل جسم Composable يُعاد رسمه بكثرة؛ استخرجها إلى `remember` أو ثابت خارجي.
- كل شاشة تعرض بيانات من Room يجب أن تعرض حالة `Loading` بمكوّن Shimmer/Skeleton قبل توفر البيانات، عبر `Crossfade` بين الحالتين.

## 5. الكود النظيف — قواعد صارمة

- تسمية واضحة بالإنجليزية لكل شيء في الكود (متغيرات، دوال، كلاسات)؛ التعليقات فقط يمكن أن تكون بالعربية عند الحاجة لتوضيح منطق أعمال خاص بالتطبيق (مثل قواعد قرعة المجموعات).
- ممنوع "Magic Numbers/Strings" — كل قيمة ثابتة (مثل عدد اللاعبين الأدنى للبطولة = 3) تُعرَّف كـ `const val` باسم دلالي في ملف `Constants.kt` مخصص ضمن `core/`.
- كل دالة تفعل شيئاً واحداً فقط (Single Responsibility). إن وجدت دالة تحتوي أكثر من مستوى تعقيد منطقي واحد (شرط متداخل داخل حلقة داخل شرط)، فكّكها إلى دوال أصغر.
- استخدم `sealed interface`/`sealed class` لتمثيل النتائج القابلة للفشل (`Result<T>` مخصص: `Success`, `Error`, `Loading`) بدل رمي Exceptions عبر الطبقات.
- لا تكرار كود (DRY): أي منطق يتكرر في أكثر من مكانين يُستخرج إلى دالة أو Extension Function مشتركة في `core/`.
- كل Repository, UseCase, ViewModel له اختبار وحدوي (Unit Test) واحد على الأقل عند إنشائه، حتى لو بسيطاً — لا تُترك الاختبارات لمرحلة لاحقة.

## 6. ما يجب فعله عند إعداد البنية الأساسية (هذه المرحلة بالذات)

- أنشئ كل الحزم (packages) المذكورة في `architecture.md` حتى الفارغة، مع ملف `.gitkeep` أو ملف placeholder بسيط يحتوي فقط تعليق `// TODO: Phase 2/3 feature` — هذا يمنع التشتت لاحقاً ويجعل بنية المشروع مكتملة الشكل من اليوم الأول.
- لا تُنشئ منطق فعلي لميزات المرحلة الثانية/الثالثة (الإحصائيات، الإشعارات، الاستيراد/التصدير) — فقط الهيكل الفارغ (interfaces فارغة، أو ملف واحد بسيط بدالة `TODO()`).
- كل شاشة من شاشات المرحلة الأولى (البروفايلات، أدوات القرعة الثلاثة، الصفحة الرئيسية Bento، الإعدادات الأساسية) يجب أن تكون كاملة الوظيفة وقابلة للتشغيل الفعلي، لا Placeholder.

## 7. قبل كل Commit — قائمة تحقق ذاتية للوكيل

1. هل يوجد أي ملف XML Layout؟ إن وجد، احذفه وأعد البناء بـ Compose.
2. هل يوجد ملف يتجاوز 250 سطراً يحتوي أكثر من مسؤولية واحدة؟ إن وجد، فكّكه.
3. هل كل `LazyColumn`/`LazyGrid` تستخدم `key`؟
4. هل كل حالة تحميل بيانات تعرض Shimmer قبل المحتوى؟
5. هل طبقة `domain` خالية تماماً من أي `import androidx.room.*` أو `import android.*`؟

## 8. قاعدة RTL الإلزامية والواجهات العربية (Right-to-Left Enforcement)

- **الفرض المركزي من الجذر:** التطبيق مفروض عليه RTL بالكامل من نقطة واحدة مركزية هي `RndmTheme` في `core/theme/RndmTheme.kt` عبر `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
- **ممنوع الفرض اليدوي المكرر:** ممنوع منعاً باتاً استخدام `CompositionLocalProvider(LocalLayoutDirection...)` في أي شاشة أو مكوّن فردي إلا في حالة استثنائية واحدة محددة: النصوص أو الأرقام المركبة التي يجب أن تبقى LTR دائماً (مثل نتيجة مباراة رقمية "3-1" أو نصوص لاتينية)، ويجب استخدام المكوّن المخصص `LtrForcedText` من `core/ui/components/LtrForcedText.kt`.
- **المحاذاة القياسية في Compose:** ممنوع استخدام `Modifier.padding(left=, right=)` أو محاذاة يدوية مقلوبة؛ استخدم دوماً `start` و `end` القياسية في Compose لأنها تُترجَم تلقائياً حسب اتجاه الـ `LocalLayoutDirection`.
- **الأيقونات الاتجاهية:** كل أيقونة ذات اتجاه حركي أو بصري (سهم للخلف، التالي، التقدم) يجب أن تكون من مجموعة `Icons.AutoMirrored` أو تحتوي خاصية `android:autoMirrored="true"` داخل ملف الـ Vector XML. الأيقونات غير الاتجاهية (الشعار، العجلة، البطاقة) لا تحتاج انعكاساً.

## 9. قواعد بناء وتوزيع التحديثات وحزم التطبيق (R8 Release & Optimization)

- **تقليص الحجم الإلزامي (R8 & Shrinking):** يتم دائماً بناء نسخ التحديثات الموجهة للمستخدمين بنظام وتقنية **R8** الكاملة مع تفعيل تصغير الكود وتقليص الموارد (`isMinifyEnabled = true` و `isShrinkResources = true`) لتقليص استهلاك بيانات ومساحة أجهزة المستخدمين إلى الحد الأدنى (حجم الحزمة ~5-6 MB).
- **التوقيع الرقمي الإلزامي:** يجب أن تكون حزمة الـ Release موقّعة رقمياً (`signingConfig`) لضمان قبول التثبيت التلقائي والمباشر على جميع أجهزة أندرويد بدون أخطاء الشهادات.
- **تحديث المانيفست `update.json`:** عند إصدار أي تحديث جديد، يجب دائماً حساب بصمة SHA-256 الدقيقة وحجم الملف بالبايتات وتحديث `update.json` في المستودع لضمان وصول التحديث اللحظي لجميع المستخدمين.
- **حزم المخرجات:** يتم وضع النسخة المصغرة والموقعة في المسار `.build-outputs/app-release.apk` و `.build-outputs/RNDM-release.apk`.

