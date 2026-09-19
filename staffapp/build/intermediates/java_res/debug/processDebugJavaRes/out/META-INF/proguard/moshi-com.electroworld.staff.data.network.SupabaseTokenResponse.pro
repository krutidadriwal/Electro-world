-keepnames class com.electroworld.staff.data.network.SupabaseTokenResponse
-if class com.electroworld.staff.data.network.SupabaseTokenResponse
-keep class com.electroworld.staff.data.network.SupabaseTokenResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
