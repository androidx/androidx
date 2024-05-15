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

package androidx.lifecycle.viewmodel.internal

import androidx.lifecycle.ViewModel

/**
 * [JvmViewModelProviders] provides common helper functionalities.
 *
 * Kotlin Multiplatform does not support expect class with default implementation yet, so we
 * extracted the common logic used by all platforms to this internal class.
 *
 * @see <a href="https://youtrack.jetbrains.com/issue/KT-20427">KT-20427</a>
 */
internal object JvmViewModelProviders {

    /**
     * Creates a new [ViewModel] instance using the no-args constructor if available, otherwise
     * throws a [RuntimeException].
     */
    @Suppress("DocumentExceptions")
    fun <T : ViewModel> createViewModel(modelClass: Class<T>): T =
        try {
            modelClass.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Cannot create an instance of $modelClass", e)
        }
}
