package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
  private val moshi = Moshi.Builder().build()

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(
      HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
      }
    )
    .build()

  private val retrofit: Retrofit by lazy {
    Retrofit.Builder()
      .baseUrl("${BuildConfig.SERVER_BASE_URL}/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  val userApi: UserApi by lazy { retrofit.create(UserApi::class.java) }
  val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
  val deviceTokenApi: DeviceTokenApi by lazy { retrofit.create(DeviceTokenApi::class.java) }
}
