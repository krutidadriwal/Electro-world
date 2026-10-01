-keepnames class com.electroworld.staff.data.network.SupabaseRecoverRequest
-if class com.electroworld.staff.data.network.SupabaseRecoverRequest
-keep class com.electroworld.staff.data.network.SupabaseRecoverRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
