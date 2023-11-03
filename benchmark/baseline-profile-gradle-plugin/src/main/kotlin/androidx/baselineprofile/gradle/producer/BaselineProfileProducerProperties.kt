/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.producer

/**
 * This property determines whether the test task should actually run to generate the baseline
 * profile artifacts. When this property is set, the test tasks will be disabled and baseline
 * profile won't be generated. This property is useful for Github CI, as default ubuntu runners
 * cannot run android emulators.
 *
 * An example of how to configure baseline profile generation on Github CI is in the project
 * NowInAndroid:
 * https://github.com/android/nowinandroid/blob/main/.github/workflows/AndroidCIWithGmd.yaml#L42
 * It's necessary to set `runs-on: macos-12` and properties -Dorg.gradle.workers.max=1
 * -Pandroid.testoptions.manageddevices.emulator.gpu="swiftshader_indirect.
 */
internal const val PROP_SKIP_GENERATION = "androidx.baselineprofile.skipgeneration"
