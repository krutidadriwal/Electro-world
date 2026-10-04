-keepnames class com.electroworld.staff.data.network.InstallationStatusResponse
-if class com.electroworld.staff.data.network.InstallationStatusResponse
-keep class com.electroworld.staff.data.network.InstallationStatusResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
