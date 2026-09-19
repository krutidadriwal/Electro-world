-keepnames class com.electroworld.staff.data.network.UploadNotificationImageRequest
-if class com.electroworld.staff.data.network.UploadNotificationImageRequest
-keep class com.electroworld.staff.data.network.UploadNotificationImageRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
