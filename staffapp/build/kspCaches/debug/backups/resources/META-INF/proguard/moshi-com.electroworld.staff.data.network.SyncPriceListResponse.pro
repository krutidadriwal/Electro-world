-keepnames class com.electroworld.staff.data.network.SyncPriceListResponse
-if class com.electroworld.staff.data.network.SyncPriceListResponse
-keep class com.electroworld.staff.data.network.SyncPriceListResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
