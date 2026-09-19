-keepnames class com.electroworld.staff.data.network.SupabasePasswordLoginRequest
-if class com.electroworld.staff.data.network.SupabasePasswordLoginRequest
-keep class com.electroworld.staff.data.network.SupabasePasswordLoginRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
