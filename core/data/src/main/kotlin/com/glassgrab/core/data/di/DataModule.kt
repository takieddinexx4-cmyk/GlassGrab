package com.glassgrab.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.glassgrab.core.common.ApplicationScope
import com.glassgrab.core.data.local.DownloadDao
import com.glassgrab.core.data.local.GlassGrabDatabase
import com.glassgrab.core.data.preferences.UserPreferencesSerializer
import com.glassgrab.core.data.repository.DownloadRepositoryImpl
import com.glassgrab.core.data.repository.MediaExtractorRepositoryImpl
import com.glassgrab.core.data.repository.PreferencesRepositoryImpl
import com.glassgrab.core.domain.model.UserPreferences
import com.glassgrab.core.domain.repository.DownloadRepository
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import com.glassgrab.core.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GlassGrabDatabase =
        Room.databaseBuilder(context, GlassGrabDatabase::class.java, "glassgrab.db")
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

    @Provides
    fun provideDownloadDao(db: GlassGrabDatabase): DownloadDao = db.downloadDao()

    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<UserPreferences> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer,
        scope = scope,
        produceFile = { context.dataStoreFile("user_preferences.pb") },
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindExtractorRepository(impl: MediaExtractorRepositoryImpl): MediaExtractorRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
