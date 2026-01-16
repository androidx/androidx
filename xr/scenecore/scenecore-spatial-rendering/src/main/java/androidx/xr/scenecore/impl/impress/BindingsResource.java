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

import android.util.Log;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Parent class for common bindings resource operations. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class BindingsResource {
    private final String mTAG = getClass().getSimpleName();
    private final long mNativeHandle;
    private final AtomicBoolean mIsDestroyed;

    protected BindingsResource(
            @NonNull BindingsResourceManager resourceManager,
            long nativeHandle,
            @NonNull Consumer<Long> destroyer) {

        this.mNativeHandle = nativeHandle;

        AtomicBoolean sharedDestroyedState = new AtomicBoolean(false);
        this.mIsDestroyed = sharedDestroyedState;

        Runnable cleanupCallback =
                () -> {
                    if (sharedDestroyedState.compareAndSet(false, true)) {
                        // Not using mTAG to avoid holding a reference of the class in the callback.
                        Log.d(
                                "BindingsResource",
                                "Bindings resource with handle "
                                        + nativeHandle
                                        + " is destroyed via GC");

                        destroyer.accept(nativeHandle);
                    }
                };
        resourceManager.register(this, cleanupCallback);
    }

    /** Destroys the bindings resource. */
    public final void destroy() {
        throwIfDestroyed();
        if (mIsDestroyed.compareAndSet(false, true)) {
            Log.d(
                    mTAG,
                    "Bindings resource with handle " + mNativeHandle + " is explicitly destroyed");
            releaseBindingsResource(mNativeHandle);
        }
    }

    /** Returns the native handle of the bindings resource. */
    public long getNativeHandle() {
        throwIfDestroyed();
        return mNativeHandle;
    }

    protected void throwIfDestroyed() {
        if (mIsDestroyed.get()) {
            throw new IllegalStateException(mTAG + " has already been destroyed.");
        }
    }

    protected abstract void releaseBindingsResource(long nativeHandle);
}
