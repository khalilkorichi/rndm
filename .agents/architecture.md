# architecture.md — الهيكلة البرمجية لمشروع RNDM

## 1. مبدأ عام

Clean Architecture بثلاث طبقات، مع اتجاه اعتماد واحد فقط: `presentation → domain ← data`. طبقة `domain` لا تعرف بوجود Room أو Android SDK أو Compose نهائياً — هي كوتلن نقي (Pure Kotlin Module قابل حتى للفصل كـ Gradle module مستقل لاحقاً).

## 2. شجرة الحزم الكاملة (يجب إنشاؤها بالكامل في هذه المرحلة، حتى الفارغة)

```
com.rndm.app/
│
├── core/
│   ├── di/
│   │   ├── DatabaseModule.kt
│   │   ├── RepositoryModule.kt
│   │   └── DispatcherModule.kt
│   ├── navigation/
│   │   ├── Destination.kt              (sealed interface لكل الوجهات)
│   │   ├── RndmNavHost.kt
│   │   └── BottomNavItem.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   ├── Shape.kt
│   │   └── RndmTheme.kt
│   ├── ui/
│   │   ├── components/
│   │   │   ├── ShimmerBox.kt
│   │   │   ├── BentoCard.kt
│   │   │   ├── ConfirmDialog.kt
│   │   │   ├── EmptyState.kt
│   │   │   └── RndmButton.kt
│   │   └── modifier/
│   │       └── ShimmerModifier.kt
│   ├── util/
│   │   ├── Constants.kt
│   │   ├── RandomProvider.kt            (واجهة عشوائية قابلة للاختبار/المحاكاة)
│   │   └── Result.kt                    (sealed class: Success/Error/Loading)
│   └── extensions/
│       └── FlowExtensions.kt
│
├── data/
│   ├── local/
│   │   ├── RndmDatabase.kt
│   │   ├── entity/
│   │   │   ├── ProfileEntity.kt
│   │   │   ├── ProfileItemEntity.kt
│   │   │   ├── MatchEntity.kt                  (placeholder فارغ للمرحلة 2)
│   │   │   └── TournamentEntity.kt             (placeholder فارغ للمرحلة 2)
│   │   └── dao/
│   │       ├── ProfileDao.kt
│   │       ├── ProfileItemDao.kt
│   │       ├── MatchDao.kt                     (placeholder فارغ للمرحلة 2)
│   │       └── TournamentDao.kt                (placeholder فارغ للمرحلة 2)
│   ├── mapper/
│   │   ├── ProfileMapper.kt
│   │   └── ProfileItemMapper.kt
│   └── repository/
│       ├── ProfileRepositoryImpl.kt
│       └── DrawRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Profile.kt
│   │   ├── ProfileItem.kt
│   │   ├── ProfileType.kt               (enum: PLAYERS, CLUBS)
│   │   ├── DrawType.kt                  (enum: WHEEL, FLIP_CARDS, SPIN_LIST, ROUND_ROBIN)
│   │   ├── DrawResult.kt
│   │   └── MatchPairing.kt
│   ├── repository/
│   │   ├── ProfileRepository.kt         (interface)
│   │   └── DrawRepository.kt            (interface)
│   └── usecase/
│       ├── profile/
│       │   ├── CreateProfileUseCase.kt
│       │   ├── UpdateProfileUseCase.kt
│       │   ├── DeleteProfileUseCase.kt
│       │   ├── DuplicateProfileUseCase.kt
│       │   ├── GetAllProfilesUseCase.kt
│       │   └── GetProfileByIdUseCase.kt
│       └── draw/
│           ├── PerformWheelDrawUseCase.kt
│           ├── PerformFlipCardDrawUseCase.kt
│           ├── PerformSpinListDrawUseCase.kt
│           └── GenerateRoundRobinPairingsUseCase.kt
│
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── HomeUiState.kt
│   │   └── components/
│   │       └── QuickDrawCard.kt
│   ├── profile/
│   │   ├── list/
│   │   │   ├── ProfileListScreen.kt
│   │   │   ├── ProfileListViewModel.kt
│   │   │   └── ProfileListUiState.kt
│   │   ├── edit/
│   │   │   ├── CreateEditProfileScreen.kt
│   │   │   ├── CreateEditProfileViewModel.kt
│   │   │   └── CreateEditProfileUiState.kt
│   │   └── detail/
│   │       ├── ProfileDetailScreen.kt
│   │       └── ProfileDetailViewModel.kt
│   ├── draw/
│   │   ├── setup/
│   │   │   ├── DrawSetupScreen.kt
│   │   │   └── DrawSetupViewModel.kt
│   │   ├── wheel/
│   │   │   ├── WheelDrawScreen.kt
│   │   │   └── WheelDrawViewModel.kt
│   │   ├── flipcards/
│   │   │   ├── FlipCardDrawScreen.kt
│   │   │   └── FlipCardDrawViewModel.kt
│   │   ├── spinlist/
│   │   │   ├── SpinListDrawScreen.kt
│   │   │   └── SpinListDrawViewModel.kt
│   │   └── result/
│   │       ├── DrawResultScreen.kt
│   │       └── DrawResultViewModel.kt
│   ├── tournament/                              (placeholder — منطق كامل في المرحلة 2)
│   │   └── list/
│   │       ├── TournamentListScreen.kt
│   │       └── TournamentListViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   └── SettingsUiState.kt
│   └── statistics/                              (placeholder فارغ — المرحلة 3)
│       └── README_PLACEHOLDER.kt
│
└── RndmApplication.kt   (@HiltAndroidApp)
```

