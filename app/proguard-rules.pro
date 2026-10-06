# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Firebase Rules ---
-keep class com.google.firebase.** { *; }

# --- Room Rules ---
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

# --- General rules to keep annotations and JSON serialization ---
-keepattributes *Annotation*

# Keep classes that are used for serialization
-keepnames class com.example.data.model.** { *; }
