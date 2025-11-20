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

/**
 * Callback interface for the prerender operation.
 */
public interface PrerenderOperationCallback {
    /**
     * Called when a prerendered page is activated (used for an actual navigation).
     */
    void onPrerenderActivated();

    /**
     * Called when the prerender operation fails.
     *
     * @param exception The exception indicating the cause of the failure.
     */
    void onError(@NonNull PrerenderException exception);

}
