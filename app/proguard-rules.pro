# GlassGrab R8 Full Mode Rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.glassgrab.**$$serializer { *; }
-keepclassmembers class com.glassgrab.** {
    *** Companion;
}
-keepclasseswithmembers class com.glassgrab.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# youtubedl-android / FFmpeg / Aria2c
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room & Hilt
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
