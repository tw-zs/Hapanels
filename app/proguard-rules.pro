# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
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
# Sealed-interface descriptors (HaInbound, HaOutbound) need their $serializer
# objects intact under R8 full mode; the polymorphic discriminator otherwise
# resolves to Unknown for every frame. Keep the synthesised serializer modules
# the @Serializable sealed types generate.
-keepclassmembers class com.github.itskenny0.r1ha.core.ha.** {
    static <fields>;
    static <methods>;
}
# DataStore + AppSettings: full-mode R8 sometimes drops the no-arg constructor
# kotlinx.serialization needs for default-value deserialization of nested
# @Serializable data classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.github.itskenny0.r1ha.core.prefs.** {
    <init>(...);
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
