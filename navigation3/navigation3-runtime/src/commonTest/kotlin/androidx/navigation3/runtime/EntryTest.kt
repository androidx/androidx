/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.test.Test

class EntryTest {

    @Test
    fun getKey() {
        val entry = NavEntry(key = "myKey", contentKey = "myKey", content = {})
        assertThat(entry.contentKey).isEqualTo("myKey")
    }

    @Test
    fun getFeatureMap() {
        val entry =
            NavEntry(
                key = "myKey",
                metadata = mapOf("feature1" to 1, "feature2" to MyObject),
                content = {},
            )
        assertThat(entry.metadata["feature1"]).isEqualTo(1)
        assertThat(entry.metadata["feature2"]).isEqualTo(MyObject)
    }

    object MyObject
}
