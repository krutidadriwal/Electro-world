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

  val userApi: UserApi by lazy {
    Retrofit.Builder()
      .baseUrl("${BuildConfig.SERVER_BASE_URL}/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(UserApi::class.java)
  }
}
