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

package androidx.activity

import androidx.activity.result.ActivityResultLauncher

/**
 * Convenience method to launch a prepared call using an invoke operator.
 */
operator fun <I> ActivityResultLauncher<I>.invoke(input: I) = launch(input)

/**
 * Convenience method to launch a no-argument prepared call using an invoke operator
 * without arguments.
 */
operator fun ActivityResultLauncher<Void?>.invoke() = launch(null)

/**
 * Convenience method to launch a no-argument prepared call using an invoke operator
 * without arguments.
 */
@JvmName("invokeUnit")
operator fun ActivityResultLauncher<Unit>.invoke() = launch(null)