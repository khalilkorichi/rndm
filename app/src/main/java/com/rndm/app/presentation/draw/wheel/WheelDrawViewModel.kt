package com.rndm.app.presentation.draw.wheel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.DrawFixture
import com.rndm.app.domain.model.DrawResult
import com.rndm.app.domain.model.DrawType
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.ProfileItem
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.usecase.profile.GetAllProfilesUseCase
import com.rndm.app.domain.usecase.profile.GetProfileByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WheelDrawViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val drawFixtureRepository: DrawFixtureRepository,
    private val randomProvider: RandomProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialProfileId: Long = savedStateHandle.get<Long>("profileId") ?: 0L

    private val _uiState = MutableStateFlow(WheelDrawUiState(isLoading = true, isSpinning = false))
    val uiState: StateFlow<WheelDrawUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
        observeFixtures()
        observePendingNewPlayers()
    }

    fun initializeWithProfileId(profileId: Long) {
        if (profileId <= 0L) return
        viewModelScope.launch {
            val profile = getProfileByIdUseCase(profileId)
            if (profile != null) {
                when (profile.type) {
                    ProfileType.PLAYERS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.PLAYERS,
                                selectedPlayersProfile = profile,
                                remainingPlayers = profile.items,
                                isSpinning = false,
                                targetRotation = 0f
                            )
                        }
                    }
                    ProfileType.CLUBS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.CLUBS,
                                selectedClubsProfile = profile,
                                remainingClubs = profile.items,
                                isSpinning = false,
                                targetRotation = 0f
                            )
                        }
                    }
                    ProfileType.NATIONAL_TEAMS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.NATIONAL_TEAMS,
                                selectedNationalTeamsProfile = profile,
                                remainingNationalTeams = profile.items,
                                isSpinning = false,
                                targetRotation = 0f
                            )
                        }
                    }
                }
                updatePrompt()
            }
        }
    }

    private fun observeProfiles() {
        getAllProfilesUseCase()
            .onEach { profiles ->
                val players = profiles.filter { it.type == ProfileType.PLAYERS }
                val clubs = profiles.filter { it.type == ProfileType.CLUBS }
                val teams = profiles.filter { it.type == ProfileType.NATIONAL_TEAMS }

                _uiState.update { current ->
                    val selPlayer = current.selectedPlayersProfile ?: players.firstOrNull { it.id == initialProfileId } ?: players.firstOrNull()
                    val selClub = current.selectedClubsProfile ?: clubs.firstOrNull { it.id == initialProfileId } ?: clubs.firstOrNull()
                    val selTeam = current.selectedNationalTeamsProfile ?: teams.firstOrNull { it.id == initialProfileId } ?: teams.firstOrNull()

                    val remPlayers = if (current.remainingPlayers.isEmpty() && selPlayer != null) selPlayer.items else current.remainingPlayers
                    val remClubs = if (current.remainingClubs.isEmpty() && selClub != null) selClub.items else current.remainingClubs
                    val remTeams = if (current.remainingNationalTeams.isEmpty() && selTeam != null) selTeam.items else current.remainingNationalTeams

                    current.copy(
                        isLoading = false,
                        playersProfiles = players,
                        clubsProfiles = clubs,
                        nationalTeamsProfiles = teams,
                        selectedPlayersProfile = selPlayer,
                        selectedClubsProfile = selClub,
                        selectedNationalTeamsProfile = selTeam,
                        remainingPlayers = remPlayers,
                        remainingClubs = remClubs,
                        remainingNationalTeams = remTeams,
                        isSpinning = false
                    )
                }
                updatePrompt()
            }
            .launchIn(viewModelScope)
    }

    private fun observeFixtures() {
        drawFixtureRepository.fixtures
            .onEach { fixtures ->
                _uiState.update { it.copy(fixtures = fixtures) }
                updatePrompt()
            }
            .launchIn(viewModelScope)
    }

    private fun observePendingNewPlayers() {
        drawFixtureRepository.pendingNewPlayers
            .onEach { pending ->
                if (pending.isNotEmpty()) {
                    val consumed = drawFixtureRepository.consumePendingNewPlayers()
                    onAddNewPlayers(consumed)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCategorySelect(category: DrawCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                targetRotation = 0f,
                isSpinning = false,
                selectedIndex = -1
            )
        }
        updatePrompt()
    }

    fun onSelectProfileForCategory(category: DrawCategory, profile: Profile) {
        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(selectedPlayersProfile = profile, remainingPlayers = profile.items)
                DrawCategory.CLUBS -> current.copy(selectedClubsProfile = profile, remainingClubs = profile.items)
                DrawCategory.NATIONAL_TEAMS -> current.copy(selectedNationalTeamsProfile = profile, remainingNationalTeams = profile.items)
            }
        }
        updatePrompt()
    }

    private fun updatePrompt() {
        val state = _uiState.value
        val fixtures = state.fixtures

        val prompt = when (state.selectedCategory) {
            DrawCategory.PLAYERS -> {
                if (state.remainingPlayers.isEmpty()) {
                    if (fixtures.isEmpty()) "أضف لاعبين لبدء القرعة"
                    else "تم سحب وتوليد جميع المباريات (${fixtures.size} مباراة)"
                } else if (state.remainingPlayers.size == 1) {
                    val single = state.remainingPlayers.first().label
                    val lastFixture = fixtures.lastOrNull()
                    if (lastFixture != null && lastFixture.playerTwoName == null) {
                        "اللاعب المتبقي الوحيد [$single] سيكون منافس [${lastFixture.playerOneName}] في المباراة ${lastFixture.matchNumber}"
                    } else {
                        "اللاعب المتبقي الوحيد [$single] للمباراة رقم ${fixtures.size + 1}"
                    }
                } else {
                    val lastFixture = fixtures.lastOrNull()
                    if (lastFixture == null || lastFixture.playerTwoName != null) {
                        "سحب اللاعب الأول للمباراة رقم ${fixtures.size + 1}"
                    } else {
                        "سحب منافس [${lastFixture.playerOneName}] للمباراة رقم ${fixtures.size}"
                    }
                }
            }
            DrawCategory.CLUBS -> {
                if (state.remainingClubs.isEmpty()) {
                    "تم سحب جميع الأندية المتاحة"
                } else if (state.remainingClubs.size == 1) {
                    val single = state.remainingClubs.first().label
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "النادي المتبقي الوحيد [$single] سيُعيّن مباشرة لـ: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين الأندية لجميع اللاعبين في المباريات"
                    }
                } else {
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "سحب نادي اللاعب: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين الأندية لجميع اللاعبين في المباريات"
                    }
                }
            }
            DrawCategory.NATIONAL_TEAMS -> {
                if (state.remainingNationalTeams.isEmpty()) {
                    "تم سحب جميع المنتخبات المتاحة"
                } else if (state.remainingNationalTeams.size == 1) {
                    val single = state.remainingNationalTeams.first().label
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "المنتخب المتبقي الوحيد [$single] سيُعيّن مباشرة لـ: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين المنتخبات لجميع اللاعبين في المباريات"
                    }
                } else {
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "سحب منتخب اللاعب: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين المنتخبات لجميع اللاعبين في المباريات"
                    }
                }
            }
        }
        _uiState.update { it.copy(currentDrawingPrompt = prompt) }
    }

    private fun getNextPlayerNeedingTeam(fixtures: List<DrawFixture>): Pair<String, String>? {
        for (f in fixtures) {
            if (f.playerOneTeam == null) {
                return Pair(f.playerOneName, "المباراة ${f.matchNumber}")
            }
            if (f.playerTwoName != null && f.playerTwoTeam == null) {
                return Pair(f.playerTwoName, "المباراة ${f.matchNumber}")
            }
        }
        return null
    }

    fun startSpin() {
        val state = _uiState.value
        val items = state.currentWheelItems
        if (state.isSpinning || items.isEmpty()) return

        // إذا كان هناك اسم واحد متبقي فقط، يتم اختياره مباشرة كفائز بدون تدوير العجلة
        if (items.size == 1) {
            applyDrawnItem(items.first())
            return
        }

        val selectedIndex = randomProvider.nextInt(items.size)
        val sliceAngle = 360f / items.size
        val baseRotations = 360f * 6
        val targetAngle = baseRotations + (360f - (selectedIndex * sliceAngle + sliceAngle / 2f))

        _uiState.update {
            it.copy(
                isSpinning = true,
                selectedIndex = selectedIndex,
                targetRotation = targetAngle,
                spinTrigger = it.spinTrigger + 1
            )
        }
    }

    fun onSpinComplete() {
        val state = _uiState.value
        val items = state.currentWheelItems
        if (state.selectedIndex !in items.indices) {
            _uiState.update { it.copy(isSpinning = false, targetRotation = 0f) }
            return
        }

        val drawnItem = items[state.selectedIndex]
        applyDrawnItem(drawnItem)
    }

    private fun applyDrawnItem(drawnItem: ProfileItem) {
        val state = _uiState.value
        val fixtures = state.fixtures.toMutableList()

        when (state.selectedCategory) {
            DrawCategory.PLAYERS -> {
                val remPlayers = state.remainingPlayers.filter { it.id != drawnItem.id }
                val lastFixture = fixtures.lastOrNull()

                if (lastFixture != null && lastFixture.playerTwoName == null) {
                    val updated = lastFixture.copy(playerTwoName = drawnItem.label)
                    fixtures[fixtures.size - 1] = updated
                } else {
                    fixtures.add(
                        DrawFixture(
                            matchNumber = fixtures.size + 1,
                            playerOneName = drawnItem.label
                        )
                    )
                }

                drawFixtureRepository.setFixtures(fixtures)
                _uiState.update {
                    it.copy(
                        isSpinning = false,
                        selectedIndex = -1,
                        targetRotation = 0f,
                        remainingPlayers = remPlayers,
                        fixtures = fixtures,
                        drawResult = DrawResult(drawType = DrawType.WHEEL, selectedItem = drawnItem)
                    )
                }
            }
            DrawCategory.CLUBS, DrawCategory.NATIONAL_TEAMS -> {
                val isClubs = state.selectedCategory == DrawCategory.CLUBS
                val remItems = if (isClubs) {
                    state.remainingClubs.filter { it.id != drawnItem.id }
                } else {
                    state.remainingNationalTeams.filter { it.id != drawnItem.id }
                }

                // Assign team to next unassigned player in fixtures
                var assigned = false
                for (i in fixtures.indices) {
                    val f = fixtures[i]
                    if (f.playerOneTeam == null) {
                        fixtures[i] = f.copy(playerOneTeam = drawnItem.label)
                        assigned = true
                        break
                    }
                    if (f.playerTwoName != null && f.playerTwoTeam == null) {
                        fixtures[i] = f.copy(playerTwoTeam = drawnItem.label)
                        assigned = true
                        break
                    }
                }

                if (assigned) {
                    drawFixtureRepository.setFixtures(fixtures)
                }

                _uiState.update { current ->
                    if (isClubs) {
                        current.copy(
                            isSpinning = false,
                            selectedIndex = -1,
                            targetRotation = 0f,
                            remainingClubs = remItems,
                            fixtures = fixtures,
                            drawResult = DrawResult(drawType = DrawType.WHEEL, selectedItem = drawnItem)
                        )
                    } else {
                        current.copy(
                            isSpinning = false,
                            selectedIndex = -1,
                            targetRotation = 0f,
                            remainingNationalTeams = remItems,
                            fixtures = fixtures,
                            drawResult = DrawResult(drawType = DrawType.WHEEL, selectedItem = drawnItem)
                        )
                    }
                }
            }
        }
        updatePrompt()
    }

    fun excludeItem(category: DrawCategory, item: ProfileItem) {
        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = current.remainingPlayers.filter { it.id != item.id || it.label != item.label }
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = current.remainingClubs.filter { it.id != item.id || it.label != item.label }
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = current.remainingNationalTeams.filter { it.id != item.id || it.label != item.label }
                )
            }
        }
        updatePrompt()
    }

    fun onRequestReplacePlayer(playerName: String, clubName: String? = null) {
        _uiState.update {
            it.copy(
                playerToReplace = playerName,
                playerToReplaceClub = clubName
            )
        }
    }

    fun onDismissReplacePlayerDialog() {
        _uiState.update {
            it.copy(
                playerToReplace = null,
                playerToReplaceClub = null
            )
        }
    }

    fun onConfirmReplacePlayer(newPlayerName: String, newClubName: String?) {
        val oldPlayerName = _uiState.value.playerToReplace ?: return
        drawFixtureRepository.replacePlayer(oldPlayerName, newPlayerName, newClubName)

        _uiState.update { current ->
            val updatedRemainingPlayers = current.remainingPlayers.map { item ->
                if (item.label == oldPlayerName) item.copy(label = newPlayerName) else item
            }
            current.copy(
                playerToReplace = null,
                playerToReplaceClub = null,
                remainingPlayers = updatedRemainingPlayers
            )
        }
        updatePrompt()
    }

    fun onOpenAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = true) }
    }

    fun onDismissAddPlayersDialog() {
        _uiState.update { it.copy(isAddPlayersDialogOpen = false) }
    }

    fun onAddNewPlayers(names: List<String>) {
        if (names.isEmpty()) return

        val state = _uiState.value
        val newPlayerItems = names.mapIndexed { idx, name ->
            ProfileItem(
                id = System.currentTimeMillis() + idx,
                label = name
            )
        }

        val updatedRemainingPlayers = state.remainingPlayers + newPlayerItems

        // Refresh remaining unassigned clubs from selected clubs profile
        val assignedClubs = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val unassignedClubs = (state.selectedClubsProfile?.items ?: emptyList()).filter { it.label !in assignedClubs }
        val updatedRemainingClubs = if (state.remainingClubs.isNotEmpty()) {
            (state.remainingClubs + unassignedClubs).distinctBy { it.label }
        } else {
            unassignedClubs
        }

        // Refresh remaining unassigned national teams from selected national teams profile
        val assignedTeams = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val unassignedTeams = (state.selectedNationalTeamsProfile?.items ?: emptyList()).filter { it.label !in assignedTeams }
        val updatedRemainingTeams = if (state.remainingNationalTeams.isNotEmpty()) {
            (state.remainingNationalTeams + unassignedTeams).distinctBy { it.label }
        } else {
            unassignedTeams
        }

        _uiState.update {
            it.copy(
                isAddPlayersDialogOpen = false,
                remainingPlayers = updatedRemainingPlayers,
                remainingClubs = updatedRemainingClubs,
                remainingNationalTeams = updatedRemainingTeams,
                selectedCategory = DrawCategory.PLAYERS,
                isSpinning = false,
                selectedIndex = -1,
                targetRotation = 0f
            )
        }
        updatePrompt()
    }

    fun resetDraw() {
        val state = _uiState.value
        val defaultPlayers = state.selectedPlayersProfile?.items
            ?: state.playersProfiles.firstOrNull()?.items
            ?: emptyList()
        val defaultClubs = state.selectedClubsProfile?.items
            ?: state.clubsProfiles.firstOrNull()?.items
            ?: emptyList()
        val defaultTeams = state.selectedNationalTeamsProfile?.items
            ?: state.nationalTeamsProfiles.firstOrNull()?.items
            ?: emptyList()

        drawFixtureRepository.clearFixtures()
        _uiState.update {
            it.copy(
                remainingPlayers = defaultPlayers,
                remainingClubs = defaultClubs,
                remainingNationalTeams = defaultTeams,
                fixtures = emptyList(),
                targetRotation = 0f,
                isSpinning = false,
                selectedIndex = -1,
                drawResult = null,
                playerToReplace = null,
                playerToReplaceClub = null,
                isAddPlayersDialogOpen = false
            )
        }
        updatePrompt()
    }
}
