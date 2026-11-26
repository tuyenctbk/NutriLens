# NutriLens ProGuard / R8 Keep Rules

# Keep Moshi generated adapters and annotated models
-keep class com.example.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep Retrofit service interface & annotations
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Android JobService & BroadcastReceiver components
-keep class com.example.notification.PantryReminderJobService { *; }
-keep class com.example.notification.PantryActionReceiver { *; }
