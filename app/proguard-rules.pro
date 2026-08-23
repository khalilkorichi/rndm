# RNDM Proguard Rules
-keepattributes *Annotation*
-dontwarn javax.annotation.**
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
