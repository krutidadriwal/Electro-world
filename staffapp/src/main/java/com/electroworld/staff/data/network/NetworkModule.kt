package com.electroworld.staff.data.network

import android.content.Context
import com.electroworld.staff.BuildConfig
import com.electroworld.staff.data.SessionManager
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Two independent Retrofit clients: one talks to Supabase's GoTrue REST API
// directly (login/refresh), the other to our own server/api for
// staff/notifications endpoints, authenticated with whatever Supabase access
// token is currently stored.
object NetworkModule {
  private lateinit var sessionManager: SessionManager

  fun init(context: Context) {
    sessionManager = SessionManager(context.applicationContext)
  }

  private val moshi = Moshi.Builder().build()

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
  }

  private val supabaseOkHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
      val request = chain.request().newBuilder()
        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
        .addHeader("Content-Type", "application/json")
        .build()
      chain.proceed(request)
    }
    .build()

  private val supabaseRetrofit: Retrofit by lazy {
    Retrofit.Builder()
      .baseUrl("${BuildConfig.SUPABASE_URL}/")
      .client(supabaseOkHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  val supabaseAuthApi: SupabaseAuthApi by lazy { supabaseRetrofit.create(SupabaseAuthApi::class.java) }

  // Refreshes the stored session using the refresh token, blocking (this
  // runs inside OkHttp's Authenticator, which is synchronous by contract).
  // Returns true if the refresh succeeded and a new access token was saved.
  private fun refreshSession(): Boolean {
    val refreshToken = sessionManager.refreshToken
    if (refreshToken.isBlank()) return false
    return try {
      val response = runBlocking { supabaseAuthApi.refresh(request = SupabaseRefreshRequest(refreshToken)) }
      sessionManager.saveTokens(response.access_token, response.refresh_token)
      true
    } catch (e: Exception) {
      false
    }
  }

  private val serverOkHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .addInterceptor { chain ->
      val token = sessionManager.accessToken
      val request = if (token.isNotBlank()) {
        chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
      } else {
        chain.request()
      }
      chain.proceed(request)
    }
    .authenticator(object : Authenticator {
      override fun authenticate(route: Route?, response: Response): Request? {
        // Only ever retry once -- if the request we're looking at already
        // carries a bearer header, this is already a retry attempt.
        if (response.request.header("Authorization") != null && response.priorResponse != null) {
          return null
        }
        if (!refreshSession()) return null
        return response.request.newBuilder()
          .header("Authorization", "Bearer ${sessionManager.accessToken}")
          .build()
      }
    })
    .build()

  private val serverRetrofit: Retrofit by lazy {
    Retrofit.Builder()
      .baseUrl("${BuildConfig.SERVER_BASE_URL}/")
      .client(serverOkHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  val staffApi: StaffApi by lazy { serverRetrofit.create(StaffApi::class.java) }
  val notificationsApi: NotificationsApi by lazy { serverRetrofit.create(NotificationsApi::class.java) }
}
