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

import java.util.Objects;

/** Implementation of the Impress nodes wrapper. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ImpressNode {
    private static final String TAG = ImpressNode.class.getSimpleName();
    private final int mHandle;

    public ImpressNode(int handle) {
        mHandle = handle;
    }

    public int getHandle() {
        return mHandle;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        ImpressNode node = (ImpressNode) obj;
        return mHandle == node.mHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHandle);
    }
}
