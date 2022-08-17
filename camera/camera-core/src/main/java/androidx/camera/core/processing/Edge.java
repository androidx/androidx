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

import android.media.ImageWriter;
import android.view.Surface;

/**
 * A handler to a publisher/subscriber pair.
 *
 * <p>Upstream units publish the frames to the edge, which will be delivered to the downstream
 * subscribers. Edge usually contains both the image buffer and the specifications about the
 * image, such as the size and format of the image buffer.
 *
 * <p>One example is Android’s {@link Surface} API. Surface is a handler to a {@code BufferQueue}.
 * The publisher, e.g. camera, sends images to the {@link Surface} by using APIs such as
 * {@link ImageWriter}, OpenGL and/or NDK. Subscribers can get the frames by using APIs such as
 * {@code ImageReader.OnImageAvailableListener} or {@code SurfaceTexture.OnFrameAvailableListener}.
 */
public interface Edge {
}
