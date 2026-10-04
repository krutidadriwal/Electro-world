-keepnames class com.electroworld.staff.data.network.ComplaintStatusResponse
-if class com.electroworld.staff.data.network.ComplaintStatusResponse
-keep class com.electroworld.staff.data.network.ComplaintStatusResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
