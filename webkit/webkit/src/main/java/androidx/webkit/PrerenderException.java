/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Super class for all asynchronous exceptions produced by {@link WebViewCompat} prerender
 * operations.
 */
public class PrerenderException extends Exception {
    /**
     * Constructs a new PrerenderException with the specified failure message and cause.
     *
     * @param error The detail message of the failure. This parameter cannot be null.
     * @param cause The cause of the failure. A {@code null} value is permitted, and indicates
     *              that the cause is nonexistent or unknown.
     */
    public PrerenderException(@NonNull String error, @Nullable Throwable cause) {
        super(error, cause);
    }
}
