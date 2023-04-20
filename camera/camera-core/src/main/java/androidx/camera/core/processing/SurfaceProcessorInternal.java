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

import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An internal {@link SurfaceProcessor} that is releasable.
 *
 * <p>Note: the implementation of this interface must be thread-safe. e.g. methods can be
 * safely invoked on any thread.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public interface SurfaceProcessorInternal extends SurfaceProcessor {

    /**
     * Releases all the resources allocated by the processor.
     *
     * <p>An processor created by CameraX should be released by CameraX when it's no longer needed.
     * On the other hand, an external processor should not be released by CameraX, because CameraX
     * not does know if the processor will be needed again. In that case, the app is responsible for
     * releasing the processor. It should be able to keep the processor alive across multiple
     * attach/detach cycles if it's necessary.
     *
     * @see Node#release()
     */
    void release();

    /**
     * Takes a snapshot of the next available frame and write it to JPEG outputs.
     */
    @NonNull
    default ListenableFuture<Void> snapshot(@IntRange(from = 0, to = 100) int jpegQuality) {
        return Futures.immediateFuture(null);
    }
}
