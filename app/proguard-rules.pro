# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**
