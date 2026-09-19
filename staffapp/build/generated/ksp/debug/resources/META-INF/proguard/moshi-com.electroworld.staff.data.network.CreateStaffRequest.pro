-keepnames class com.electroworld.staff.data.network.CreateStaffRequest
-if class com.electroworld.staff.data.network.CreateStaffRequest
-keep class com.electroworld.staff.data.network.CreateStaffRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
