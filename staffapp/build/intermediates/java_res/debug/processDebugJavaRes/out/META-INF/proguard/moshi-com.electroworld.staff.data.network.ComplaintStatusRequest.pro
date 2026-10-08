-keepnames class com.electroworld.staff.data.network.ComplaintStatusRequest
-if class com.electroworld.staff.data.network.ComplaintStatusRequest
-keep class com.electroworld.staff.data.network.ComplaintStatusRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.ComplaintStatusRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.ComplaintStatusRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
