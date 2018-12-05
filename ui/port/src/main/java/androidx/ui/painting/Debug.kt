/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting

/**
 * Whether to replace all shadows with solid color blocks.
 *
 * This is useful when writing golden file tests (see [matchesGoldenFile]) since
 * the rendering of shadows is not guaranteed to be pixel-for-pixel identical from
 * version to version (or even from run to run).
 */
val debugDisableShadows: Boolean = false