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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.collection.buildIntSet
import androidx.collection.intSetOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalRemoteCreationComposeApi::class)
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class ProfileTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val displayInfo = CreationDisplayInfo(100, 100, 160)
    private val minimalProfileSupportedOperations = buildIntSet {
        RcPlatformProfiles.ANDROIDX.supportedOperations.forEach { op ->
            if (
                op != Operations.COLOR_THEME &&
                    op != Operations.MODIFIER_TOUCH_DOWN &&
                    op != Operations.MODIFIER_TOUCH_UP &&
                    op != Operations.MODIFIER_TOUCH_CANCEL &&
                    op != Operations.CLIP_PATH
            ) {
                add(op)
            }
        }
    }
    private val minimalProfile =
        createProfile(supportedOperations = minimalProfileSupportedOperations)

    @Test
    fun createProfile_defaultValues() {
        val profile = createProfile()

        assertThat(profile.apiLevel).isEqualTo(CoreDocument.DOCUMENT_API_LEVEL)
        assertThat(profile.operationsProfiles).isEqualTo(RcProfiles.PROFILE_ANDROIDX)
        assertThat(profile.platform).isInstanceOf(AndroidxRcPlatformServices::class.java)

        val writer = profile.create(displayInfo, null)
        assertThat(writer).isInstanceOf(RemoteComposeWriterAndroid::class.java)
    }

    @Test
    fun createProfile_customParameters() {
        val customOps = intSetOf(Operations.HEADER, Operations.DRAW_RECT, Operations.DRAW_LINE)
        val profile =
            createProfile(
                docApiLevel = 7,
                profileFlags = RcProfiles.PROFILE_WEAR_WIDGETS,
                supportedOperations = customOps,
            )

        assertThat(profile.apiLevel).isEqualTo(7)
        assertThat(profile.operationsProfiles).isEqualTo(RcProfiles.PROFILE_WEAR_WIDGETS)
        assertThat(profile.supportedOperations)
            .containsExactly(Operations.HEADER, Operations.DRAW_RECT, Operations.DRAW_LINE)

        val writer = profile.create(displayInfo, null)
        assertThat(writer).isInstanceOf(RemoteComposeWriterAndroid::class.java)
        assertThat(profile.supportedOperations).doesNotContain(Operations.DRAW_CIRCLE)
    }

    @Test
    fun currentProfileSupportsThemedColorsAndTouchModifiers_androidxProfile_returnsTrue() =
        runTest {
            var themedColors: Boolean? = null
            var touchModifiers: Boolean? = null

            captureSingleRemoteDocument(context, profile = RcPlatformProfiles.ANDROIDX) {
                themedColors = currentProfileSupportsThemedColors()
                touchModifiers = currentProfileSupportsTouchModifiers()
                RemoteBox()
            }

            assertThat(themedColors).isTrue()
            assertThat(touchModifiers).isTrue()
        }

    @Test
    fun currentProfileSupportsThemedColorsAndTouchModifiers_minimalProfile_returnsFalse() =
        runTest {
            var themedColors: Boolean? = null
            var touchModifiers: Boolean? = null

            captureSingleRemoteDocument(context, profile = minimalProfile) {
                themedColors = currentProfileSupportsThemedColors()
                touchModifiers = currentProfileSupportsTouchModifiers()
                RemoteBox()
            }

            assertThat(themedColors).isFalse()
            assertThat(touchModifiers).isFalse()
        }

    @Test
    fun currentProfileSupportsTouchModifiers_requiresAllTouchOperations() = runTest {
        val partialTouchProfile =
            createProfile(
                supportedOperations =
                    buildIntSet {
                        addAll(minimalProfileSupportedOperations)
                        add(Operations.MODIFIER_TOUCH_DOWN)
                        add(Operations.MODIFIER_TOUCH_UP)
                    }
            )
        var touchModifiers: Boolean? = null

        captureSingleRemoteDocument(context, profile = partialTouchProfile) {
            touchModifiers = currentProfileSupportsTouchModifiers()
            RemoteBox()
        }

        assertThat(touchModifiers).isFalse()
    }

    @Test
    fun currentProfileSupportsPathClip_androidxProfile_returnsTrue() = runTest {
        var pathClip: Boolean? = null

        captureSingleRemoteDocument(context, profile = RcPlatformProfiles.ANDROIDX) {
            pathClip = currentProfileSupportsPathClip()
            RemoteBox()
        }

        assertThat(pathClip).isTrue()
    }

    @Test
    fun currentProfileSupportsPathClip_minimalProfile_returnsFalse() = runTest {
        var pathClip: Boolean? = null

        captureSingleRemoteDocument(context, profile = minimalProfile) {
            pathClip = currentProfileSupportsPathClip()
            RemoteBox()
        }

        assertThat(pathClip).isFalse()
    }

    @Test
    fun currentProfileSupports_checksAllGivenOperations() = runTest {
        var supportsDrawRect: Boolean? = null
        var supportsColorTheme: Boolean? = null
        var supportsBoth: Boolean? = null

        captureSingleRemoteDocument(context, profile = minimalProfile) {
            supportsDrawRect = currentProfileSupports(Operations.DRAW_RECT)
            supportsColorTheme = currentProfileSupports(Operations.COLOR_THEME)
            supportsBoth = currentProfileSupports(Operations.DRAW_RECT, Operations.COLOR_THEME)
            RemoteBox()
        }

        assertThat(supportsDrawRect).isTrue()
        assertThat(supportsColorTheme).isFalse()
        assertThat(supportsBoth).isFalse()
    }
}
