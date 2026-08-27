package com.rndm.app.presentation.draw.flipcards

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
import com.rndm.app.domain.usecase.profile.UpdateItemActiveStateUseCase
import com.rndm.app.presentation.draw.wheel.DrawCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlipCardDrawViewModel @Inject constructor(
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val updateItemActiveStateUseCase: UpdateItemActiveStateUseCase,
    private val drawFixtureRepository: DrawFixtureRepository,
    private val randomProvider: RandomProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialProfileId: Long = savedStateHandle.get<Long>("profileId") ?: 0L

    private val _uiState = MutableStateFlow(FlipCardDrawUiState(isLoading = true))
    val uiState: StateFlow<FlipCardDrawUiState> = _uiState.asStateFlow()

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
                val activeItems = profile.activeItems.ifEmpty { profile.items }
                val excludedItems = if (profile.activeItems.isEmpty()) emptyList() else profile.excludedItems

                when (profile.type) {
                    ProfileType.PLAYERS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.PLAYERS,
                                selectedPlayersProfile = profile,
                                remainingPlayers = activeItems,
                                excludedPlayers = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false
                            )
                        }
                    }
                    ProfileType.CLUBS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.CLUBS,
                                selectedClubsProfile = profile,
                                remainingClubs = activeItems,
                                excludedClubs = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false
                            )
                        }
                    }
                    ProfileType.NATIONAL_TEAMS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.NATIONAL_TEAMS,
                                selectedNationalTeamsProfile = profile,
                                remainingNationalTeams = activeItems,
                                excludedNationalTeams = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false
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

                    val remPlayers = if (current.remainingPlayers.isEmpty() && selPlayer != null) {
                        selPlayer.activeItems.ifEmpty { selPlayer.items }
                    } else current.remainingPlayers

                    val remClubs = if (current.remainingClubs.isEmpty() && selClub != null) {
                        selClub.activeItems.ifEmpty { selClub.items }
                    } else current.remainingClubs

                    val remTeams = if (current.remainingNationalTeams.isEmpty() && selTeam != null) {
                        selTeam.activeItems.ifEmpty { selTeam.items }
                    } else current.remainingNationalTeams

                    val exclPlayers = if (current.excludedPlayers.isEmpty() && selPlayer != null) selPlayer.excludedItems else current.excludedPlayers
                    val exclClubs = if (current.excludedClubs.isEmpty() && selClub != null) selClub.excludedItems else current.excludedClubs
                    val exclTeams = if (current.excludedNationalTeams.isEmpty() && selTeam != null) selTeam.excludedItems else current.excludedNationalTeams

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
                        excludedPlayers = exclPlayers,
                        excludedClubs = exclClubs,
                        excludedNationalTeams = exclTeams
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
                flippedCardIndex = -1,
                isRevealing = false
            )
        }
        updatePrompt()
    }

    fun onShuffleCards() {
        val state = _uiState.value
        if (state.isRevealing) return

        _uiState.update { current ->
            when (current.selectedCategory) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = randomProvider.shuffle(current.remainingPlayers),
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = randomProvider.shuffle(current.remainingClubs),
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = randomProvider.shuffle(current.remainingNationalTeams),
                    flippedCardIndex = -1
                )
            }
        }
    }

    fun onCardClick(cardIndex: Int) {
        val state = _uiState.value
        val items = state.currentCardsItems
        if (state.isRevealing || items.isEmpty() || cardIndex !in items.indices) return

        // Pick item for this card slot
        val drawnItem = items[cardIndex]

        viewModelScope.launch {
            // 1. Reveal card
            _uiState.update {
                it.copy(
                    flippedCardIndex = cardIndex,
                    isRevealing = true,
                    drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                )
            }

            // 2. 1.2s celebration delay
            delay(1200)

            // 3. Apply to fixtures & consume card
            applyDrawnItem(drawnItem)

            _uiState.update {
                it.copy(
                    flippedCardIndex = -1,
                    isRevealing = false
                )
            }

            // 4. If only 1 item remains in the category, auto-assign it seamlessly
            checkAutoAssignSingleRemaining()
        }
    }

    private fun checkAutoAssignSingleRemaining() {
        val state = _uiState.value
        val items = state.currentCardsItems
        if (items.size == 1) {
            viewModelScope.launch {
                delay(600)
                val lastItem = items.first()
                _uiState.update {
                    it.copy(
                        flippedCardIndex = 0,
                        isRevealing = true,
                        drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = lastItem)
                    )
                }
                delay(1200)
                applyDrawnItem(lastItem)
                _uiState.update {
                    it.copy(
                        flippedCardIndex = -1,
                        isRevealing = false
                    )
                }
            }
        }
    }

    private fun applyDrawnItem(drawnItem: ProfileItem) {
        val state = _uiState.value
        val fixtures = state.fixtures.toMutableList()

        when (state.selectedCategory) {
            DrawCategory.PLAYERS -> {
                val remPlayers = state.remainingPlayers.filter { it.id != drawnItem.id && it.label != drawnItem.label }
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
                        remainingPlayers = remPlayers,
                        fixtures = fixtures,
                        drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                    )
                }
            }
            DrawCategory.CLUBS, DrawCategory.NATIONAL_TEAMS -> {
                val isClubs = state.selectedCategory == DrawCategory.CLUBS
                val remItems = if (isClubs) {
                    state.remainingClubs.filter { it.id != drawnItem.id && it.label != drawnItem.label }
                } else {
                    state.remainingNationalTeams.filter { it.id != drawnItem.id && it.label != drawnItem.label }
                }

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
                            remainingClubs = remItems,
                            fixtures = fixtures,
                            drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                        )
                    } else {
                        current.copy(
                            remainingNationalTeams = remItems,
                            fixtures = fixtures,
                            drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                        )
                    }
                }
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
                    if (fixtures.isEmpty()) "أضف لاعبين لبدء سحب البطاقات"
                    else "تم سحب وتوليد جميع المباريات (${fixtures.size} مباراة)"
                } else if (state.remainingPlayers.size == 1) {
                    val single = state.remainingPlayers.first().label
                    val lastFixture = fixtures.lastOrNull()
                    if (lastFixture != null && lastFixture.playerTwoName == null) {
                        "اللاعب المتبقي الوحيد [$single] سيكون منافس [${lastFixture.playerOneName}] في المباراة #${lastFixture.matchNumber}"
                    } else {
                        "اللاعب المتبقي الوحيد [$single] للمباراة #${fixtures.size + 1}"
                    }
                } else {
                    val lastFixture = fixtures.lastOrNull()
                    if (lastFixture == null || lastFixture.playerTwoName != null) {
                        "اختر بطاقة لكشف اللاعب الأول للمباراة #${fixtures.size + 1}"
                    } else {
                        "اختر بطاقة لكشف منافس [${lastFixture.playerOneName}] للمباراة #${fixtures.size}"
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
                        "النادي المتبقي [$single] سيُعيّن لـ: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين الأندية لجميع اللاعبين في المباريات"
                    }
                } else {
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "اختر بطاقة لتحديد نادي: [${nextPlayer.first}] (${nextPlayer.second})"
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
                        "المنتخب المتبقي [$single] سيُعيّن لـ: [${nextPlayer.first}] (${nextPlayer.second})"
                    } else {
                        "تم تعيين المنتخبات لجميع اللاعبين في المباريات"
                    }
                } else {
                    val nextPlayer = getNextPlayerNeedingTeam(fixtures)
                    if (nextPlayer != null) {
                        "اختر بطاقة لتحديد منتخب: [${nextPlayer.first}] (${nextPlayer.second})"
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
                return Pair(f.playerOneName, "المباراة #${f.matchNumber}")
            }
            if (f.playerTwoName != null && f.playerTwoTeam == null) {
                return Pair(f.playerTwoName, "المباراة #${f.matchNumber}")
            }
        }
        return null
    }

    fun onOpenExcludeDialog() {
        _uiState.update { it.copy(isExcludeDialogOpen = true) }
    }

    fun onDismissExcludeDialog() {
        _uiState.update { it.copy(isExcludeDialogOpen = false) }
    }

    fun excludeItem(category: DrawCategory, item: ProfileItem) {
        val state = _uiState.value
        val profileId = when (category) {
            DrawCategory.PLAYERS -> state.selectedPlayersProfile?.id ?: 0L
            DrawCategory.CLUBS -> state.selectedClubsProfile?.id ?: 0L
            DrawCategory.NATIONAL_TEAMS -> state.selectedNationalTeamsProfile?.id ?: 0L
        }

        viewModelScope.launch {
            if (item.id > 0L) {
                updateItemActiveStateUseCase.setItemActive(item.id, false)
            } else if (profileId > 0L) {
                updateItemActiveStateUseCase.setItemActiveByLabel(profileId, item.label, false)
            }
        }

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = current.remainingPlayers.filter { it.id != item.id || it.label != item.label },
                    excludedPlayers = (current.excludedPlayers + item.copy(isActive = false)).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = current.remainingClubs.filter { it.id != item.id || it.label != item.label },
                    excludedClubs = (current.excludedClubs + item.copy(isActive = false)).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = current.remainingNationalTeams.filter { it.id != item.id || it.label != item.label },
                    excludedNationalTeams = (current.excludedNationalTeams + item.copy(isActive = false)).distinctBy { it.label },
                    flippedCardIndex = -1
                )
            }
        }
        updatePrompt()
    }

    fun restoreExcludedItem(category: DrawCategory, item: ProfileItem) {
        val state = _uiState.value
        val profileId = when (category) {
            DrawCategory.PLAYERS -> state.selectedPlayersProfile?.id ?: 0L
            DrawCategory.CLUBS -> state.selectedClubsProfile?.id ?: 0L
            DrawCategory.NATIONAL_TEAMS -> state.selectedNationalTeamsProfile?.id ?: 0L
        }

        viewModelScope.launch {
            if (item.id > 0L) {
                updateItemActiveStateUseCase.setItemActive(item.id, true)
            } else if (profileId > 0L) {
                updateItemActiveStateUseCase.setItemActiveByLabel(profileId, item.label, true)
            }
        }

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = (current.remainingPlayers + item.copy(isActive = true)).distinctBy { it.label },
                    excludedPlayers = current.excludedPlayers.filter { it.id != item.id && it.label != item.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = (current.remainingClubs + item.copy(isActive = true)).distinctBy { it.label },
                    excludedClubs = current.excludedClubs.filter { it.id != item.id && it.label != item.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = (current.remainingNationalTeams + item.copy(isActive = true)).distinctBy { it.label },
                    excludedNationalTeams = current.excludedNationalTeams.filter { it.id != item.id && it.label != item.label },
                    flippedCardIndex = -1
                )
            }
        }
        updatePrompt()
    }

    fun excludeAll(category: DrawCategory) {
        val state = _uiState.value
        val profileId = when (category) {
            DrawCategory.PLAYERS -> state.selectedPlayersProfile?.id ?: 0L
            DrawCategory.CLUBS -> state.selectedClubsProfile?.id ?: 0L
            DrawCategory.NATIONAL_TEAMS -> state.selectedNationalTeamsProfile?.id ?: 0L
        }

        val itemsToExclude = when (category) {
            DrawCategory.PLAYERS -> state.remainingPlayers
            DrawCategory.CLUBS -> state.remainingClubs
            DrawCategory.NATIONAL_TEAMS -> state.remainingNationalTeams
        }

        viewModelScope.launch {
            itemsToExclude.forEach { item ->
                if (item.id > 0L) {
                    updateItemActiveStateUseCase.setItemActive(item.id, false)
                } else if (profileId > 0L) {
                    updateItemActiveStateUseCase.setItemActiveByLabel(profileId, item.label, false)
                }
            }
        }

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = emptyList(),
                    excludedPlayers = (current.excludedPlayers + current.remainingPlayers.map { it.copy(isActive = false) }).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = emptyList(),
                    excludedClubs = (current.excludedClubs + current.remainingClubs.map { it.copy(isActive = false) }).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = emptyList(),
                    excludedNationalTeams = (current.excludedNationalTeams + current.remainingNationalTeams.map { it.copy(isActive = false) }).distinctBy { it.label },
                    flippedCardIndex = -1
                )
            }
        }
        updatePrompt()
    }

    fun restoreAll(category: DrawCategory) {
        val state = _uiState.value
        val profileId = when (category) {
            DrawCategory.PLAYERS -> state.selectedPlayersProfile?.id ?: 0L
            DrawCategory.CLUBS -> state.selectedClubsProfile?.id ?: 0L
            DrawCategory.NATIONAL_TEAMS -> state.selectedNationalTeamsProfile?.id ?: 0L
        }

        viewModelScope.launch {
            if (profileId > 0L) {
                updateItemActiveStateUseCase.resetAllActive(profileId)
            }
        }

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    remainingPlayers = (current.remainingPlayers + current.excludedPlayers.map { it.copy(isActive = true) }).distinctBy { it.label },
                    excludedPlayers = emptyList(),
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    remainingClubs = (current.remainingClubs + current.excludedClubs.map { it.copy(isActive = true) }).distinctBy { it.label },
                    excludedClubs = emptyList(),
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    remainingNationalTeams = (current.remainingNationalTeams + current.excludedNationalTeams.map { it.copy(isActive = true) }).distinctBy { it.label },
                    excludedNationalTeams = emptyList(),
                    flippedCardIndex = -1
                )
            }
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
        val profileId = state.selectedPlayersProfile?.id ?: 0L

        viewModelScope.launch {
            if (profileId > 0L) {
                names.forEach { name ->
                    updateItemActiveStateUseCase.setItemActiveByLabel(profileId, name, true)
                }
            }
        }

        val newPlayerItems = names.mapIndexed { idx, name ->
            ProfileItem(
                id = System.currentTimeMillis() + idx,
                label = name,
                isActive = true
            )
        }

        val updatedRemainingPlayers = (state.remainingPlayers + newPlayerItems).distinctBy { it.label }
        val updatedExcludedPlayers = state.excludedPlayers.filter { excl ->
            names.none { it.equals(excl.label, ignoreCase = true) }
        }

        val assignedClubs = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val unassignedClubs = (state.selectedClubsProfile?.activeItems ?: emptyList()).filter { it.label !in assignedClubs }
        val updatedRemainingClubs = if (state.remainingClubs.isNotEmpty()) {
            (state.remainingClubs + unassignedClubs).distinctBy { it.label }
        } else {
            unassignedClubs
        }

        val assignedTeams = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val unassignedTeams = (state.selectedNationalTeamsProfile?.activeItems ?: emptyList()).filter { it.label !in assignedTeams }
        val updatedRemainingTeams = if (state.remainingNationalTeams.isNotEmpty()) {
            (state.remainingNationalTeams + unassignedTeams).distinctBy { it.label }
        } else {
            unassignedTeams
        }

        _uiState.update {
            it.copy(
                isAddPlayersDialogOpen = false,
                remainingPlayers = updatedRemainingPlayers,
                excludedPlayers = updatedExcludedPlayers,
                remainingClubs = updatedRemainingClubs,
                remainingNationalTeams = updatedRemainingTeams,
                selectedCategory = DrawCategory.PLAYERS,
                flippedCardIndex = -1,
                isRevealing = false
            )
        }
        updatePrompt()
    }

    fun onOpenReorderFixtureDialog(fixture: DrawFixture) {
        _uiState.update { it.copy(reorderingFixture = fixture) }
    }

    fun onDismissReorderDialog() {
        _uiState.update { it.copy(reorderingFixture = null) }
    }

    fun onSwapFixtures(fromFixtureId: String, toFixtureId: String) {
        val list = _uiState.value.fixtures
        val idx1 = list.indexOfFirst { it.id == fromFixtureId }
        val idx2 = list.indexOfFirst { it.id == toFixtureId }
        if (idx1 >= 0 && idx2 >= 0 && idx1 != idx2) {
            drawFixtureRepository.swapFixtures(idx1, idx2)
        }
        _uiState.update { it.copy(reorderingFixture = null) }
    }

    fun onMoveFixtureUp(fixture: DrawFixture) {
        val list = _uiState.value.fixtures
        val idx = list.indexOfFirst { it.id == fixture.id }
        if (idx > 0) {
            drawFixtureRepository.moveFixture(idx, idx - 1)
        }
    }

    fun onMoveFixtureDown(fixture: DrawFixture) {
        val list = _uiState.value.fixtures
        val idx = list.indexOfFirst { it.id == fixture.id }
        if (idx >= 0 && idx < list.size - 1) {
            drawFixtureRepository.moveFixture(idx, idx + 1)
        }
    }

    fun onOpenSwapPlayerDialog(fixture: DrawFixture, isSlotOne: Boolean) {
        _uiState.update { it.copy(swappingPlayerSlot = Pair(fixture, isSlotOne)) }
    }

    fun onDismissSwapPlayerDialog() {
        _uiState.update { it.copy(swappingPlayerSlot = null) }
    }

    fun onConfirmSwapPlayers(targetFixtureId: String, isTargetSlotOne: Boolean) {
        val currentSlot = _uiState.value.swappingPlayerSlot ?: return
        val currentFixture = currentSlot.first
        val isCurrentSlotOne = currentSlot.second

        drawFixtureRepository.swapPlayers(
            fixtureId1 = currentFixture.id,
            isSlot1A = isCurrentSlotOne,
            fixtureId2 = targetFixtureId,
            isSlot1B = isTargetSlotOne
        )
        _uiState.update { it.copy(swappingPlayerSlot = null) }
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
                excludedPlayers = emptyList(),
                excludedClubs = emptyList(),
                excludedNationalTeams = emptyList(),
                fixtures = emptyList(),
                flippedCardIndex = -1,
                isRevealing = false,
                drawResult = null,
                isAddPlayersDialogOpen = false,
                isExcludeDialogOpen = false
            )
        }
        updatePrompt()
    }
}
