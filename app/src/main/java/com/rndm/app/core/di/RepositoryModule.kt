package com.rndm.app.core.di

import com.rndm.app.core.util.DefaultRandomProvider
import com.rndm.app.core.util.RandomProvider
import com.rndm.app.data.repository.DrawFixtureRepositoryImpl
import com.rndm.app.data.repository.DrawRepositoryImpl
import com.rndm.app.data.repository.ProfileRepositoryImpl
import com.rndm.app.data.repository.TournamentRepositoryImpl
import com.rndm.app.data.repository.UserPreferencesRepositoryImpl
import com.rndm.app.domain.repository.DrawFixtureRepository
import com.rndm.app.domain.repository.DrawRepository
import com.rndm.app.domain.repository.ProfileRepository
import com.rndm.app.domain.repository.TournamentRepository
import com.rndm.app.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindDrawRepository(
        drawRepositoryImpl: DrawRepositoryImpl
    ): DrawRepository

    @Binds
    @Singleton
    abstract fun bindDrawFixtureRepository(
        drawFixtureRepositoryImpl: DrawFixtureRepositoryImpl
    ): DrawFixtureRepository

    @Binds
    @Singleton
    abstract fun bindTournamentRepository(
        tournamentRepositoryImpl: TournamentRepositoryImpl
    ): TournamentRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindRandomProvider(
        defaultRandomProvider: DefaultRandomProvider
    ): RandomProvider
}
