package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.GambitDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.GameSaveDao
import com.example.data.local.dao.PlayerProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Random
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGambitDatabase(
        @ApplicationContext context: Context
    ): GambitDatabase {
        return Room.databaseBuilder(
            context,
            GambitDatabase::class.java,
            "gambit_database"
        )
        .fallbackToDestructiveMigration() // safe schema migrations
        .build()
    }

    @Provides
    @Singleton
    fun provideGameDao(database: GambitDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    @Singleton
    fun provideGameSaveDao(database: GambitDatabase): GameSaveDao {
        return database.gameSaveDao()
    }

    @Provides
    @Singleton
    fun providePlayerProgressDao(database: GambitDatabase): PlayerProgressDao {
        return database.playerProgressDao()
    }

    @Provides
    @Singleton
    fun provideAIMemoryDao(database: GambitDatabase): com.example.data.local.dao.AIMemoryDao {
        return database.aiMemoryDao()
    }

    @Provides
    @Singleton
    fun provideRandom(): Random {
        // Seeded for debug reproducibility and predictability under test
        return Random(42)
    }
}
