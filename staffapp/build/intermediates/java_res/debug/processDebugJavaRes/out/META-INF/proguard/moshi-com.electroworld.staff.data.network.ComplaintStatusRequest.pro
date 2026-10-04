-keepnames class com.electroworld.staff.data.network.ComplaintStatusRequest
-if class com.electroworld.staff.data.network.ComplaintStatusRequest
-keep class com.electroworld.staff.data.network.ComplaintStatusRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
