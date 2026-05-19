# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.github.itskenny0.r1ha.**$$serializer { *; }
-keepclassmembers class com.github.itskenny0.r1ha.** {
    *** Companion;
}
-keepclasseswithmembers class com.github.itskenny0.r1ha.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Strip verbose logs from release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

# Strip our own debug/verbose logs from release too. R1Log.d is called on every
# wheel detent (CardStackViewModel hot path) and assembling the StringBuilder
# argument allocates per-call; -assumenosideeffects lets R8 drop the call and
# its arguments entirely in optimised release builds.
-assumenosideeffects class com.github.itskenny0.r1ha.core.util.R1Log {
    public *** d(...);
    public *** v(...);
}
