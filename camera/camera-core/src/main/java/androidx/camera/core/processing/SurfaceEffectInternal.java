/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing;

import androidx.annotation.NonNull;
import androidx.camera.core.SurfaceEffect;

import java.util.concurrent.Executor;

/**
 * An internal {@link SurfaceEffect} that is releasable.
 */
public interface SurfaceEffectInternal extends SurfaceEffect {

    /**
     * Gets the executor on which the interface will be invoked.
     *
     * <p>For external implementations, the executor is provided when the {@link SurfaceEffect}
     * is set. Internal implementations must provide the executor themselves.
     */
    @NonNull
    Executor getExecutor();

    /**
     * Releases all the resources allocated by the effect.
     *
     * <p>An effect created by CameraX should be released by CameraX when it's no longer needed.
     * On the other hand, an external effect should not be released by CameraX, because CameraX
     * not does know if the effect will be needed again. In that case, the app is responsible for
     * releasing the effect. It should be able to keep the effect alive across multiple
     * attach/detach cycles if it's necessary.
     *
     * @see Node#release()
     */
    void release();
}
