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

import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureResult;

/**
 * Base unit for CameraX post-processing.
 *
 * <p>All CameraX post-processing should be wrapped by this interface to explicitly define the I/O.
 *
 * <p>Both {@link I} and {@link O} should include handlers to buffers that
 * contain camera frames, as well as the callbacks to notify when the frames are updated. One
 * example of such buffer is a {@link Surface}. e.g. {@link Surface} itself is a handler to a
 * {@code GraphicBuffer}, and one can get the callback in the form of {@code SurfaceTexture
 * .OnFrameAvailableListener} or{@link android.media.ImageReader.OnImageAvailableListener}.
 *
 * <p>The buffers are usually for image frames, but they could also contain other types of data
 * such as {@link CameraCaptureResult}, EXIF or depth. Both input and output could contain one
 * or multiple buffers. If there are multiple inputs and/or outputs, it's the implementation's
 * responsibility to properly share or merge the streams.
 *
 * <p>Besides the buffers, the I/O usually carry additional information about the buffer, such
 * as dimension, format and transformation. Usually, the {@link I} includes instructions on
 * how the buffer should be edited. If there are multiple outputs, there should be one
 * instruction per output streams.
 *
 * <p>The pipeline will be built in the direction from the camera to the app. The input of the
 * first node will be the direct output from the camera. Each subsequent node provide buffers for
 * the upstream nodes to write to, and demand buffers from the downstream nodes. For the nodes
 * that doing actual image processing, they usually need to allocate and maintain buffers.
 *
 * <p>Nodes should be stateful, e.g. keeping track of the previous I/O and the buffer allocated,
 * so that the pipeline can be partially recreated for efficiency. For example, one may need to
 * change post-processing effects without reconfiguring the {@link Surface}s for camera output and
 * app display.
 *
 * @param <I>  input specifications
 * @param <O> output specifications
 */
public interface Node<I, O> {

    /**
     * Transforms an input specification to an output specification.
     *
     * <p>This method will be invoked in {@code UseCase#createPipeline}. For now, {@code
     * #createPipeline}s are called on the main thread.
     *
     * <p> Returns {@code null} if the input does not change the current state of the
     * {@link Node}. This usually happens when the input specification can be handled by the
     * previously allocated buffer, thus no new buffer needs to be allocated. The node will
     * provide the existing buffer for the upstream node to write to.
     */
    @NonNull
    @MainThread
    O transform(@NonNull I i);

    /**
     * Releases the node.
     *
     * <p>Releases all the resources allocated by the node, including threads, buffers, GL context
     * etc. Once released, the node can never be brought back to life.
     *
     * <p>This method is usually called during {@link UseCase#onDetached()}. It also can be called
     * outside of {@link UseCase#onDetached()}. If the pipeline is rebuilt on-the-fly, e.g.
     * target rotation is changed by the app, then the node should be released right away. On the
     * other hand, it's also possible to not release even after {@link UseCase#onDetached()}, if the
     * node needs to be kept alive across lifecycles. For example, for front/back camera switching
     * during video recording.
     */
    void release();
}
