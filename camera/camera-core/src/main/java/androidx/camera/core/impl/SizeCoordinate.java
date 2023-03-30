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

package androidx.camera.core.impl;

/**
 * The size coordinate system enum.
 */
public enum SizeCoordinate {
    /**
     * Size is expressed in the camera sensor's natural orientation (landscape).
     */
    CAMERA_SENSOR,

    /**
     * Size is expressed in the Android View's orientation.
     */
    ANDROID_VIEW
}
