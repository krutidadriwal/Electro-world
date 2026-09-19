-keepnames class com.electroworld.staff.data.network.SupabaseRefreshRequest
-if class com.electroworld.staff.data.network.SupabaseRefreshRequest
-keep class com.electroworld.staff.data.network.SupabaseRefreshRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
