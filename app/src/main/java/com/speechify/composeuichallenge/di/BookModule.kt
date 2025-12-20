package com.speechify.composeuichallenge.di

import com.speechify.composeuichallenge.repository.BooksRepository
import com.speechify.composeuichallenge.repository.BooksRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BookModule {

    @Binds
    fun bindBooksRepository(
        implementation: BooksRepositoryImpl
    ): BooksRepository
}
