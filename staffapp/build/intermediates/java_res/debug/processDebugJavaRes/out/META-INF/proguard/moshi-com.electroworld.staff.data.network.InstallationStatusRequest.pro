-keepnames class com.electroworld.staff.data.network.InstallationStatusRequest
-if class com.electroworld.staff.data.network.InstallationStatusRequest
-keep class com.electroworld.staff.data.network.InstallationStatusRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.InstallationStatusRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.InstallationStatusRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
