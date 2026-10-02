-keepnames class com.electroworld.staff.data.network.CategoriesResponse
-if class com.electroworld.staff.data.network.CategoriesResponse
-keep class com.electroworld.staff.data.network.CategoriesResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
