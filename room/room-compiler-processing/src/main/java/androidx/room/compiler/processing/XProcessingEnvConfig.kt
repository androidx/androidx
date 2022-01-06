/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing

/**
 * Configuration class for XProcessingEnv where certain behaviors might be modified.
 *
 * See documentation for details.
 *
 * To create an instance from Java, use the provided [Builder] class.
 * To create an instance from Kotlin, you can either use the provided [Builder] or just [copy] an
 * existing configuration.
 *
 * If you are using XProcessing Testing library, you can set an implementation of
 * `XProcessingEnvironmentTestConfigProvider` via a service configuration to load your
 * default configuration in `runProcessorTest` calls.
 */
@Suppress("SyntheticAccessor", "DataClassPrivateConstructor")
data class XProcessingEnvConfig private constructor(
    /**
     * TODO: not implemented yet.
     */
    val excludeMethodsWithInvalidJvmSourceNames: Boolean = false
) {
    fun toBuilder() = Builder(this)

    class Builder(
        baseline: XProcessingEnvConfig = XProcessingEnvConfig()
    ) {
        private var instance = baseline

        fun excludeMethodsWithInvalidJvmSourceNames(value: Boolean) = apply {
            instance = instance.copy(
                excludeMethodsWithInvalidJvmSourceNames = value
            )
        }

        fun build(): XProcessingEnvConfig {
            return instance
        }
    }

    companion object {
        /**
         * Default configuration for XProcessingEnv
         */
        val DEFAULT = Builder().build()
    }
}