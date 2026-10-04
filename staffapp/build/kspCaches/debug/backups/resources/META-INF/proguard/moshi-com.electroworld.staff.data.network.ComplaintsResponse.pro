-keepnames class com.electroworld.staff.data.network.ComplaintsResponse
-if class com.electroworld.staff.data.network.ComplaintsResponse
-keep class com.electroworld.staff.data.network.ComplaintsResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
