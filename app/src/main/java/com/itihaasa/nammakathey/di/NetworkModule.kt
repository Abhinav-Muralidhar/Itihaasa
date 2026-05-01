package com.itihaasa.nammakathey.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.itihaasa.nammakathey.data.remote.GeminiApiService
import com.itihaasa.nammakathey.data.remote.WikipediaApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WikipediaRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiRetrofit(gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .client(geminiHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @WikipediaRetrofit
    fun provideWikipediaRetrofit(gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(wikipediaHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(
        @GeminiRetrofit retrofit: Retrofit
    ): GeminiApiService = retrofit.create(GeminiApiService::class.java)

    @Provides
    @Singleton
    fun provideWikipediaApiService(
        @WikipediaRetrofit retrofit: Retrofit
    ): WikipediaApiService = retrofit.create(WikipediaApiService::class.java)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    private fun geminiHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private fun wikipediaHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
