/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.work

import androidx.annotation.RestrictTo

/**
 * A factory object that creates [InputMerger] instances. The factory is invoked every
 * time a work runs. You can override the default implementation of this factory by manually
 * initializing [WorkManager] (see [WorkManager.initialize]
 * and specifying a new [InputMergerFactory] in
 * [Configuration.Builder.setInputMergerFactory].
 */
abstract class InputMergerFactory {
    /**
     * Override this method to create an instance of a [InputMerger] given its fully
     * qualified class name.
     *
     *
     * Throwing an [Exception] here will crash the application. If an
     * [InputMergerFactory] is unable to create an instance of a [InputMerger], it
     * should return `null` so it can delegate to the default [InputMergerFactory].
     *
     * @param className The fully qualified class name for the [InputMerger]
     * @return an instance of [InputMerger]
     */
    abstract fun createInputMerger(className: String): InputMerger?

    /**
     * Creates an instance of a [InputMerger] given its fully
     * qualified class name with the correct fallback behavior.
     *
     * @param className The fully qualified class name for the [InputMerger]
     * @return an instance of [InputMerger]
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createInputMergerWithDefaultFallback(className: String): InputMerger? {
        var inputMerger = createInputMerger(className)
        if (inputMerger == null) {
            inputMerger = fromClassName(className)
        }
        return inputMerger
    }
}

internal object NoOpInputMergerFactory : InputMergerFactory() {
    override fun createInputMerger(className: String) = null
}
