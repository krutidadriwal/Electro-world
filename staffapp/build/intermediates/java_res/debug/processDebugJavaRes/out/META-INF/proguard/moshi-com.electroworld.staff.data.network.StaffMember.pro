-keepnames class com.electroworld.staff.data.network.StaffMember
-if class com.electroworld.staff.data.network.StaffMember
-keep class com.electroworld.staff.data.network.StaffMemberJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.StaffMember
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.StaffMember {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
