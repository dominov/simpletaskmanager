package com.example.simpletaskmanagement.di

import android.content.Context
import androidx.room.Room
import com.example.simpletaskmanagement.data.local.TaskDatabase
import com.example.simpletaskmanagement.data.local.dao.CategoryDao
import com.example.simpletaskmanagement.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<TaskDatabase>
    ): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            TaskDatabase.DATABASE_NAME
        )
            .addCallback(TaskDatabase.createCallback { databaseProvider.get() })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCategoryDao(database: TaskDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()
}
