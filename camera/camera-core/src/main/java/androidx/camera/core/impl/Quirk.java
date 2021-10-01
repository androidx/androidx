/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.RequiresApi;

/**
 * Defines an inconsistency, a limitation, or any behavior that deviates from the standard behavior.
 *
 * <p> This class is used to define both device specific and camera specific quirks. Device
 * specific quirks depend on device related information, such as the device's brand, model and OS
 * level. Whereas camera related quirks depend on the camera id and/or camera characteristics.
 *
 * @see Quirks
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface Quirk {
}
