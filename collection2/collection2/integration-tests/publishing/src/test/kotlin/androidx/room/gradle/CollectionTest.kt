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
        c1metadata.readLines().zip(c2metadata.readLines()).forEach { (c1, c2) ->
            if (!c1.contains("lastUpdated") && !c1.contains("beta")) {
                Assert.assertEquals(c1.replace("collection", "collection2"), c2)
            }
        }
    }

    private fun getPublishedFile(name: String) =
        File(projectSetup.props.localSupportRepo).resolve(name).checkIt { exists() }

    private fun <T> T.checkIt(eval: T.() -> Boolean): T {
        if (!eval()) {
            Assert.fail("Failed assertion: $this")
        }
        return this
    }
}
