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

package androidx.build.clang

import org.gradle.api.Action
import org.gradle.api.Project

/** Not internal to be able to use in buildSrc-tests */
class AndroidXClang(val project: Project) {
    private val multiTargetNativeCompilations = mutableMapOf<String, MultiTargetNativeCompilation>()

    fun createNativeCompilation(
        archiveName: String,
        configure: Action<MultiTargetNativeCompilation>,
    ): MultiTargetNativeCompilation {
        val multiTargetNativeCompilation =
            multiTargetNativeCompilations.getOrPut(archiveName) {
                MultiTargetNativeCompilation(project = project, archiveName = archiveName)
            }
        configure.execute(multiTargetNativeCompilation)
        return multiTargetNativeCompilation
    }
}
