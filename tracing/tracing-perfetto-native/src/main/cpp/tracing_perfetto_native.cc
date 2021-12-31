#include <jni.h>
#include <string>
#include <android/log.h>
#include "perfetto/perfetto.h"
#include "trace_categories.h"

// TODO: define API for categories
#define CATEGORY "rendering"

namespace tracing_perfetto_native {
    void RegisterWithPerfetto() {
        perfetto::TracingInitArgs args;
        // The backends determine where trace events are recorded. Here we
        // are going to use the system-wide tracing service, so that we can see our
        // app's events in context with system profiling information.
        args.backends = perfetto::kSystemBackend; // TODO: make this configurable

        perfetto::Tracing::Initialize(args);
        perfetto::TrackEvent::Register();
    }

    void TraceEventBegin(int key, const char *traceInfo) {
        TRACE_EVENT_BEGIN(CATEGORY, nullptr, [&](perfetto::EventContext ctx) {
            ctx.event()->set_name(std::string(traceInfo) + " key=" + std::to_string(key));
        });
    }

    void TraceEventEnd() {
        TRACE_EVENT_END(CATEGORY);
    }

    void Flush() {
        perfetto::TrackEvent::Flush();
    }

}

extern "C" {

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_NativeCalls_nativeRegisterWithPerfetto(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto_native::RegisterWithPerfetto();
    PERFETTO_LOG("Perfetto: initialized");
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_NativeCalls_nativeTraceEventBegin(
        JNIEnv *env, __unused jclass clazz, jint key, jstring traceInfo) {
    const char *traceInfoUtf = env->GetStringUTFChars(traceInfo, NULL);
    tracing_perfetto_native::TraceEventBegin(key, traceInfoUtf);
    PERFETTO_LOG("Perfetto: TraceEventBegin(%s key=%d)", traceInfoUtf, key);
    env->ReleaseStringUTFChars(traceInfo, traceInfoUtf);
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_NativeCalls_nativeTraceEventEnd(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto_native::TraceEventEnd();
    PERFETTO_LOG("Perfetto: TraceEventEnd()");
}

JNIEXPORT void JNICALL
Java_androidx_tracing_perfetto_jni_NativeCalls_nativeFlushEvents(
        JNIEnv *env, __unused jclass clazz) {
    tracing_perfetto_native::Flush();
    PERFETTO_LOG("Perfetto: Flush()");
}
} // tracing_perfetto_native
