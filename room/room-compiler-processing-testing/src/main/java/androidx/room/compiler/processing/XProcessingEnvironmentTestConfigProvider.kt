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

import java.util.ServiceLoader

/**
 * This interface can be implemented by XProcessingTesting clients to provide a default
 * configuration for all runProcessingTest calls.
 *
 * XProcessingTesting will find the implementation via a [ServiceLoader]. To register your
 * implementation, add the following file
 * `androidx.room.compiler.processing.XProcessingEnvironmentTestConfigProvider` into
 * `resources/META-INF/services/` folder (test source path). The contents of the file should have
 * the fully qualified name of your implementation class.
 */
interface XProcessingEnvironmentTestConfigProvider {
    fun configure(options: Map<String, String>): XProcessingEnvConfig

    companion object {
        private val instance: XProcessingEnvironmentTestConfigProvider? by lazy {
            val implementations = ServiceLoader.load(
                XProcessingEnvironmentTestConfigProvider::class.java
            ).toList()
            if (implementations.size >= 2) {
                error(
                    "Multiple XProcessingEnvironmentTestConfigProvider implementations " +
                        "were found: $implementations. There can be only 1 or 0."
                )
            }
            implementations.firstOrNull()
        }

        internal fun createConfig(options: Map<String, String>): XProcessingEnvConfig {
            return instance?.configure(options) ?: XProcessingEnvConfig.DEFAULT
        }
    }
}
