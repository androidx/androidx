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

package androidx.camera.extensions;

import androidx.annotation.IntRange;
import androidx.camera.core.CameraControl;

/**
 * A camera extensions control instance that allows customization of capture request settings for
 * supported camera extensions.
 *
 * <p>Applications can leverage the
 * {@link ExtensionsManager#getCameraExtensionsControl(CameraControl)} method to acquire a
 * CameraExtensionsControl object to manage extension-related settings.
 */
public interface CameraExtensionsControl {
    /**
     * Sets the extension strength for the extension mode associated with the
     * CameraExtensionsControl.
     *
     * <p>Strength equal to 0 means that the extension must not apply any post-processing and
     * return a regular captured frame. Strength equal to 100 is the default level of
     * post-processing applied when the control is not supported or not set by the client. Values
     * between 0 and 100 will have different effect depending on the extension type as described
     * below:
     * <ul>
     *     <li>{@link ExtensionMode#BOKEH} - the strength will control the amount of blur.
     *     <li>{@link ExtensionMode#HDR} and {@link ExtensionMode#NIGHT} - the strength will
     *     control the amount of images fused and the brightness of the final image.
     *     <li>{@link ExtensionMode#FACE_RETOUCH} - the strength value will control the amount of
     *     cosmetic enhancement and skin smoothing.
     * </ul>
     *
     * <p>This will be supported if the
     * {@link CameraExtensionsInfo#isExtensionStrengthAvailable()} associated to the same
     * extensions enabled camera returns {@code true}. Invoking this method will be no-op if
     * extension strength is not supported.
     *
     * @param strength the new extension strength value
     */
    default void setExtensionStrength(@IntRange(from = 0, to = 100) int strength){
    }
}
