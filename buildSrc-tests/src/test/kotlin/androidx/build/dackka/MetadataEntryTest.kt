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

package androidx.build.dackka

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MetadataEntryTest {

    @Test
    fun toMap() {
        val entry = MetadataEntry(
            groupId = "AndroidX Group ID",
            artifactId = "AndroidX Artifact ID",
            releaseNotesUrl = "https://d.android.com/jetpack",
            sourceDir = "androidx/"
        )
        val map = entry.toMap()

        assertThat(map["groupId"]).isEqualTo("AndroidX Group ID")
        assertThat(map["artifactId"]).isEqualTo("AndroidX Artifact ID")
        assertThat(map["releaseNotesUrl"]).isEqualTo("https://d.android.com/jetpack")
        assertThat(map["sourceDir"]).isEqualTo("androidx/")
    }
}