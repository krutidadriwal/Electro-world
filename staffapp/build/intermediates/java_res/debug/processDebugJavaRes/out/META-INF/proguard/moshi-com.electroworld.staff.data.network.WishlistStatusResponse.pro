-keepnames class com.electroworld.staff.data.network.WishlistStatusResponse
-if class com.electroworld.staff.data.network.WishlistStatusResponse
-keep class com.electroworld.staff.data.network.WishlistStatusResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
