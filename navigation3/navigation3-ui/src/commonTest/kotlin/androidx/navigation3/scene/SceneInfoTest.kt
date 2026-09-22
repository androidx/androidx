/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.scene

import androidx.compose.runtime.Composable
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import kotlin.test.Test

internal class SceneInfoTest {

    @Test
    fun sceneInfo_defaultsToNullTitleAndUrl() {
        val scene = FakeScene<String>(key = "home")
        val info = SceneInfo(scene = scene)

        assertThat(info.scene).isEqualTo(scene)
        assertThat(info.title).isNull()
        assertThat(info.url).isNull()
    }

    @Test
    fun sceneInfo_equalsAndHashCode_includesTitleAndUrl() {
        val scene = FakeScene<String>(key = "home")
        val info1 = SceneInfo(scene = scene, title = "Home", url = "https://example.com/home")
        val info2 = SceneInfo(scene = scene, title = "Home", url = "https://example.com/home")
        val diffTitle = SceneInfo(scene = scene, title = "Other", url = "https://example.com/home")
        val diffUrl = SceneInfo(scene = scene, title = "Home", url = "https://example.com/other")

        assertThat(info1).isEqualTo(info2)
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode())
        assertThat(info1).isNotEqualTo(diffTitle)
        assertThat(info1).isNotEqualTo(diffUrl)
        assertThat(info1.toString())
            .isEqualTo("SceneInfo(scene=$scene, title=Home, url=https://example.com/home)")
    }

    @Test
    fun titleKeyAndUrlKey_storeAndRetrieveComposableLambdas() {
        val entryMetadata = metadata {
            put(TitleMetadataKey) { "Home Title" }
            put(UrlMetadataKey) { "https://example.com/home" }
        }

        assertThat(entryMetadata[TitleMetadataKey]).isNotNull()
        assertThat(entryMetadata[UrlMetadataKey]).isNotNull()
    }

    @Test
    fun scene_metadata_retrievesTitleAndUrlFromLastEntryInScene() {
        val entryA =
            NavEntry(
                key = "A",
                metadata =
                    metadata {
                        put(TitleMetadataKey) { "Title A" }
                        put(UrlMetadataKey) { "https://example.com/a" }
                    },
            ) {}
        val entryB = NavEntry(key = "B") {}

        // When entryB is the last entry in the scene and has no metadata, metadata does not have
        // keys
        val sceneWithEntryBAsLast = FakeScene(key = "multi1", entries = listOf(entryA, entryB))
        assertThat(sceneWithEntryBAsLast.metadata[TitleMetadataKey]).isNull()
        assertThat(sceneWithEntryBAsLast.metadata[UrlMetadataKey]).isNull()

        // When entryA is the last entry in the scene and has metadata, metadata has entryA's keys
        val sceneWithEntryAAsLast = FakeScene(key = "multi2", entries = listOf(entryB, entryA))
        assertThat(sceneWithEntryAAsLast.metadata[TitleMetadataKey]).isNotNull()
        assertThat(sceneWithEntryAAsLast.metadata[UrlMetadataKey]).isNotNull()
    }

    private data class FakeScene<T : Any>(
        override val key: Any,
        override val entries: List<NavEntry<T>> = emptyList(),
        override val previousEntries: List<NavEntry<T>> = emptyList(),
        override val content: @Composable () -> Unit = {},
    ) : Scene<T>
}
