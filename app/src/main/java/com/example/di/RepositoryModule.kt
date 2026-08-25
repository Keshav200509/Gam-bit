package com.example.di

import com.example.data.local.GambitDatabase
import com.example.data.local.dao.UserProfileDao
import com.example.data.firestore.UserProfileRepository
import com.example.data.firestore.UserProfileRepositoryImpl
import com.example.data.firestore.GameHistoryRepository
import com.example.data.firestore.GameHistoryRepositoryImpl
import com.example.data.firestore.FriendsRepository
import com.example.data.firestore.FriendsRepositoryImpl
import com.example.data.firestore.PvPInviteRepository
import com.example.data.firestore.PvPInviteRepositoryImpl
import com.example.data.realtime.PvPMatchRepository
import com.example.data.realtime.PvPMatchRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindGameHistoryRepository(
        impl: GameHistoryRepositoryImpl
    ): GameHistoryRepository

    @Binds
    @Singleton
    abstract fun bindFriendsRepository(
        impl: FriendsRepositoryImpl
    ): FriendsRepository

    @Binds
    @Singleton
    abstract fun bindPvPInviteRepository(
        impl: PvPInviteRepositoryImpl
    ): PvPInviteRepository

    @Binds
    @Singleton
    abstract fun bindPvPMatchRepository(
        impl: PvPMatchRepositoryImpl
    ): PvPMatchRepository

    @Binds
    @Singleton
    abstract fun bindDailyChallengeRepository(
        impl: com.example.data.repository.DailyChallengeRepositoryImpl
    ): com.example.data.repository.DailyChallengeRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseDatabase(): FirebaseDatabase {
            return FirebaseDatabase.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }

        @Provides
        @Singleton
        fun provideUserProfileDao(database: GambitDatabase): UserProfileDao {
            return database.userProfileDao()
        }
    }
}
