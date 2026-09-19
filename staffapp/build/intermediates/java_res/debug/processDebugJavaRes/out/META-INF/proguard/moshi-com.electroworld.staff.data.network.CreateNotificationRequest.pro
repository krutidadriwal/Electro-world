-keepnames class com.electroworld.staff.data.network.CreateNotificationRequest
-if class com.electroworld.staff.data.network.CreateNotificationRequest
-keep class com.electroworld.staff.data.network.CreateNotificationRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.CreateNotificationRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.CreateNotificationRequest {
    public synthetic <init>(java.lang.String,int,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
