# Production Obfuscation & Shrinking Rules for GAMBIT

# Preserve line numbers and source files for clean stack traces in production crash logs
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Deprecated,*Annotation*

# Keep Firestore mapping models from being obfuscated (critical for document reflection mapping)
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.domain.model.** { *; }
-keep class com.example.domain.model.** { *; }

# Keep Firebase Auth and Firestore internal classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.firebase-auth-api.** { *; }
-dontwarn com.google.firebase.**

# Keep Jetpack Room DB entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.entity.** { *; }
-dontwarn androidx.room.**

# Keep Hilt / Dagger generated injection bindings
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * extends dagger.hilt.internal.GeneratedComponentManager
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory
-keep class **_HiltModules* { *; }
-keep class **_HiltModules_BindsModule { *; }
-keep class **_HiltModules_KeyModule { *; }
-keep class **_HiltModules_DeclareRoles { *; }
-keep class **_HiltModules_GeneratedClass { *; }
-keep class * extends dagger.internal.Factory
-keep class * extends javax.inject.Provider

# Keep Kotlinx Coroutines and Serialization
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn kotlinx.coroutines.**
