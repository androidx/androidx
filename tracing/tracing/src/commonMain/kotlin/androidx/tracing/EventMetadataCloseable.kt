/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing

/** A holder for a [EventMetadata] and the [AutoCloseable]. */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@DelicateTracingApi
public class EventMetadataCloseable(
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var metadata: EventMetadata = EmptyEventMetadata,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var closeable: AutoCloseable = EmptyCloseable,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    // beginEventWithMetadata tells us the actual propagation token that was used.
    @JvmField
    public var propagationToken: PropagationToken = PropagationUnsupportedToken,
)

/** The empty holder. */
@PublishedApi
internal val EmptyEventMetadataCloseable: EventMetadataCloseable = EventMetadataCloseable()
