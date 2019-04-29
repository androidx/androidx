/*
 * Copyright (C) 2019 The Android Open Source Project
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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/** A set of {@link ImageProxy} which are mapped an identifier. */
public interface ImageProxyBundle {
    /**
     * Get a {@link ListenableFuture} for a {@link ImageProxy}.
     *
     * <p> The future will be satisfied when the {@link ImageProxy} for the given identifier has
     * been generated.
     *
     * @param captureId The id for the captures that generated the {@link ImageProxy}.
     */
    ListenableFuture<ImageProxy> getImageProxy(int captureId);

    /**
     * Returns the list of identifiers for the capture that produced the data in
     * this bundle.
     */
    List<Integer> getCaptureIds();
}
