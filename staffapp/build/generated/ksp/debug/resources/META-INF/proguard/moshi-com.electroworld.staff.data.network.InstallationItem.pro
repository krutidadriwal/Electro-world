-keepnames class com.electroworld.staff.data.network.InstallationItem
-if class com.electroworld.staff.data.network.InstallationItem
-keep class com.electroworld.staff.data.network.InstallationItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
