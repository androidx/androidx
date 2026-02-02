/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection;

import androidx.annotation.NonNull;

/**
 * This interface provides inspector specific utilities, such as
 * managed threads and ARTTI features.
 */
public interface InspectorEnvironment {

    /**
     * Executors provided by App Inspection Platforms. Clients should use it instead of
     * creating their own.
     */
    @NonNull
    default InspectorExecutors executors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Interface that provides ART TI capabilities.
     */
    @NonNull
    ArtTooling artTooling();
}
