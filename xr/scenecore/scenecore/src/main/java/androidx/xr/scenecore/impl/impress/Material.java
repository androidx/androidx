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

package androidx.xr.scenecore.impl.impress;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** Interface defining the common functionality of all materials. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Material {
    private final ImpressApi impressApi;
    private final long nativeMaterial;

    @NonNull
    public Material(@NonNull ImpressApi impressApi, long nativeMaterial) {
        this.impressApi = impressApi;
        this.nativeMaterial = nativeMaterial;
    }

    /** Returns the native handle of the Impress material. */
    public long getNativeHandle() {
        return nativeMaterial;
    }

    /** Destroys the Impress material object using the native handle. */
    public void destroyNativeObject() {
        impressApi.destroyNativeObject(nativeMaterial);
    }
}
