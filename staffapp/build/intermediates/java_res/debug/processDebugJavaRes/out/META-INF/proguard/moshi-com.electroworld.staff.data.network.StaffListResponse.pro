-keepnames class com.electroworld.staff.data.network.StaffListResponse
-if class com.electroworld.staff.data.network.StaffListResponse
-keep class com.electroworld.staff.data.network.StaffListResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
