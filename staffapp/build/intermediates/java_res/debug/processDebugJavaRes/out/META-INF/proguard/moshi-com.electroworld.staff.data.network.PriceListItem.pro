-keepnames class com.electroworld.staff.data.network.PriceListItem
-if class com.electroworld.staff.data.network.PriceListItem
-keep class com.electroworld.staff.data.network.PriceListItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
