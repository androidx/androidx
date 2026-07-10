/**
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package androidx.build

import java.io.File
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/** Holder class used for lazily registering tasks using the new Lazy task execution API. */
data class LazyTaskRegistry(
    private val names: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
) {

    companion object {
        private const val KEY = "AndroidXAutoRegisteredTasks"
        private val lock = ReentrantLock()

        fun get(project: Project): LazyTaskRegistry {
            val existing = project.extensions.findByName(KEY) as? LazyTaskRegistry
            if (existing != null) {
                return existing
            }
            return lock.withLock {
                project.extensions.findByName(KEY) as? LazyTaskRegistry
                    ?: LazyTaskRegistry().also { project.extensions.add(KEY, it) }
            }
        }
    }
}

internal fun Project.lazyReadFile(fileName: String): Provider<String> {
    val fileProperty = objects.fileProperty().fileValue(File(getSupportRootFolder(), fileName))
    return providers.fileContents(fileProperty).asText
}
