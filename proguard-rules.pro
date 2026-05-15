-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.ymid.wakeonlan.BuildConfig { *; }
-dontwarn org.slf4j.**
-dontwarn sun.security.x509.**
-dontwarn javax.naming.**
-dontwarn sun.misc.**

# BouncyCastle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# Room — keep entities and DAOs so R8 doesn't strip generated _Impl classes
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WorkManager — constructors must be kept so the factory can instantiate workers
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# sshj
-keep class net.schmizz.sshj.** { *; }
-dontwarn net.schmizz.sshj.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Google Play Review
-keep class com.google.android.play.core.** { *; }

##--- Gson -------------------------------------------------------------------
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
