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

package androidx.xr.scenecore.impl;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.InputEventListener;
import androidx.xr.runtime.internal.PointerCaptureComponent;
import androidx.xr.runtime.internal.PointerCaptureComponent.StateListener;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/** Implementation of PointerCaptureComponent. */
final class PointerCaptureComponentImpl implements PointerCaptureComponent {

    private final Executor mExecutor;
    private final StateListener mStateListener;
    private final InputEventListener mInputListener;

    private AndroidXrEntity mAttachedEntity;

    PointerCaptureComponentImpl(
            @NonNull Executor executor,
            @NonNull StateListener stateListener,
            @NonNull InputEventListener inputListener) {
        mExecutor = executor;
        mStateListener = stateListener;
        mInputListener = inputListener;
    }

    @Override
    public boolean onAttach(@NonNull Entity entity) {
        if (!(entity instanceof AndroidXrEntity) || mAttachedEntity != null) {
            return false;
        }

        mAttachedEntity = (AndroidXrEntity) entity;
        return mAttachedEntity.requestPointerCapture(mExecutor, mInputListener, mStateListener);
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        mAttachedEntity.stopPointerCapture();
        mAttachedEntity = null;
    }
}
