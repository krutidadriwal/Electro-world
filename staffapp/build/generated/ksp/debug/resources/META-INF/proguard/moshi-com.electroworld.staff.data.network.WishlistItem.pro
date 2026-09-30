-keepnames class com.electroworld.staff.data.network.WishlistItem
-if class com.electroworld.staff.data.network.WishlistItem
-keep class com.electroworld.staff.data.network.WishlistItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
