-keepnames class com.electroworld.staff.data.network.NotificationItem
-if class com.electroworld.staff.data.network.NotificationItem
-keep class com.electroworld.staff.data.network.NotificationItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.NotificationItem
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.NotificationItem {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,java.lang.String,java.lang.String,boolean,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
