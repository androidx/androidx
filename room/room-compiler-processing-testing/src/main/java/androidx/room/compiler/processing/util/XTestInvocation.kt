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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRoundEnv
import com.google.common.truth.Truth
import kotlin.reflect.KClass

/**
 * Data holder for XProcessing tests to access the processing environment.
 */
@ExperimentalProcessingApi
class XTestInvocation(
    processingEnv: XProcessingEnv,
    roundEnv: XRoundEnv
) {
    val processingEnv: XProcessingEnv = processingEnv
        get() {
            assertNotDisposed()
            return field
        }
    val roundEnv: XRoundEnv = roundEnv
        get() {
            assertNotDisposed()
            return field
        }

    /**
     * Set to true after callback is called to ensure the test does not re-use an invocation that
     * is no longer usable (no longer in the process method of the processor)
     */
    private var disposed = false

    /**
     * Extension mechanism to allow putting objects into invocation that can be retrieved later.
     */
    private val userData = mutableMapOf<KClass<*>, Any>()

    private val postCompilationAssertions = mutableListOf<CompilationResultSubject.() -> Unit>()

    val isKsp: Boolean = processingEnv.backend == XProcessingEnv.Backend.KSP

    /**
     * Registers a block that will be called with a [CompilationResultSubject] when compilation
     * finishes.
     *
     * Note that it is not safe to access the environment in this block.
     */
    fun assertCompilationResult(block: CompilationResultSubject.() -> Unit) {
        assertNotDisposed()
        postCompilationAssertions.add(block)
    }

    internal fun runPostCompilationChecks(
        compilationResultSubject: CompilationResultSubject
    ) {
        postCompilationAssertions.forEach {
            it(compilationResultSubject)
        }
    }

    fun <T : Any> getUserData(key: KClass<T>): T? {
        assertNotDisposed()
        @Suppress("UNCHECKED_CAST")
        return userData[key] as T?
    }

    fun <T : Any> putUserData(key: KClass<T>, value: T) {
        assertNotDisposed()
        userData[key] = value
    }

    fun <T : Any> getOrPutUserData(key: KClass<T>, create: () -> T): T {
        getUserData(key)?.let {
            return it
        }
        return create().also {
            putUserData(key, it)
        }
    }

    fun dispose() {
        disposed = true
    }

    private fun assertNotDisposed() {
        Truth.assertWithMessage("Cannot use a test invocation after it is disposed.")
            .that(disposed)
            .isFalse()
    }
}