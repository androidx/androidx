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

import android.os.Handler;
import android.util.Log;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the lifecycle of Impress objects by hooking into the JVM Garbage Collector using
 * PhantomReference objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BindingsResourceManager {
    private static final String RESOURCE_MANAGER_THREAD = "resource_manager_thread";
    private final String mTAG = getClass().getSimpleName();
    private final Handler mMainThreadHandler;
    private final ReferenceQueue<Object> mQueue = new ReferenceQueue<>();
    private final Set<BindingsObjectPhantomReference> mPhantomReferences =
            Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean mIsDisabled = new AtomicBoolean(false);

    public BindingsResourceManager(@NonNull Handler mainThreadHandler) {
        this.mMainThreadHandler = mainThreadHandler;
        Thread thread = new Thread(this::processQueue, RESOURCE_MANAGER_THREAD);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Registers the resource object's PhantomReference in the BindingsResourceManager.
     *
     * @param object The resource object that is getting registered.
     * @param callback The callback that gets triggered when the object is garbage collected.
     */
    public void register(@NonNull Object object, @NonNull Runnable callback) {
        mPhantomReferences.add(new BindingsObjectPhantomReference(object, mQueue, callback));
    }

    /**
     * Stops the resource manager from processing any further cleanup callbacks after the rendering
     * engine is torn down.
     */
    public void disable() {
        mIsDisabled.set(true);
    }

    private void processQueue() {
        while (true) {
            try {
                BindingsObjectPhantomReference ref =
                        (BindingsObjectPhantomReference) mQueue.remove();
                mMainThreadHandler.post(
                        () -> {
                            mPhantomReferences.remove(ref);

                            if (!mIsDisabled.get()) {
                                ref.cleanup();
                            }
                        });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(mTAG, "Queue processing thread was interrupted and is now terminating.");
                break;
            }
        }
    }
}
