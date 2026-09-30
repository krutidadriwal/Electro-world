-keepnames class com.electroworld.staff.data.network.WishlistCallResponse
-if class com.electroworld.staff.data.network.WishlistCallResponse
-keep class com.electroworld.staff.data.network.WishlistCallResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
