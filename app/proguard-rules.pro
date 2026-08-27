# RNDM Proguard Rules

# Annotations & Attributes
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**

# Room Database
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Domain & Data Models
-keep class com.rndm.app.domain.model.** { *; }
-keep class com.rndm.app.data.local.entity.** { *; }
-keep class com.rndm.app.data.remote.model.** { *; }
-keep class com.rndm.app.data.local.dto.** { *; }
-keep class com.rndm.app.data.update.** { *; }
-keep interface com.rndm.app.data.update.** { *; }

# Moshi & JSON serialization
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepattributes *JsonClass*
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.**

# KotlinX Serialization
-keepattributes *Serializable*
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}

# Dagger / Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.Provides *;
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepclassmembers enum * { *; }

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.Immutable *;
    @androidx.compose.runtime.Stable *;
}
