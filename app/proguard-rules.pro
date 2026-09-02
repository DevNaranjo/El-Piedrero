# ======================================================================
# ProGuard / R8 Rules for "El Piedrero" (Ronda Canaria Android)
# ======================================================================

# Kotlin Serialization (Preservar serializers reflexivos y metadatos)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ZXing (Generación y escaneo de códigos QR)
-keep class com.google.zxing.** { *; }

# Modelos de Dominio y Red (NDJSON Data Classes)
-keep class com.app.rondacanaria.data.model.** { *; }
-keep class com.app.rondacanaria.domain.model.** { *; }

# Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
