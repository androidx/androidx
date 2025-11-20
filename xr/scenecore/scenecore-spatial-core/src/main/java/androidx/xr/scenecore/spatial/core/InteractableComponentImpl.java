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

package androidx.xr.scenecore.spatial.core;

import android.util.Log;

import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.InputEventListener;
import androidx.xr.scenecore.runtime.InteractableComponent;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

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
        } else if (entity instanceof SurfaceEntityImpl) {
            ((SurfaceEntityImpl) entity).setColliderEnabled(true);
        }
        // InputEvent type translation happens here.
        entity.addInputEventListener(mExecutor, mConsumer);
        return true;
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        if (entity instanceof GltfEntityImpl) {
            ((GltfEntityImpl) entity).setColliderEnabled(false);
        } else if (entity instanceof SurfaceEntityImpl) {
            ((SurfaceEntityImpl) entity).setColliderEnabled(false);
        }
        entity.removeInputEventListener(mConsumer);
        mEntity = null;
    }
}
