# General JNI keep rules
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes in the project's core packages from being renamed
-keep class me.robin.heion.** { *; }

# Keep LiteRT LM classes and their members
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }

# Keep reflection-targeted fields in LiteRT LM
-keepclassmembers class com.google.ai.edge.litertlm.ConversationConfig {
    private boolean enableThinking;
}
-keepclassmembers class com.google.ai.edge.litertlm.Message {
    private java.util.Map channels;
}

# Keep ONNX Runtime classes
-keep class ai.onnxruntime.** { *; }

# Keep AboutLibraries classes
-keep class com.mikepenz.aboutlibraries.** { *; }

# Keep all R classes to prevent resource-related crashes if referenced by name/reflection
-keep class **.R$* { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
