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
                val activeCards = activeItems.map { FlipCardState(item = it, isDrawn = false) }
                val excludedItems = if (profile.activeItems.isEmpty()) emptyList() else profile.excludedItems

                when (profile.type) {
                    ProfileType.PLAYERS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.PLAYERS,
                                selectedPlayersProfile = profile,
                                playerCards = activeCards,
                                excludedPlayers = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false,
                                isShuffling = false
                            )
                        }
                    }
                    ProfileType.CLUBS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.CLUBS,
                                selectedClubsProfile = profile,
                                clubsCards = activeCards,
                                excludedClubs = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false,
                                isShuffling = false
                            )
                        }
                    }
                    ProfileType.NATIONAL_TEAMS -> {
                        _uiState.update {
                            it.copy(
                                selectedCategory = DrawCategory.NATIONAL_TEAMS,
                                selectedNationalTeamsProfile = profile,
                                nationalTeamsCards = activeCards,
                                excludedNationalTeams = excludedItems,
                                flippedCardIndex = -1,
                                isRevealing = false,
                                isShuffling = false
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

                    val initPlayerCards = if (current.playerCards.isEmpty() && selPlayer != null) {
                        (selPlayer.activeItems.ifEmpty { selPlayer.items }).map { FlipCardState(item = it, isDrawn = false) }
                    } else current.playerCards

                    val initClubCards = if (current.clubsCards.isEmpty() && selClub != null) {
                        (selClub.activeItems.ifEmpty { selClub.items }).map { FlipCardState(item = it, isDrawn = false) }
                    } else current.clubsCards

                    val initTeamCards = if (current.nationalTeamsCards.isEmpty() && selTeam != null) {
                        (selTeam.activeItems.ifEmpty { selTeam.items }).map { FlipCardState(item = it, isDrawn = false) }
                    } else current.nationalTeamsCards

                    val exclPlayers = if (current.excludedPlayers.isEmpty() && selPlayer != null) selPlayer.excludedItems else current.excludedPlayers
                    val exclClubs = if (current.excludedClubs.isEmpty() && selClub != null) selClub.excludedItems else current.excludedClubs
                    val exclTeams = if (current.excludedNationalTeams.isEmpty() && selTeam != null) selTeam.excludedItems else current.excludedNationalTeams

                    val initialCategory = if (current.selectedPlayersProfile == null && current.selectedClubsProfile == null && current.selectedNationalTeamsProfile == null) {
                        profiles.firstOrNull { it.id == initialProfileId }?.type?.let {
                            when (it) {
                                ProfileType.PLAYERS -> DrawCategory.PLAYERS
                                ProfileType.CLUBS -> DrawCategory.CLUBS
                                ProfileType.NATIONAL_TEAMS -> DrawCategory.NATIONAL_TEAMS
                            }
                        } ?: current.selectedCategory
                    } else current.selectedCategory

                    current.copy(
                        isLoading = false,
                        selectedCategory = initialCategory,
                        playersProfiles = players,
                        clubsProfiles = clubs,
                        nationalTeamsProfiles = teams,
                        selectedPlayersProfile = selPlayer,
                        selectedClubsProfile = selClub,
                        selectedNationalTeamsProfile = selTeam,
                        playerCards = initPlayerCards,
                        clubsCards = initClubCards,
                        nationalTeamsCards = initTeamCards,
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
                isRevealing = false,
                isShuffling = false
            )
        }
        updatePrompt()
    }

    fun onShuffleCards() {
        val state = _uiState.value
        if (state.isRevealing || state.isShuffling) return

        val currentCards = state.currentCards
        val undrawnIndices = currentCards.indices.filter { !currentCards[it].isDrawn }
        if (undrawnIndices.size <= 1) return

        val undrawnItems = undrawnIndices.map { currentCards[it].item }
        val shuffledUndrawn = randomProvider.shuffle(undrawnItems)

        val updatedCards = currentCards.toMutableList()
        undrawnIndices.forEachIndexed { i, targetIdx ->
            updatedCards[targetIdx] = FlipCardState(item = shuffledUndrawn[i], isDrawn = false)
        }

        _uiState.update {
            it.copy(
                isShuffling = true,
                shuffleTrigger = it.shuffleTrigger + 1,
                flippedCardIndex = -1
            )
        }

        viewModelScope.launch {
            delay(650)
            _uiState.update { current ->
                when (current.selectedCategory) {
                    DrawCategory.PLAYERS -> current.copy(
                        playerCards = updatedCards,
                        isShuffling = false
                    )
                    DrawCategory.CLUBS -> current.copy(
                        clubsCards = updatedCards,
                        isShuffling = false
                    )
                    DrawCategory.NATIONAL_TEAMS -> current.copy(
                        nationalTeamsCards = updatedCards,
                        isShuffling = false
                    )
                }
            }
        }
    }

    fun onCardClick(cardIndex: Int) {
        val state = _uiState.value
        val cards = state.currentCards
        if (state.isRevealing || state.isShuffling || cards.isEmpty() || cardIndex !in cards.indices) return

        val targetCard = cards[cardIndex]
        if (targetCard.isDrawn) return

        val drawnItem = targetCard.item

        // 1. Reveal card immediately
        _uiState.update {
            it.copy(
                flippedCardIndex = cardIndex,
                isRevealing = true,
                drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
            )
        }

        viewModelScope.launch {
            // 2. 1.2s celebration delay
            delay(1200)

            // 3. Atomically apply to fixtures, mark card as isDrawn, and reset flippedCardIndex
            applyDrawnItem(cardIndex, drawnItem)
        }
    }

    private fun applyDrawnItem(cardIndex: Int, drawnItem: ProfileItem) {
        val state = _uiState.value
        val fixtures = state.fixtures.toMutableList()

        when (state.selectedCategory) {
            DrawCategory.PLAYERS -> {
                val updatedCards = state.playerCards.mapIndexed { idx, card ->
                    if (idx == cardIndex) card.copy(isDrawn = true) else card
                }
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
                        playerCards = updatedCards,
                        fixtures = fixtures,
                        flippedCardIndex = -1,
                        isRevealing = false,
                        drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                    )
                }
            }
            DrawCategory.CLUBS, DrawCategory.NATIONAL_TEAMS -> {
                val isClubs = state.selectedCategory == DrawCategory.CLUBS
                val targetCards = if (isClubs) state.clubsCards else state.nationalTeamsCards
                val updatedCards = targetCards.mapIndexed { idx, card ->
                    if (idx == cardIndex) card.copy(isDrawn = true) else card
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
                            clubsCards = updatedCards,
                            fixtures = fixtures,
                            flippedCardIndex = -1,
                            isRevealing = false,
                            drawResult = DrawResult(drawType = DrawType.FLIP_CARDS, selectedItem = drawnItem)
                        )
                    } else {
                        current.copy(
                            nationalTeamsCards = updatedCards,
                            fixtures = fixtures,
                            flippedCardIndex = -1,
                            isRevealing = false,
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
                    playerCards = current.playerCards.filter { it.item.id != item.id || it.item.label != item.label },
                    excludedPlayers = (current.excludedPlayers + item.copy(isActive = false)).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    clubsCards = current.clubsCards.filter { it.item.id != item.id || it.item.label != item.label },
                    excludedClubs = (current.excludedClubs + item.copy(isActive = false)).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    nationalTeamsCards = current.nationalTeamsCards.filter { it.item.id != item.id || it.item.label != item.label },
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

        val restoredCard = FlipCardState(item = item.copy(isActive = true), isDrawn = false)

        _uiState.update { current ->
            when (category) {
                DrawCategory.PLAYERS -> current.copy(
                    playerCards = (current.playerCards + restoredCard).distinctBy { it.item.label },
                    excludedPlayers = current.excludedPlayers.filter { it.id != item.id && it.label != item.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    clubsCards = (current.clubsCards + restoredCard).distinctBy { it.item.label },
                    excludedClubs = current.excludedClubs.filter { it.id != item.id && it.label != item.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    nationalTeamsCards = (current.nationalTeamsCards + restoredCard).distinctBy { it.item.label },
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
                    playerCards = current.playerCards.filter { it.isDrawn },
                    excludedPlayers = (current.excludedPlayers + itemsToExclude.map { it.copy(isActive = false) }).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.CLUBS -> current.copy(
                    clubsCards = current.clubsCards.filter { it.isDrawn },
                    excludedClubs = (current.excludedClubs + itemsToExclude.map { it.copy(isActive = false) }).distinctBy { it.label },
                    flippedCardIndex = -1
                )
                DrawCategory.NATIONAL_TEAMS -> current.copy(
                    nationalTeamsCards = current.nationalTeamsCards.filter { it.isDrawn },
                    excludedNationalTeams = (current.excludedNationalTeams + itemsToExclude.map { it.copy(isActive = false) }).distinctBy { it.label },
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
                DrawCategory.PLAYERS -> {
                    val restoredCards = current.excludedPlayers.map { FlipCardState(it.copy(isActive = true), isDrawn = false) }
                    current.copy(
                        playerCards = (current.playerCards + restoredCards).distinctBy { it.item.label },
                        excludedPlayers = emptyList(),
                        flippedCardIndex = -1
                    )
                }
                DrawCategory.CLUBS -> {
                    val restoredCards = current.excludedClubs.map { FlipCardState(it.copy(isActive = true), isDrawn = false) }
                    current.copy(
                        clubsCards = (current.clubsCards + restoredCards).distinctBy { it.item.label },
                        excludedClubs = emptyList(),
                        flippedCardIndex = -1
                    )
                }
                DrawCategory.NATIONAL_TEAMS -> {
                    val restoredCards = current.excludedNationalTeams.map { FlipCardState(it.copy(isActive = true), isDrawn = false) }
                    current.copy(
                        nationalTeamsCards = (current.nationalTeamsCards + restoredCards).distinctBy { it.item.label },
                        excludedNationalTeams = emptyList(),
                        flippedCardIndex = -1
                    )
                }
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

        val newCardStates = names.mapIndexed { idx, name ->
            FlipCardState(
                item = ProfileItem(
                    id = System.currentTimeMillis() + idx,
                    label = name,
                    isActive = true
                ),
                isDrawn = false
            )
        }

        val updatedPlayerCards = (state.playerCards + newCardStates).distinctBy { it.item.label }
        val updatedExcludedPlayers = state.excludedPlayers.filter { excl ->
            names.none { it.equals(excl.label, ignoreCase = true) }
        }

        val assignedClubs = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val existingClubLabels = state.clubsCards.map { it.item.label }.toSet()
        val unassignedClubs = (state.selectedClubsProfile?.activeItems ?: emptyList())
            .filter { it.label !in assignedClubs && it.label !in existingClubLabels }
        val updatedClubCards = state.clubsCards + unassignedClubs.map { FlipCardState(item = it, isDrawn = false) }

        val assignedTeams = state.fixtures.flatMap { listOfNotNull(it.playerOneTeam, it.playerTwoTeam) }.toSet()
        val existingTeamLabels = state.nationalTeamsCards.map { it.item.label }.toSet()
        val unassignedTeams = (state.selectedNationalTeamsProfile?.activeItems ?: emptyList())
            .filter { it.label !in assignedTeams && it.label !in existingTeamLabels }
        val updatedTeamCards = state.nationalTeamsCards + unassignedTeams.map { FlipCardState(item = it, isDrawn = false) }

        _uiState.update {
            it.copy(
                isAddPlayersDialogOpen = false,
                playerCards = updatedPlayerCards,
                excludedPlayers = updatedExcludedPlayers,
                clubsCards = updatedClubCards,
                nationalTeamsCards = updatedTeamCards,
                selectedCategory = DrawCategory.PLAYERS,
                flippedCardIndex = -1,
                isRevealing = false,
                isShuffling = false
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
        val selPlayer = state.selectedPlayersProfile ?: state.playersProfiles.firstOrNull()
        val selClub = state.selectedClubsProfile ?: state.clubsProfiles.firstOrNull()
        val selTeam = state.selectedNationalTeamsProfile ?: state.nationalTeamsProfiles.firstOrNull()

        val defaultPlayers = (selPlayer?.activeItems?.ifEmpty { selPlayer.items } ?: emptyList()).map { FlipCardState(item = it, isDrawn = false) }
        val defaultClubs = (selClub?.activeItems?.ifEmpty { selClub.items } ?: emptyList()).map { FlipCardState(item = it, isDrawn = false) }
        val defaultTeams = (selTeam?.activeItems?.ifEmpty { selTeam.items } ?: emptyList()).map { FlipCardState(item = it, isDrawn = false) }

        drawFixtureRepository.clearFixtures()
        _uiState.update {
            it.copy(
                playerCards = defaultPlayers,
                clubsCards = defaultClubs,
                nationalTeamsCards = defaultTeams,
                excludedPlayers = selPlayer?.excludedItems ?: emptyList(),
                excludedClubs = selClub?.excludedItems ?: emptyList(),
                excludedNationalTeams = selTeam?.excludedItems ?: emptyList(),
                fixtures = emptyList(),
                flippedCardIndex = -1,
                isRevealing = false,
                isShuffling = false,
                drawResult = null,
                isAddPlayersDialogOpen = false,
                isExcludeDialogOpen = false
            )
        }
        updatePrompt()
    }
}
