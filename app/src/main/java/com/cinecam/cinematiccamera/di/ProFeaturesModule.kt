package com.cinecam.cinematiccamera.di

import android.content.Context
import com.cinecam.cinematiccamera.processing.effects.*
import com.cinecam.cinematiccamera.processing.recording.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Pro Features components
 */
@Module
@InstallIn(SingletonComponent::class)
object ProFeaturesModule {
    
    // ============ Visual Effects ============
    
    @Provides
    @Singleton
    fun provideFocusPeakingProcessor(): FocusPeakingProcessor {
        return FocusPeakingProcessor()
    }
    
    @Provides
    @Singleton
    fun provideZebraPatternProcessor(): ZebraPatternProcessor {
        return ZebraPatternProcessor()
    }
    
    @Provides
    @Singleton
    fun provideFilmGrainProcessor(): FilmGrainProcessor {
        return FilmGrainProcessor()
    }
    
    @Provides
    @Singleton
    fun provideAnamorphicProcessor(): AnamorphicProcessor {
        return AnamorphicProcessor()
    }
    
    // ============ Monitoring ============
    
    @Provides
    @Singleton
    fun provideHistogramProcessor(): HistogramProcessor {
        return HistogramProcessor()
    }
    
    @Provides
    @Singleton
    fun provideAudioLevelProcessor(): AudioLevelProcessor {
        return AudioLevelProcessor()
    }
    
    @Provides
    @Singleton
    fun provideGridOverlayProcessor(): GridOverlayProcessor {
        return GridOverlayProcessor()
    }
    
    // ============ Special Recording Modes ============
    
    @Provides
    @Singleton
    fun provideSlowMotionRecorder(
        @ApplicationContext context: Context
    ): SlowMotionRecorder {
        return SlowMotionRecorder(context)
    }
    
    @Provides
    @Singleton
    fun provideTimeLapseRecorder(
        @ApplicationContext context: Context
    ): TimeLapseRecorder {
        return TimeLapseRecorder(context)
    }
    
    @Provides
    @Singleton
    fun provideHyperlapseRecorder(
        @ApplicationContext context: Context
    ): HyperlapseRecorder {
        return HyperlapseRecorder(context)
    }
}
