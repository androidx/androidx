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

import android.util.Log;

import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.InputEventListener;
import androidx.xr.runtime.internal.InteractableComponent;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/** Implementation of [JxrPlatformAdapter.InteractableComponent]. */
class InteractableComponentImpl implements InteractableComponent {
    final InputEventListener mConsumer;
    final Executor mExecutor;
    Entity mEntity;

    InteractableComponentImpl(Executor executor, InputEventListener consumer) {
        mConsumer = consumer;
        mExecutor = executor;
    }

    @Override
    public boolean onAttach(@NonNull Entity entity) {
        if (mEntity != null) {
            Log.e("Runtime", "Already attached to entity " + mEntity);
            return false;
        }
        mEntity = entity;
        if (entity instanceof GltfEntityImpl) {
            ((GltfEntityImpl) entity).setColliderEnabled(true);
        }
        // InputEvent type translation happens here.
        entity.addInputEventListener(mExecutor, mConsumer);
        return true;
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        if (entity instanceof GltfEntityImpl) {
            ((GltfEntityImpl) entity).setColliderEnabled(false);
        }
        entity.removeInputEventListener(mConsumer);
        mEntity = null;
    }
}
