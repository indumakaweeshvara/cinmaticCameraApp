package com.cinecam.cinematiccamera.di

import android.content.Context
import com.cinecam.cinematiccamera.processing.segmentation.BlurProcessor
import com.cinecam.cinematiccamera.processing.segmentation.SelfieSegmentationProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Processing components
 */
@Module
@InstallIn(SingletonComponent::class)
object ProcessingModule {
    
    @Provides
    @Singleton
    fun provideSelfieSegmentationProcessor(
        @ApplicationContext context: Context
    ): SelfieSegmentationProcessor {
        return SelfieSegmentationProcessor(context)
    }
    
    @Provides
    @Singleton
    fun provideBlurProcessor(): BlurProcessor {
        return BlurProcessor()
    }
}
