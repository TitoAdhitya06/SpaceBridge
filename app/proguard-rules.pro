# SpaceBridge v3 ProGuard / R8 Rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    java.lang.Object deserialize(...);
    void serialize(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# Retrofit 2
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-dontwarn kotlinx.coroutines.**

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Firebase & Google Play Services
-keepattributes *Annotation*
-dontwarn com.google.android.gms.**
-keepclassmembers class com.example.spacebridgeapp.core.data.notes.NoteDto { *; }
-keepclassmembers class com.example.spacebridgeapp.core.data.notes.ChecklistItemDto { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
