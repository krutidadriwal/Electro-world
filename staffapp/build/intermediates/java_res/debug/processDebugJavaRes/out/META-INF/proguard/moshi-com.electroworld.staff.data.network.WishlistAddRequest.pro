-keepnames class com.electroworld.staff.data.network.WishlistAddRequest
-if class com.electroworld.staff.data.network.WishlistAddRequest
-keep class com.electroworld.staff.data.network.WishlistAddRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.WishlistAddRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.WishlistAddRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
