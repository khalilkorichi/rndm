package com.rndm.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.PlayerProfileDao
import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.ProfileGroupDao
import com.rndm.app.data.local.dao.ProfileItemDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.PlayerProfileEntity
import com.rndm.app.data.local.entity.ProfileEntity
import com.rndm.app.data.local.entity.ProfileGroupEntity
import com.rndm.app.data.local.entity.ProfileItemEntity
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentExclusionEntity
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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `player_profiles` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `nickname` TEXT,
                `avatarIcon` TEXT,
                `favoriteClub` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_playerOneName_playerTwoName_id` ON `matches` (`playerOneName`, `playerTwoName`, `id`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tournaments ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN remoteId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN shareCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isRemote INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isHost INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN hostUid TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN lastSyncedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tournaments ADD COLUMN remoteVersion INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE matches ADD COLUMN remoteId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE matches ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY'")
        db.execSQL("ALTER TABLE matches ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE tournament_participants ADD COLUMN remoteId TEXT DEFAULT NULL")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournaments_remoteId` ON `tournaments` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournaments_shareCode` ON `tournaments` (`shareCode`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_remoteId` ON `matches` (`remoteId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournament_participants_remoteId` ON `tournament_participants` (`remoteId`)")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `profile_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `icon` TEXT NOT NULL DEFAULT 'ic_folder',
                `colorHex` TEXT,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("ALTER TABLE profiles ADD COLUMN groupId INTEGER DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profiles_groupId` ON `profiles` (`groupId`)")
        db.execSQL("ALTER TABLE profile_items ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profile_groups_createdAt` ON `profile_groups` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_profile_groups_name` ON `profile_groups` (`name`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tournaments ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `tournament_exclusions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tournamentId` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `itemLabel` TEXT NOT NULL,
                FOREIGN KEY(`tournamentId`) REFERENCES `tournaments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tournament_exclusions_tournamentId` ON `tournament_exclusions` (`tournamentId`)")
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        ProfileItemEntity::class,
        ProfileGroupEntity::class,
        TournamentEntity::class,
        TournamentParticipantEntity::class,
        TournamentExclusionEntity::class,
        MatchEntity::class,
        PlayerProfileEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class RndmDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileItemDao(): ProfileItemDao
    abstract fun profileGroupDao(): ProfileGroupDao
    abstract fun tournamentDao(): TournamentDao

    abstract fun matchDao(): MatchDao
    abstract fun playerProfileDao(): PlayerProfileDao
}


