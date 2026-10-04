-keepnames class com.electroworld.staff.data.network.InstallationStatusRequest
-if class com.electroworld.staff.data.network.InstallationStatusRequest
-keep class com.electroworld.staff.data.network.InstallationStatusRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
