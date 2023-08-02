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
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

/**
 * Class for processing a single image frame.
 *
 * <p>Both {@link I} and {@link O} contain one or many camera frames and their metadata such as
 * dimension, format and Exif.
 *
 * <p>This is a syntax sugar for the {@link Node} class. The purpose is for building a pipeline
 * intuitively without using {@link Node}'s publisher/subscriber model.
 *
 * @param <I> input image frame
 * @param <O> output image frame.
 */
public interface Processor<I, O> {

    /**
     * Processes an input frame and produces an output frame.
     *
     * <p>The implementation of method performs the image processing operations that usually
     * blocks the current thread. It must be invoked on a non-blocking thread. e.g.
     * {@link CameraXExecutors#ioExecutor()}.
     */
    @NonNull
    @WorkerThread
    O process(@NonNull I i) throws ImageCaptureException;
}
