/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video

import androidx.annotation.RestrictTo

/**
 * Denotes that the annotated method uses an experimental path for high-speed/slow-motion recording.
 *
 * High-speed recording, also the basis for slow-motion recording, captures video at a significantly
 * higher frame rate than standard video, allowing for detailed playback of fast-paced actions in
 * slow motion.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn
public annotation class ExperimentalHighSpeedVideo
