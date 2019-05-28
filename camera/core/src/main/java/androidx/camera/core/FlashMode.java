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

/** The flash mode options when taking a picture using ImageCapture. */
public enum FlashMode {
    /**
     * Auto flash. The flash will be used according to the camera system's determination when taking
     * a picture.
     */
    AUTO,
    /** Always flash. The flash will always be used when taking a picture. */
    ON,
    /** No flash. The flash will never be used when taking a picture. */
    OFF
}
