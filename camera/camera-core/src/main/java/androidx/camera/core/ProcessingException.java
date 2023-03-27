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

package androidx.camera.core;

/**
 * Exception throw when effects post-processing fails.
 *
 * <p>Implementation of {@link SurfaceProcessor} throws this exception from
 * {@link SurfaceProcessor#onInputSurface} or {@link SurfaceProcessor#onOutputSurface} when an
 * error occurs during effect processing.
 *
 * @see SurfaceProcessor
 */
public class ProcessingException extends Exception {
}

