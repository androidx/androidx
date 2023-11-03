/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string>
#include "perfetto/perfetto.h"
#include "trace_categories.h"
#include "tracing_perfetto.h"

// TODO: define API for categories
#define CATEGORY_RENDERING "rendering"

// Concept of version useful e.g. for human-readable error messages, and stable once released.
// Does not replace the need for a binary verification mechanism (e.g. checksum check).
// TODO: populate using CMake
#define VERSION "1.0.0-beta03"

namespace tracing_perfetto {
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
        // Note: key is ignored for now
        TRACE_EVENT_BEGIN(CATEGORY_RENDERING, nullptr, [&](perfetto::EventContext ctx) {
            ctx.event()->set_name(traceInfo);
        });
    }

    void TraceEventEnd() {
        TRACE_EVENT_END(CATEGORY_RENDERING);
    }

    const char* Version() {
        return VERSION;
    }
} // tracing_perfetto
