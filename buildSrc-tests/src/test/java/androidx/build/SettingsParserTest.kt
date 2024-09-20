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

package androidx.build

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsParserTest {
    @Test
    fun parseProjects() {
        val projects =
            SettingsParser.findProjects(
                """
            includeProject(":no:filepath", [BuildType.MAIN])
            includeProject(":with:filepath", "some/dir", [BuildType.COMPOSE])
                includeProject(":has:spaces:before:include", "dir2", [BuildType.MAIN])
            includeProject(":has:comments:after", "dir3", [BuildType.MAIN]) // some comment
            // includeProject("commented", "should not be there", [BuildType.MAIN])
            includeProject("no:build:type")
            includeProject("no:build:type:with:path", "dir4")
            """
                    .trimIndent()
            )
        assertThat(projects)
            .containsExactly(
                IncludedProject(gradlePath = ":with:filepath", filePath = "some/dir"),
                IncludedProject(gradlePath = ":no:filepath", filePath = "no/filepath"),
                IncludedProject(gradlePath = ":has:spaces:before:include", filePath = "dir2"),
                IncludedProject(gradlePath = ":has:comments:after", filePath = "dir3"),
                IncludedProject(gradlePath = "no:build:type", filePath = "no/build/type"),
                IncludedProject(gradlePath = "no:build:type:with:path", filePath = "dir4")
            )
    }
}
