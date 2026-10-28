# Add project specific ProGuard rules here.

# --- Performance & Optimization Passes ---
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

# Preserve line numbers for debugging and stack traces
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# --- Firebase & Google Play Services Rules ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# --- Room Database Rules ---
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class * extends androidx.room.Database
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <methods>;
}

# --- WorkManager Rules ---
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Jetpack Compose & Kotlinx Serialization ---
-keepattributes *Annotation*,Signature,EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepnames @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <init>(...);
}

# --- App Models & DTOs ---
-keep class com.example.data.model.** { *; }
-keep class com.example.ui.screens.** { *; }

# --- Coroutines Optimizations & Metadata ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class * implements kotlinx.coroutines.CoroutineExceptionHandler { *; }
