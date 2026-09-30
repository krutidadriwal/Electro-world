-keepnames class com.electroworld.staff.data.network.WishlistCallRequest
-if class com.electroworld.staff.data.network.WishlistCallRequest
-keep class com.electroworld.staff.data.network.WishlistCallRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
