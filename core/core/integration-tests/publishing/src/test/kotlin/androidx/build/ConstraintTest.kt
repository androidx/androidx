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

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConstraintTest {
    @get:Rule val projectSetup = ProjectSetupRule()

    /** Test for matching constraint versions in Gradle metadata. */
    @Test
    fun mavenMetadataRequiresVersion() {
        val baseMavenMetadata = getPublishedFile("androidx/core/core/maven-metadata.xml")
        val baseVersion = getLatestVersion(baseMavenMetadata)

        val baseGradleMetadata =
            getPublishedFile("androidx/core/core/$baseVersion/core-$baseVersion.module")
        val baseRequiresKtxVersion =
            getConstraintVersion(baseGradleMetadata.readText(), "androidx.core", "core-ktx")!!

        val ktxMavenMetadata = getPublishedFile("androidx/core/core-ktx/maven-metadata.xml")
        val ktxVersion = getLatestVersion(ktxMavenMetadata)

        val ktxGradleMetadata =
            getPublishedFile("androidx/core/core-ktx/$ktxVersion/core-ktx-$ktxVersion.module")
        val ktxRequiresBaseVersion =
            getConstraintVersion(ktxGradleMetadata.readText(), "androidx.core", "core")!!

        // The core and core-ktx libraries should share the same version.
        assertEquals(baseVersion, ktxVersion)

        // Their Gradle metadata files should reference each other as `required`-type constraints.
        assertEquals(baseVersion, ktxRequiresBaseVersion)
        assertEquals(ktxVersion, baseRequiresKtxVersion)
    }

    /** Unit test for the constraint version extraction function. */
    @Test
    fun getConstraintVersionTest() {
        val metadata =
            """
              "version": {
                "requires": "1.0.0"
              }
            }
          ],
          variants: [
            {
              "name": "releaseVariantReleaseApiPublication",
              "dependencyConstraints": [
                {
                   "group": "org.jetbrains.kotlin",
                   "module": "kotlin-stdlib",
                   "version": {
                     "requires": "1.8.22"
                   }
                }
              ],
             },
             {
              "name": "releaseVariantReleaseRuntimePublication",
              "dependencyConstraints": [
                {
                  "group": "androidx.preference",
                  "module": "preference-ktx",
                  "version": {
                    "requires": "1.3.0-alpha01"
                  }
                }
              ],
             }
          ]
          "files": [
            {
              "name": "preference-1.3.0-alpha01.aar",
              "url": "preference-1.3.0-alpha01.aar",
        """
                .trimIndent()

        val requiresVersion =
            getConstraintVersion(metadata, "androidx.preference", "preference-ktx")
        assertEquals("1.3.0-alpha01", requiresVersion)
    }

    private fun getConstraintVersion(metadata: String, groupId: String, artifact: String): String? =
        getDependencyConstraints(metadata).let {
            Regex(
                    "\"group\": \"$groupId\",\\s+\"module\": " +
                        "\"$artifact\",\\s+\"version\": \\{\\s+\"requires\": \"(.+?)\""
                )
                .find(it)
                ?.groups
                ?.get(1)
                ?.value
        }

    private fun getDependencyConstraints(moduleJson: String) =
        Regex("(?s)\"dependencyConstraints\": \\[(.+?)]").findAll(moduleJson).joinToString("\n") {
            it.groups.get(1)?.value.orEmpty()
        }

    // Yes, I know https://stackoverflow.com/a/1732454/258688, but it's just a test...
    private fun getLatestVersion(metadataFile: File) =
        metadataFile
            .readLines()
            .mapNotNull { Regex(".*<latest>(.*?)</latest>.*").find(it)?.groups?.get(1)?.value }
            .first()

    private fun getPublishedFile(name: String) =
        File(projectSetup.props.tipOfTreeMavenRepoPath).resolve(name).check { it.exists() }

    private fun <T> T.check(eval: (T) -> Boolean): T {
        if (!eval(this)) {
            Assert.fail("Failed assertion: $this")
        }
        return this
    }
}
