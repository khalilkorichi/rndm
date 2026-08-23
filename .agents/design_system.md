# design_system.md — نظام التصميم لتطبيق RNDM

## 1. مبدأ عام

كل قيم التصميم (ألوان، خطوط، أشكال) تُعرَّف كـ Design Tokens مركزية في `core/theme/` وتُستهلك عبر `MaterialTheme` — **ممنوع** استخدام قيم Hex أو Sp مباشرة (`Color(0xFF...)`, `16.sp`) داخل أي Composable شاشة. كل قيمة تأتي من الـ Theme.

## 2. Color Tokens (Material 3 — Dynamic + Fixed Fallback)

يُبنى نظام الألوان عبر `ColorScheme` الخاص بـ Material 3، مع دعم Dynamic Color (Android 12+) وFallback ثابت لباقي الإصدارات.

| Token | القيمة (Light) | القيمة (Dark) | الاستخدام |
|---|---|---|---|
| `primary` | `#5B4FE8` (بنفسجي-أزرق حيوي) | `#B8B0FF` | الأزرار الأساسية، السهم في العجلة، العناصر النشطة |
| `secondary` | `#00C896` (أخضر مائل للفيروزي) | `#5CE0BE` | البطاقات المقلوبة، حالة "فاز" |
| `tertiary` | `#FF8A5B` | `#FFB088` | تنبيهات، شارات "جديد" |
| `error` | `#E5484D` | `#FF8A8A` | رسائل الخطأ، حذف بروفايل |
| `background` | `#FAFAFC` | `#121218` | خلفية عامة للشاشات |
| `surface` | `#FFFFFF` | `#1C1C24` | البطاقات (Bento Cards) |
| `surfaceVariant` | `#EDEDF4` | `#2A2A34` | خلفية Shimmer/Skeleton |
| `onSurface` | `#1A1A22` | `#EDEDF4` | النصوص الأساسية |
| `outline` | `#C7C7D4` | `#44444E` | الحدود الخفيفة، فواصل القوائم |

**ألوان مخصصة لأدوات القرعة** (خارج نظام Material الأساسي، معرّفة في `ExtendedColors` ضمن `Color.kt`): 6 ألوان متدرجة لتقسيمات العجلة (`wheelSegment1..6`) تُعاد دورياً إذا تجاوز عدد العناصر 6.

## 3. الخطوط (Typography)

خط أساسي: **IBM Plex Sans Arabic** (يدعم العربية بوضوح ووزن جيد للأرقام) أو **Cairo** كبديل، عبر `FontFamily` مضمّن في `res/font/` وليس اعتماد على خط النظام فقط — لضمان ثبات الشكل بين الأجهزة.

| النمط (Compose Typography Token) | الحجم | الوزن | الاستخدام |
|---|---|---|---|
| `displayMedium` | 36sp | Bold | نتيجة القرعة النهائية (اسم الفائز) |
| `headlineSmall` | 24sp | SemiBold | عناوين الشاشات (AppBar Title) |
| `titleMedium` | 18sp | Medium | عناوين بطاقات Bento |
| `bodyLarge` | 16sp | Regular | نص عادي، أسماء العناصر في القوائم |
| `bodyMedium` | 14sp | Regular | نص ثانوي، تواريخ، أوصاف |
| `labelSmall` | 12sp | Medium | شارات، Tags |

جميع النصوص يجب أن تحترم `LayoutDirection.Rtl` تلقائياً (Compose يفعل ذلك افتراضياً لواجهات عربية عند ضبط لغة النظام)، مع التحقق اليدوي من محاذاة الأيقونات في `NavigationBar` (بعض الأيقونات كالسهم يجب عكسها في RTL).

## 4. الأشكال (Shape Tokens)

يتبع التطبيق نظام انحناءات متناسق ومعتدل (Balanced Corner Radii) لتجنب الانحناء المفرط أو التشوه البيضاوي/الدائري في النوافذ والصناديق:

| Token | القيمة | الاستخدام |
|---|---|---|
| `extraSmall` | 6dp | شارات صغيرة (Chips)، عناصر Tags |
| `small` | 10dp | حقول الإدخال، الأزرار الصغيرة، الأزرار التفاعلية |
| `medium` | 14dp | بطاقات Bento العادية، بطاقات القوائم والصناديق |
| `large` | 18dp | البطاقة الكبيرة في الرئيسية، الحاويات المتوسطة |
| `extraLarge` | 22dp | النوافذ المنبثقة (AlertDialogs)، القوائم السفلية (ModalBottomSheets)، الحاويات الرئيسية الكبيرة |

> ⚠️ **قاعدة إلزامية:** يُمنع تماماً استخدام `CircleShape` أو قيم دوران مفرطة تتجاوز 24dp للحاويات الكبيرة، النوافذ (Dialogs)، أو القوائم السفلية (BottomSheets) لأنها تسبب تشوهاً بيضاوياً/دائرياً غير ملائم للمحتوى. `CircleShape` مخصص فقط للأزرار الدائرية الصغيرة (FABs) والأفاتار والشارات الدائرية.

