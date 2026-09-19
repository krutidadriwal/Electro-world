-keepnames class com.electroworld.staff.data.network.UploadNotificationImageResponse
-if class com.electroworld.staff.data.network.UploadNotificationImageResponse
-keep class com.electroworld.staff.data.network.UploadNotificationImageResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
