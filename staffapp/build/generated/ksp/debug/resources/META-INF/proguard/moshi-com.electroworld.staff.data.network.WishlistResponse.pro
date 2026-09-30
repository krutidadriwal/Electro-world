-keepnames class com.electroworld.staff.data.network.WishlistResponse
-if class com.electroworld.staff.data.network.WishlistResponse
-keep class com.electroworld.staff.data.network.WishlistResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
