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

package androidx.room.gradle

import androidx.testutils.gradle.ProjectSetupRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class CollectionTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    @Test
    fun collection2mavenMetadata() {
        val c1metadata = getPublishedFile("androidx/collection/collection/maven-metadata.xml")
        val c2metadata = getPublishedFile("androidx/collection2/collection2/maven-metadata.xml")

        val v1 = getLatestVersion(c1metadata)
        val v2 = getLatestVersion(c2metadata)

        c1metadata.readLines().zip(c2metadata.readLines()).forEach { (c1, c2) ->
            if (!c1.contains("lastUpdated")) {
                Assert.assertEquals(c1.replace("collection", "collection2").replace(v1, v2), c2)
            }
        }
    }

    // Yes, I know https://stackoverflow.com/a/1732454/258688, but it's just a test...
    private fun getLatestVersion(metadataFile: File) = metadataFile.readLines()
        .mapNotNull { Regex(".*<latest>(.*?)</latest>.*").find(it)?.groups?.get(1)?.value }.first()

    private fun getPublishedFile(name: String) =
        File(projectSetup.props.localSupportRepo).resolve(name).check { it.exists() }

    private fun <T> T.check(eval: (T) -> Boolean): T {
        if (!eval(this)) {
            Assert.fail("Failed assertion: $this")
        }
        return this
    }
}
