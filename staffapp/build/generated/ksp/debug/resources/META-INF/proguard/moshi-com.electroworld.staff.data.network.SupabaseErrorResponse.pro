-keepnames class com.electroworld.staff.data.network.SupabaseErrorResponse
-if class com.electroworld.staff.data.network.SupabaseErrorResponse
-keep class com.electroworld.staff.data.network.SupabaseErrorResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.SupabaseErrorResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.SupabaseErrorResponse {
    public synthetic <init>(java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
