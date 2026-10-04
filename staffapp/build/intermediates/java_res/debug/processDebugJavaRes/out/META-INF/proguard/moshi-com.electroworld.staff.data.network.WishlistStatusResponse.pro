-keepnames class com.electroworld.staff.data.network.WishlistStatusResponse
-if class com.electroworld.staff.data.network.WishlistStatusResponse
-keep class com.electroworld.staff.data.network.WishlistStatusResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.WishlistStatusResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.WishlistStatusResponse {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
