# Preserve class methods (that we explicitly register in native code), if the class is preserved
-keepclassmembers class androidx.tracing.perfetto.jni.PerfettoNative {
    java.lang.String nativeVersion();
    void nativeRegisterWithPerfetto();
    void nativeTraceEventBegin(int, java.lang.String);
    void nativeTraceEventEnd();
}