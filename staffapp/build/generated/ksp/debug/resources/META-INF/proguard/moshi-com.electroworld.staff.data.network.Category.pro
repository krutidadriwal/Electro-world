-keepnames class com.electroworld.staff.data.network.Category
-if class com.electroworld.staff.data.network.Category
-keep class com.electroworld.staff.data.network.CategoryJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
