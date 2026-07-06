# ML Kit models are loaded reflectively; keep their classes when minification is enabled.
-keep class com.google.mlkit.** { *; }
