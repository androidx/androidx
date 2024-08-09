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

package androidx.camera.camera2.pipe.integration.impl

import androidx.camera.camera2.pipe.Metadata
import androidx.camera.core.impl.TagBundle

/** Custom tags that can be passed used by CameraPipe */
public val CAMERAX_TAG_BUNDLE: Metadata.Key<TagBundle> =
    Metadata.Key.create<TagBundle>("camerax.tag_bundle")
public val USE_CASE_CAMERA_STATE_CUSTOM_TAG: Metadata.Key<Int> =
    Metadata.Key.create<Int>("use_case_camera_state.tag")
