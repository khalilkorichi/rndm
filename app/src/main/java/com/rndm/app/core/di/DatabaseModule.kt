package com.rndm.app.core.di

import android.content.Context
import androidx.room.Room
import com.rndm.app.core.util.Constants
import com.rndm.app.data.local.MIGRATION_1_2
import com.rndm.app.data.local.MIGRATION_2_3
import com.rndm.app.data.local.MIGRATION_3_4
import com.rndm.app.data.local.MIGRATION_4_5
import com.rndm.app.data.local.MIGRATION_5_6
import com.rndm.app.data.local.MIGRATION_6_7
import com.rndm.app.data.local.MIGRATION_7_8
import com.rndm.app.data.local.MIGRATION_8_9
import com.rndm.app.data.local.RndmDatabase
import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.PlayerProfileDao
import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.ProfileItemDao
import com.rndm.app.data.local.dao.TournamentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRndmDatabase(
        @ApplicationContext context: Context
    ): RndmDatabase {
        return Room.databaseBuilder(
            context,
            RndmDatabase::class.java,
            Constants.DATABASE_NAME
        ).addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideProfileDao(database: RndmDatabase): ProfileDao {
        return database.profileDao()
    }

    @Provides
    fun provideProfileItemDao(database: RndmDatabase): ProfileItemDao {
        return database.profileItemDao()
    }

    @Provides
    fun provideTournamentDao(database: RndmDatabase): TournamentDao {
        return database.tournamentDao()
    }

    @Provides
    fun provideMatchDao(database: RndmDatabase): MatchDao {
        return database.matchDao()
    }

    @Provides
    fun providePlayerProfileDao(database: RndmDatabase): PlayerProfileDao {
        return database.playerProfileDao()
    }
}
