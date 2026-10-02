-keepnames class com.electroworld.staff.data.network.Subcategory
-if class com.electroworld.staff.data.network.Subcategory
-keep class com.electroworld.staff.data.network.SubcategoryJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
