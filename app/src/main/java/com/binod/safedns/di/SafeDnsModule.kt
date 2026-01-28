package com.binod.safedns.di

import com.binod.safedns.domain.repository.ProtectionRepository
import com.binod.safedns.domain.repository.ProtectionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindProtectionRepository(
        impl: ProtectionRepositoryImpl
    ): ProtectionRepository
}
