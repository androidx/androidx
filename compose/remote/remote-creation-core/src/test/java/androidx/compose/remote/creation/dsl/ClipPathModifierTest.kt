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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemotePathBase
import androidx.compose.remote.core.SystemInfo
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.modifiers.ClipPathModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClipPathModifierTest {

    private val androidxProfile =
        RcProfile(
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                RcPlatformServices.None,
            ) { _, profile, _ ->
                RemoteComposeWriter(profile)
            }
        )

    private val androidxExperimentalProfile =
        RcProfile(androidxProfile.profile, isExperimental = true)

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformServices.None,
        ) { _, profile, _ ->
            RemoteComposeWriter(profile)
        }

    @Test
    fun clipWithPath_emitsClipPathModifierReferencingThePath() {
        // RcPlatformServices.None cannot convert platform paths, so build the path data directly.
        val triangle = RemotePathBase()
        triangle.moveTo(0f, 0f)
        triangle.lineTo(100f, 0f)
        triangle.lineTo(50f, 100f)
        triangle.close()

        val writer = RemoteComposeWriter(experimentalProfile)
        val scope = RcScopeImpl(writer)
        val pathId = writer.addPathData(triangle.path)
        writer.root {
            scope.Box(modifier = Modifier.size(100f).clip(RcShape.path(RcPath(pathId)))) {}
        }

        val document = inflate(writer.encodeToByteArray())
        val clip = findClipPathModifier(document)

        assertThat(clip).isNotNull()
        assertThat(clip!!.pathId).isEqualTo(pathId)
        assertThat(document.displayHierarchy()).contains("CLIP_PATH = [$pathId]")
    }

    @Test
    fun clipWithPathId_emitsClipPathModifier() {
        val bytes =
            createRcBuffer(androidxExperimentalProfile) {
                Box(modifier = Modifier.size(100f).clip(RcShape.path(RcPath(42)))) {}
            }

        val clip = findClipPathModifier(inflate(bytes))

        assertThat(clip).isNotNull()
        assertThat(clip!!.pathId).isEqualTo(42)
    }

    @Test
    fun clipWithPath_throwsWhenProfileDoesNotIncludeExperimentalOperations() {
        val error =
            assertThrows(RuntimeException::class.java) {
                createRcBuffer(androidxProfile) {
                    Box(modifier = Modifier.size(100f).clip(RcShape.path(RcPath(42)))) {}
                }
            }

        assertThat(error).hasMessageThat().contains("not supported")
    }

    private fun inflate(bytes: ByteArray): CoreDocument {
        val buffer = RemoteComposeBuffer()
        buffer.buffer.setSystemInfo(
            SystemInfo(CoreDocument.DOCUMENT_API_LEVEL, CoreDocument.PROFILE)
        )
        buffer.buffer.reset(bytes.size)
        for (b in bytes) {
            buffer.buffer.writeByte(b.toInt())
        }
        buffer.buffer.setIndex(0)
        val document = CoreDocument()
        document.initFromBuffer(buffer)
        return document
    }

    private fun findClipPathModifier(document: CoreDocument): ClipPathModifierOperation? {
        val root = document.rootLayoutComponent ?: return null
        val box = root.list.filterIsInstance<Component>().firstOrNull() ?: return null
        return box.list
            .filterIsInstance<ComponentModifiers>()
            .flatMap { it.modifiersList }
            .filterIsInstance<ClipPathModifierOperation>()
            .firstOrNull()
    }
}
