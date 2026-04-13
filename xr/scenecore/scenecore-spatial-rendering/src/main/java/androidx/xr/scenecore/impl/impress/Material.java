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
import androidx.xr.scenecore.runtime.MaterialResource;

import org.jspecify.annotations.NonNull;

/** Interface defining the common functionality of all materials. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class Material extends BindingsResource implements MaterialResource {
    private final ImpressApi mImpressApi;

    protected Material(@NonNull ImpressApi impressApi, long nativeMaterial) {
        super(
                impressApi.getBindingsResourceManager(),
                nativeMaterial,
                impressApi::destroyNativeObject);
        mImpressApi = impressApi;
    }

    @Override
    protected void releaseBindingsResource(long nativeHandle) {
        mImpressApi.destroyNativeObject(nativeHandle);
    }
}
