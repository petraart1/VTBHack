package com.example.project_for_vtb.di

import android.content.Context
import com.example.project_for_vtb.data.mock.MockDataService
import com.example.project_for_vtb.data.remote.api.AuthApiService
import com.example.project_for_vtb.data.remote.api.BankApiService
import com.example.project_for_vtb.data.remote.api.RadarApiService
import com.example.project_for_vtb.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Модуль для предоставления сетевых зависимостей
 * 
 * Предоставляет три отдельных Retrofit клиента для работы с разными сервисами:
 * - AuthApiService: http://158.160.102.225/api/v1/auth
 * - BankApiService: http://158.160.102.225/api/v1/bank
 * - RadarApiService: http://158.160.102.225/api/v1/radar
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // Базовые URL для API сервисов
    private const val BASE_URL_AUTH = "http://158.160.102.225/api/v1/auth/"
    private const val BASE_URL_BANK = "http://158.160.102.225/api/v1/bank/"
    private const val BASE_URL_RADAR = "http://158.160.102.225/api/v1/radar/"
    
    /**
     * Предоставляет Mock сервис для разработки Frontend.
     * Можно использовать для тестирования без подключенного Backend.
     */
    @Provides
    @Singleton
    fun provideMockDataService(
        @ApplicationContext context: Context,
        gson: Gson
    ): MockDataService {
        return MockDataService(context, gson)
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }
    
    /**
     * Предоставляет OkHttpClient для банковского API с авторизацией
     */
    @Provides
    @Singleton
    @Named("bankClient")
    fun provideBankOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Предоставляет OkHttpClient для авторизации (без авторизационного интерцептора)
     */
    @Provides
    @Singleton
    @Named("authClient")
    fun provideAuthOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Предоставляет OkHttpClient для радара
     */
    @Provides
    @Singleton
    @Named("radarClient")
    fun provideRadarOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Предоставляет Retrofit клиент для авторизации
     */
    @Provides
    @Singleton
    @Named("authRetrofit")
    fun provideAuthRetrofit(
        @Named("authClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_AUTH)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Предоставляет Retrofit клиент для банковского API
     */
    @Provides
    @Singleton
    @Named("bankRetrofit")
    fun provideBankRetrofit(
        @Named("bankClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_BANK)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Предоставляет Retrofit клиент для радара
     */
    @Provides
    @Singleton
    @Named("radarRetrofit")
    fun provideRadarRetrofit(
        @Named("radarClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_RADAR)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Предоставляет AuthApiService
     */
    @Provides
    @Singleton
    fun provideAuthApiService(
        @Named("authRetrofit") retrofit: Retrofit
    ): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    /**
     * Предоставляет BankApiService
     */
    @Provides
    @Singleton
    fun provideBankApiService(
        @Named("bankRetrofit") retrofit: Retrofit
    ): BankApiService {
        return retrofit.create(BankApiService::class.java)
    }
    
    /**
     * Предоставляет RadarApiService
     */
    @Provides
    @Singleton
    fun provideRadarApiService(
        @Named("radarRetrofit") retrofit: Retrofit
    ): RadarApiService {
        return retrofit.create(RadarApiService::class.java)
    }
}

