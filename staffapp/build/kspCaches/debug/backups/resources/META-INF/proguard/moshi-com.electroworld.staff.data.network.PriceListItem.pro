-keepnames class com.electroworld.staff.data.network.PriceListItem
-if class com.electroworld.staff.data.network.PriceListItem
-keep class com.electroworld.staff.data.network.PriceListItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.electroworld.staff.data.network.PriceListItem
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.electroworld.staff.data.network.PriceListItem {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,double,double,java.lang.Double,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
