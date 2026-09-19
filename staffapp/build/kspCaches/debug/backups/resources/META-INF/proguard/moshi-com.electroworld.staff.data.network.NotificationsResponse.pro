-keepnames class com.electroworld.staff.data.network.NotificationsResponse
-if class com.electroworld.staff.data.network.NotificationsResponse
-keep class com.electroworld.staff.data.network.NotificationsResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