## 3. نماذج البيانات الأساسية (Domain Models)

```kotlin
// domain/model/ProfileType.kt
enum class ProfileType { PLAYERS, CLUBS }

// domain/model/Profile.kt
@Immutable
data class Profile(
    val id: Long = 0,
    val name: String,
    val type: ProfileType,
    val items: List<ProfileItem> = emptyList(),
    val createdAt: Long,
    val lastUsedAt: Long?
)

// domain/model/ProfileItem.kt
@Immutable
data class ProfileItem(
    val id: Long = 0,
    val profileId: Long,
    val label: String,
    val order: Int
)

// domain/model/DrawType.kt
enum class DrawType { WHEEL, FLIP_CARDS, SPIN_LIST, ROUND_ROBIN }

// domain/model/DrawResult.kt
@Immutable
data class DrawResult(
    val drawType: DrawType,
    val selectedItem: ProfileItem?,          // للعجلة/البطاقات/القائمة
    val pairings: List<MatchPairing> = emptyList(), // للإقران فقط
    val timestamp: Long
)

// domain/model/MatchPairing.kt
@Immutable
data class MatchPairing(
    val playerOne: ProfileItem,
    val playerTwo: ProfileItem?              // null يعني "استراحة" (Bye) عند عدد فردي
)
```

## 4. كيانات قاعدة البيانات (Room Entities)

```kotlin
// data/local/entity/ProfileEntity.kt
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,          // اسم enum كنص
    val createdAt: Long,
    val lastUsedAt: Long?
)

// data/local/entity/ProfileItemEntity.kt
@Entity(
    tableName = "profile_items",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class ProfileItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val label: String,
    val order: Int
)

// data/local/dao/ProfileDao.kt — نموذج الاستعلام المركّب
data class ProfileWithItems(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val items: List<ProfileItemEntity>
)

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles ORDER BY lastUsedAt DESC")
    fun getAllProfilesWithItems(): Flow<List<ProfileWithItems>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileWithItems(id: Long): ProfileWithItems?

    @Insert suspend fun insertProfile(profile: ProfileEntity): Long
    @Update suspend fun updateProfile(profile: ProfileEntity)
    @Delete suspend fun deleteProfile(profile: ProfileEntity)
}
```

## 5. عقد الـ Repository (Domain Interface + Data Impl)

```kotlin
// domain/repository/ProfileRepository.kt
interface ProfileRepository {
    fun observeAllProfiles(): Flow<List<Profile>>
    suspend fun getProfileById(id: Long): Profile?
    suspend fun createProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile)
    suspend fun deleteProfile(profileId: Long)
    suspend fun duplicateProfile(profileId: Long, newName: String): Long
}
```

`ProfileRepositoryImpl` في طبقة `data` ينفّذ هذا الـ interface، يستدعي `ProfileDao`، ويحوّل `ProfileWithItems` (Room) إلى `Profile` (Domain) عبر `ProfileMapper.kt` — الـ ViewModel لا يرى أبداً كائنات Room مباشرة.

## 6. مثال UseCase نظيف (مسؤولية واحدة)

```kotlin
// domain/usecase/draw/GenerateRoundRobinPairingsUseCase.kt
class GenerateRoundRobinPairingsUseCase @Inject constructor(
    private val randomProvider: RandomProvider
) {
    operator fun invoke(items: List<ProfileItem>): List<MatchPairing> {
        val shuffled = randomProvider.shuffle(items)
        return shuffled.chunked(2).map { pair ->
            MatchPairing(playerOne = pair[0], playerTwo = pair.getOrNull(1))
        }
    }
}
```

`RandomProvider` هو interface بسيط في `core/util/` (وليس `kotlin.random.Random` مباشرة) لجعل الخوارزمية قابلة للاختبار الوحدوي بذرة ثابتة (deterministic testing).

## 7. Navigation — Destination موحّد

```kotlin
// core/navigation/Destination.kt
sealed interface Destination {
    @Serializable data object Home : Destination
    @Serializable data object ProfileList : Destination
    @Serializable data class ProfileDetail(val profileId: Long) : Destination
    @Serializable data object CreateEditProfile : Destination
    @Serializable data class DrawSetup(val profileId: Long) : Destination
    @Serializable data class Draw(val profileId: Long, val drawType: DrawType) : Destination
    @Serializable data object DrawResult : Destination
    @Serializable data object TournamentList : Destination
    @Serializable data object Settings : Destination
}
```

## 8. قواعد إلزامية إضافية للوكيل عند التنفيذ

- لا تُنشئ أي `ViewModel` بدون `HiltViewModel` annotation و constructor injection.
- كل `UseCase` يُستدعى فقط من `ViewModel`، لا يُستدعى مباشرة من Composable أبداً.
- ملفات `MatchEntity.kt`, `TournamentEntity.kt`, `MatchDao.kt`, `TournamentDao.kt`, و`statistics/README_PLACEHOLDER.kt` تُنشأ فارغة بمحتوى تعليق `// TODO: Phase 2 — Tournament groups & statistics logic` فقط — لا منطق فعلي فيها الآن، فقط لضمان اكتمال الهيكل.
