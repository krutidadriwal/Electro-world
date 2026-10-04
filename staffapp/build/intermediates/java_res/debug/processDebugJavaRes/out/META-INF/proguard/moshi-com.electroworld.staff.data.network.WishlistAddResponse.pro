-keepnames class com.electroworld.staff.data.network.WishlistAddResponse
-if class com.electroworld.staff.data.network.WishlistAddResponse
-keep class com.electroworld.staff.data.network.WishlistAddResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
