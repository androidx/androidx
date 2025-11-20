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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/** Implementation of PhantomReference for managing the lifecycle of Impress objects. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BindingsObjectPhantomReference extends PhantomReference<Object> {
    private final Runnable mCallback;

    BindingsObjectPhantomReference(
            Object referent, ReferenceQueue<? super Object> queue, Runnable callback) {
        super(referent, queue);
        mCallback = callback;
    }

    void cleanup() {
        if (mCallback != null) {
            mCallback.run();
        }
    }
}
