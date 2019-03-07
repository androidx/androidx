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

// Always inline ktx extension methods unless we have additional call site costs.
@file:Suppress("NOTHING_TO_INLINE")

package androidx.work

import androidx.annotation.NonNull
import kotlin.reflect.KClass

/**
 * Creates a [OneTimeWorkRequest] with the given [ListenableWorker].
 */
inline fun <reified W : ListenableWorker> OneTimeWorkRequestBuilder() =
        OneTimeWorkRequest.Builder(W::class.java)

/**
 * Sets an [InputMerger] on the [OneTimeWorkRequest.Builder].
 */
inline fun OneTimeWorkRequest.Builder.setInputMerger(
    @NonNull inputMerger: KClass<out InputMerger>
) = setInputMerger(inputMerger.java)