## 5. الوضع الليلي/النهاري (Dark/Light Mode)

- الافتراضي: اتباع إعداد النظام (`isSystemInDarkTheme()`) عند أول تشغيل.
- خيار يدوي في الإعدادات: فاتح / داكن / تلقائي (نظام)، محفوظ في DataStore.
- قاعدة إلزامية: كل لون في الجدول أعلاه له نسخة Light ونسخة Dark مُعرَّفة معاً في نفس ملف `Color.kt`، لا يجوز حساب لون داكن بمعادلة يدوية أثناء التشغيل (Runtime darkening) لأنه يعطي نتائج غير متسقة.
- نسبة التباين (Contrast) بين `onSurface` وخلفية `surface` يجب أن تتجاوز 4.5:1 في كلا الوضعين (معيار WCAG AA) لضمان وضوح القراءة أثناء اللعب في إضاءة منخفضة.

## 6. الأيقونات (Vector Assets)

- جميع الأيقونات بصيغة **Vector Drawable XML** (`res/drawable/ic_*.xml`) مُصدَّرة من مكتبة Material Symbols أو مصممة يدوياً بـ Figma/Illustrator بصيغة SVG ثم تحويلها عبر Android Studio Vector Asset Studio — **ممنوع** استخدام PNG/WebP للأيقونات الوظيفية (فقط للصور الفعلية إن وُجدت مستقبلاً كشعارات مثلاً).
- مجموعة أيقونات الشريط السفلي (يجب توفر نسخة Filled ونسخة Outlined لكل واحدة لتمييز الحالة النشطة/غير النشطة):
  - `ic_home_filled.xml` / `ic_home_outlined.xml`
  - `ic_profile_filled.xml` / `ic_profile_outlined.xml`
  - `ic_tournament_filled.xml` / `ic_tournament_outlined.xml`
  - `ic_settings_filled.xml` / `ic_settings_outlined.xml`
- أيقونات أدوات القرعة: `ic_wheel.xml`, `ic_cards.xml`, `ic_spinlist.xml`, `ic_roundrobin.xml`.
- حجم موحّد 24x24dp لكل أيقونات التنقل، و32x32dp لأيقونات بطاقات Bento.

## 7. مكوّنات Shimmer/Skeleton (معايير بصرية)

- لون الخلفية الأساسي للـ Shimmer: `surfaceVariant`، مع تدرّج متحرك بشفافية بين 20% و60%.
- مدة الحركة: 800ms، `LinearEasing`، تكرار لا نهائي (`infiniteRepeatable`).
- كل Skeleton يجب أن يطابق الشكل الهندسي الحقيقي للمحتوى (نفس `Shape` Token المستخدم في البطاقة الحقيقية) — مثلاً Skeleton لبطاقة Bento يستخدم `RoundedCornerShape(medium)` نفسه.

## 8. حركات الانتقال العامة (Motion & Transitions)

- **الانتقال بين الشاشات (NavHost Screen Transitions)**:
  - **Enter Transition**: `fadeIn(tween(300, easing = FastOutSlowInEasing))` + `slideIntoContainer(SlideDirection.Start, tween(300, easing = FastOutSlowInEasing), initialOffset = { it / 5 })`
  - **Exit Transition**: `fadeOut(tween(240, easing = FastOutSlowInEasing))` + `slideOutOfContainer(SlideDirection.Start, tween(240, easing = FastOutSlowInEasing), targetOffset = { -it / 5 })`
  - **Pop Enter / Exit**: حركة عودة ناعمة ومطابقة بالاتجاه المعاكس (`SlideDirection.End`) تدعم اتجاه RTL تلقائياً.
- **التبديل بين Skeleton والمحتوى الحقيقي**: `Crossfade` بمدة 400ms.
- **حركة العجلة**: `Animatable<Float>` مع `tween(durationMillis = 2500, easing = FastOutSlowInEasing)` أو معادلة فيزيائية مخصصة تحاكي الاحتكاك التدريجي.

## 9. معايير اتجاه الواجهة والأيقونات (RTL Design Standards)

- **الفرض الشامل:** التطبيق مفروض عليه اتجاه RTL من جذر الـ Theme (`CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`).
- **الأيقونات الاتجاهية:** كل أيقونة تحتوي دلالة اتجاهية (مثل الرجوع، التقدم، الأسهم) يجب أن تُعرَّف بخاصية `android:autoMirrored="true"` في ملف الـ Vector Drawable أو تُستخدم من حزمة `Icons.AutoMirrored` لتنعكس تلقائياً حسب الاتجاه.
- **الأيقونات غير الاتجاهية:** أيقونات الأدوات العامة (الشعار، العجلة، البطاقة، القائمة) لا تتأثر بالانعكاس وتحافظ على تصميمها الأصلي.
- **عرض الأرقام والنتائج:** تُعرض النتائج الرقمية والرموز اللاتينية عبر المكوّن المخصص `LtrForcedText` لضمان عدم تشويه ترتيب الأرقام (مثل "3 - 1").
- **شجرة البطولة (Tournament Brackets):** يتم توسيط بطاقات المواجهات في منتصف الشاشة، بحيث تكون أسماء الفرق/اللاعبين محاذية بدقة لمنتصف المربع المخصص لكل مواجهة (Centered Match Alignment).

