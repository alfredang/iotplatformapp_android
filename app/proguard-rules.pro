# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.tertiaryinfotech.iotflow.** {
    *** Companion;
}
-keepclasseswithmembers @kotlinx.serialization.Serializable class com.tertiaryinfotech.iotflow.** {
    <fields>;
}
