-keepnames class com.electroworld.staff.data.network.WishlistStatusRequest
-if class com.electroworld.staff.data.network.WishlistStatusRequest
-keep class com.electroworld.staff.data.network.WishlistStatusRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
