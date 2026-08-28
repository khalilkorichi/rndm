package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.PlayerProfileDao
import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.isPlaceholderPlayerName
import com.rndm.app.domain.model.isRealPlayerName
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import kotlinx.coroutines.Dispatchers

class PlayerProfileRepositoryImplTest {

    private val playerProfileDao = mockk<PlayerProfileDao>(relaxed = true)
    private val matchDao = mockk<MatchDao>(relaxed = true)
    private val tournamentDao = mockk<TournamentDao>(relaxed = true)
    private val profileDao = mockk<ProfileDao>(relaxed = true)

    private val repository = PlayerProfileRepositoryImpl(
        tournamentDao = tournamentDao,
        matchDao = matchDao,
        playerProfileDao = playerProfileDao,
        profileDao = profileDao,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun `isRealPlayerName correctly identifies real players and ignores placeholders`() {
        assertTrue("محمد".isRealPlayerName())
        assertTrue("Cristiano Ronaldo".isRealPlayerName())
        assertTrue("Ali".isRealPlayerName())

        assertFalse("فائز نصف النهائي 1".isRealPlayerName())
        assertFalse("خاسر نصف النهائي 1".isRealPlayerName())
        assertFalse("فائز ربع النهائي 4".isRealPlayerName())
        assertFalse("فائز دور الـ 16 (1)".isRealPlayerName())
        assertFalse("BYE".isRealPlayerName())
        assertFalse("bye".isRealPlayerName())
        assertFalse("TBD".isRealPlayerName())
        assertFalse("أحسن خاسر".isRealPlayerName())
        assertFalse("".isRealPlayerName())
        assertFalse(null.isRealPlayerName())

        assertTrue("فائز نصف النهائي 1".isPlaceholderPlayerName())
        assertTrue("خاسر نصف النهائي 1".isPlaceholderPlayerName())
    }

    @Test
    fun `getAllPlayersLeaderboard completely excludes placeholder names from leaderboard list`() = runTest {
        val matches = listOf(
            MatchEntity(
                id = 1L,
                tournamentId = 10L,
                stage = MatchStage.SEMI_FINALS,
                roundIndex = 2,
                playerOneName = "فائز نصف النهائي 1",
                playerTwoName = "خاسر نصف النهائي 1",
                status = MatchStatus.PENDING
            ),
            MatchEntity(
                id = 2L,
                tournamentId = 10L,
                stage = MatchStage.FINAL,
                roundIndex = 3,
                playerOneName = "محمد",
                playerTwoName = "سيف",
                scoreOne = 3,
                scoreTwo = 1,
                winnerName = "محمد",
                status = MatchStatus.FINISHED
            )
        )

        val participants = listOf(
            TournamentParticipantEntity(id = 1L, tournamentId = 10L, playerItemId = 1L, playerName = "محمد"),
            TournamentParticipantEntity(id = 2L, tournamentId = 10L, playerItemId = 2L, playerName = "سيف"),
            TournamentParticipantEntity(id = 3L, tournamentId = 10L, playerItemId = 3L, playerName = "فائز نصف النهائي 1")
        )

        every { matchDao.getAllMatches() } returns flowOf(matches)
        every { tournamentDao.getAllTournaments() } returns flowOf(emptyList())
        every { tournamentDao.getAllParticipants() } returns flowOf(participants)
        every { playerProfileDao.getAllPlayerProfiles() } returns flowOf(emptyList())
        every { profileDao.getAllProfilesWithItems() } returns flowOf(emptyList())

        val leaderboard = repository.getAllPlayersLeaderboard().first()

        assertEquals(2, leaderboard.size)
        assertEquals("محمد", leaderboard[0].playerName)
        assertEquals("سيف", leaderboard[1].playerName)
        assertTrue(leaderboard.none { it.playerName.contains("فائز") || it.playerName.contains("خاسر") })
    }
}
