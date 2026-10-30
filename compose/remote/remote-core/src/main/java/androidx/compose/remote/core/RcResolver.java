/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Host-provided plugin interface for resolving an embedded RemoteCompose document URL or identifier
 * string into serialized RemoteCompose document bytes.
 *
 * <p>Resolving a URL to a {@code byte[]} is the responsibility of the host application (for
 * example, reading packaged raw resources, app assets, local disk caches, or pre-fetched network
 * payloads). Returning {@code null} indicates that the URL cannot be resolved or is not permitted
 * by the host.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RcResolver {
    /**
     * Resolve the given URL or document identifier into serialized RemoteCompose document bytes.
     *
     * @param url the URL or document identifier string requested by an embedded document
     * @return the serialized RemoteCompose document bytes, or {@code null} if unavailable
     */
    byte @Nullable [] resolve(@NonNull String url);
}