## 10. نظام الشريط السفلي الكبسولي العائم (Floating Capsule Bottom Navigation Bar)

مكوّن ملاحة رئيسي عائم ومستقل بتصميم كبسولي حديث يركز على الراحة البصرية وتفاعلات النوابض الفيزيائية المتقدمة (`RndmBottomBar.kt`).

### أ. المواصفات الهندسية والبصرية للحاوية الخارجية (Outer Capsule)

| الخاصية | القيمة / الـ Token | الغرض والوصف |
|---|---|---|
| **الشكل (Shape)** | `CircleShape` (أو `RoundedCornerShape(32.dp)`) | كبسولة بيضاوية دائرية بالكامل تعطي طابعاً عائماً |
| **التموضع (Placement)** | `Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)` | طفو سلس أعلى شريط النظام بمسافات وهوامش متناسقة |
| **الخلفية (Background)** | تدرج رأسي: `Brush.verticalGradient(listOf(#1E1E26, #121218))` | سواد داكن فخم (Obsidian Dark) بلمسة عمق |
| **الحدود (Border)** | `1.dp` مع تدرج رأسي `Brush.verticalGradient(listOf(#38384A, #1E1E28))` | إطار رقيق يبرز أطراف الكبسولة عن محتوى الشاشة خلفها |
| **الظل (Elevation Shadow)** | `elevation = 20.dp, spotColor = #8C000000, ambientColor = #59000000` | ظل ثلاثي الأبعاد يعزز الإحساس بالطفو والارتفاع |
| **المسافات الداخلية** | `Arrangement.spacedBy(6.dp)` مع `padding(horizontal = 6.dp, vertical = 6.dp)` | تباعد متوازن ومريح لحركة تمدد الأزرار |

---

### ب. حالات وتفاعل التبويبات (Tab States & Morphing)

- **التبويب النشط (Active / Selected Tab)**:
  - **الكبسولة الداخلية**: تتمدد تلقائياً بخلفية متدرجة بارزة `Brush.horizontalGradient(listOf(#363748, #2A2B38))` وحدود `0.75.dp` بلون `#4E5066` بشكل `CircleShape`.
  - **الهوامش الداخلية**: `horizontal = 16.dp, vertical = 10.dp`.
  - **الأيقونة**: النسخة الممتلئة (`filledIcon`) بلون أبيض `#FFFFFF` بحجم `22.dp` مع تكبير نسبي `scale(1.08f)`.
  - **النص**: يظهر بسلاسة بنمط `labelMedium` (`FontWeight.SemiBold`, `fontSize = 13.sp`) بلون أبيض ناصع `#FFFFFF`.
- **التبويب غير النشط (Inactive Tab)**:
  - **الخلفية**: شفافة `Color.Transparent` بدون حدود.
  - **الهوامش الداخلية**: `horizontal = 12.dp, vertical = 10.dp`.
  - **الأيقونة**: النسخة المفرغة (`outlinedIcon`) بلون رمادي هادئ `#888898` بحجم `22.dp` وبمقياس `scale(0.95f)`.
- **الاستجابة اللمسية (Haptic Feedback)**:
  - تفعيل استجابة لمسية فورية `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` عند النقر لتعزيز الإحساس الملموس بالاختيار.

---

### ج. المعايير الفيزيائية للحركة (Motion Physics & Spring Spec)

```kotlin
// تمدد وتقلص كبسولة العنصر والتبويب
animateContentSize(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy, // 0.75f لمرونة طبيعية
        stiffness = Spring.StiffnessMediumLow        // 400f لتفاعل ناعم بدون تأخير
    )
)

// ارتداد وتكبير الأيقونة
val iconScale by animateFloatAsState(
    targetValue = if (isSelected) 1.08f else 0.95f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)

// ظهور واختفاء نص التبويب
AnimatedVisibility(
    visible = isSelected,
    enter = fadeIn(tween(220, delayMillis = 50)) +
            expandHorizontally(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Start
            ),
    exit = fadeOut(tween(140)) +
           shrinkHorizontally(
               animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
               shrinkTowards = Alignment.Start
           )
)
```

---

### د. تكامل الملاحة والـ Scaffold

- في `RndmNavHost.kt`، يتم ضبط الـ Scaffold الرئيسي على:
  - `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
  - إسناد الحشو السفلي للمحتوى بدقة: `Modifier.padding(bottom = innerPadding.calculateBottomPadding())`
  - يضمن هذا التكوين عدم تغطية المحتوى أو الأزرار العائمة (FABs) مع إتاحة تدفق الواجهة تحت شريط الحالة والناف بار بشكل كامل وجميل.



