-keepnames class com.electroworld.staff.data.network.ComplaintItem
-if class com.electroworld.staff.data.network.ComplaintItem
-keep class com.electroworld.staff.data.network.ComplaintItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
