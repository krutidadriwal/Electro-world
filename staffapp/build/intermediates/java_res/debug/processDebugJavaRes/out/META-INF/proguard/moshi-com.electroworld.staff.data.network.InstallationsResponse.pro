-keepnames class com.electroworld.staff.data.network.InstallationsResponse
-if class com.electroworld.staff.data.network.InstallationsResponse
-keep class com.electroworld.staff.data.network.InstallationsResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
