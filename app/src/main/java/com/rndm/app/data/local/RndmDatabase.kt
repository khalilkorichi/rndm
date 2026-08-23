package com.rndm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.ProfileItemDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.ProfileEntity
import com.rndm.app.data.local.entity.ProfileItemEntity
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tournaments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `stage` TEXT NOT NULL,
                `playersProfileId` INTEGER NOT NULL,
                `clubsProfileId` INTEGER,
                `groupsCount` INTEGER NOT NULL,
                `qualifiersPerGroup` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tournament_participants` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tournamentId` INTEGER NOT NULL,
                `playerItemId` INTEGER NOT NULL,
                `playerName` TEXT NOT NULL,
                `clubName` TEXT,
                `groupIndex` INTEGER NOT NULL,
                FOREIGN KEY(`tournamentId`) REFERENCES `tournaments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournament_participants_tournamentId` ON `tournament_participants` (`tournamentId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `matches` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tournamentId` INTEGER NOT NULL,
                `stage` TEXT NOT NULL,
                `groupIndex` INTEGER,
                `roundIndex` INTEGER NOT NULL,
                `bracketMatchIndex` INTEGER,
                `playerOneName` TEXT NOT NULL,
                `playerOneClub` TEXT,
                `playerTwoName` TEXT,
                `playerTwoClub` TEXT,
                `scoreOne` INTEGER,
                `scoreTwo` INTEGER,
                `winnerName` TEXT,
                `status` TEXT NOT NULL,
                `scheduledTimestamp` INTEGER,
                FOREIGN KEY(`tournamentId`) REFERENCES `tournaments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_tournamentId` ON `matches` (`tournamentId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN penaltyScoreOne INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE matches ADD COLUMN penaltyScoreTwo INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE matches ADD COLUMN isPlayerOneLuckyLoser INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE matches ADD COLUMN isPlayerTwoLuckyLoser INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_tournamentId_stage_roundIndex` ON `matches` (`tournamentId`, `stage`, `roundIndex`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournaments_isArchived_updatedAt` ON `tournaments` (`isArchived`, `updatedAt`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_lastUsedAt_createdAt` ON `profiles` (`lastUsedAt`, `createdAt`)")
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        ProfileItemEntity::class,
        TournamentEntity::class,
        TournamentParticipantEntity::class,
        MatchEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class RndmDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileItemDao(): ProfileItemDao
    abstract fun tournamentDao(): TournamentDao
    abstract fun matchDao(): MatchDao
}

