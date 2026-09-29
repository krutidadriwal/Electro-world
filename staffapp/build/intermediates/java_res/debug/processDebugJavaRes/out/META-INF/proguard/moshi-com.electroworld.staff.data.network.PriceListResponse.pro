-keepnames class com.electroworld.staff.data.network.PriceListResponse
-if class com.electroworld.staff.data.network.PriceListResponse
-keep class com.electroworld.staff.data.network.PriceListResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
