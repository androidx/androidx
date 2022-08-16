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

data class AndroidXSelfTestProject(
    val groupId: String,
    val artifactId: String?,
    val version: String?,
    private val buildGradleTextTemplate: String
) {
    val relativePath = artifactId?.let { "$groupId/$artifactId" } ?: groupId
    val gradlePath = ":$groupId:$artifactId"
    val sourceCoordinate get() = "$groupId:$artifactId:${version!!}"
    val buildGradleText = buildGradleTextTemplate.replace("%GROUP_ID%", groupId)

    companion object {
        fun cubaneBuildGradleText(
            plugins: List<String> = listOf("java-library", "kotlin", "AndroidXPlugin"),
            version: String? = "1.2.3",
            moreConfig: String = ""
        ): String {
            val mavenVersionLine = if (version != null) {
                "  mavenVersion = new Version(\"$version\")"
            } else {
                ""
            }
            return """|import androidx.build.LibraryGroup
                      |import androidx.build.Publish
                      |import androidx.build.Version
                      |
                      |plugins {
                      |${plugins.joinToString("") { "  id(\"$it\")\n" }}
                      |}
                      |
                      |dependencies {
                      |  api(libs.kotlinStdlib)
                      |}
                      |
                      |$moreConfig
                      |
                      |androidx {
                      |  publish = Publish.SNAPSHOT_AND_RELEASE
                      |$mavenVersionLine
                      |  mavenGroup = new LibraryGroup("%GROUP_ID%", null)
                      |}
                      |""".trimMargin()
        }

        /**
         * A simple non-kmp project with no source that will be part of our test androidx suite.
         * ("Cubane" is literally a random word from wikipedia.  It "is a synthetic hydrocarbon
         * molecule that consists of eight carbon atoms")
         */
        val cubaneProject =
            AndroidXSelfTestProject(
                groupId = "cubane",
                artifactId = "cubane",
                version = "1.2.3",
                buildGradleTextTemplate = cubaneBuildGradleText()
            )

        fun buildGradleForKmp(
            withJava: Boolean = true,
            addJvmDependency: Boolean = false
        ): String {
            val jvmDependency = if (addJvmDependency) {
                "jvmImplementation(\"androidx.jvmgroup:jvmdep:6.2.9\")"
            } else {
                ""
            }
            return """|import androidx.build.LibraryGroup
                      |import androidx.build.LibraryType
                      |import androidx.build.Publish
                      |import androidx.build.Version
                      |
                      |plugins {
                      |  id("AndroidXPlugin")
                      |}
                      |
                      |androidXMultiplatform {
                      |  jvm {
                      |    ${if (withJava) "withJava()" else ""}
                      |  }
                      |}
                      |
                      |dependencies {
                      |  $jvmDependency
                      |}
                      |
                      |androidx {
                      |  type = LibraryType.KMP_LIBRARY
                      |  mavenVersion = new Version("1.2.3")
                      |  mavenGroup = new LibraryGroup("%GROUP_ID%", null)
                      |}
                      |""".trimMargin()
        }

        /**
         * A simple KMP project with no actual source that will be part of our test androidx suite.
         */
        val cubaneKmpProject = AndroidXSelfTestProject(
            groupId = "cubane",
            artifactId = "cubanekmp",
            version = "1.2.3",
            buildGradleTextTemplate = buildGradleForKmp(withJava = true)
        )

        /**
         * A simple KMP project with no actual source and no java sourceSet.
         * (This means that JavaPlugin code paths will not be triggered)
         */
        val cubaneKmpNoJavaProject = AndroidXSelfTestProject(
            groupId = "cubane",
            artifactId = "cubaneNoJava",
            version = "1.2.3",
            buildGradleTextTemplate = buildGradleForKmp(withJava = false)
        )
    }
}