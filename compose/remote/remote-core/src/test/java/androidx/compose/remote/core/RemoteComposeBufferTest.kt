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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.ComponentStart
import androidx.compose.remote.core.operations.layout.ContainerEnd
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.layout.managers.TextStyle
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class RemoteComposeBufferTest {
    private lateinit var rcPlatform: RcPlatformServices

    private lateinit var androidXProfile: Profile
    private var originalEnableImageUrls: Boolean = false
    private var originalEnableImageFiles: Boolean = false

    @Before
    fun setUp() {
        rcPlatform = mock<RcPlatformServices>()

        androidXProfile =
            Profile(
                /* apiLevel= */ 7,
                /* operationProfiles= */ RcProfiles.PROFILE_ANDROIDX or
                    RcProfiles.PROFILE_EXPERIMENTAL,
                /* platform= */ rcPlatform,
            )
            /* factory= */ { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(creationDisplayInfo, null, profile)
            }
        originalEnableImageUrls = Limits.ENABLE_IMAGE_URLS
        originalEnableImageFiles = Limits.ENABLE_IMAGE_FILES
        Limits.ENABLE_IMAGE_URLS = true
        Limits.ENABLE_IMAGE_FILES = true
    }

    @After
    fun tearDown() {
        Limits.ENABLE_IMAGE_URLS = originalEnableImageUrls
        Limits.ENABLE_IMAGE_FILES = originalEnableImageFiles
    }

    @Test
    fun testTextStyleInBuffer() {
        val rcProfile =
            Profile(7, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL, rcPlatform)
            /* factory= */ { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(creationDisplayInfo, null, profile)
            }

        val writer =
            RemoteComposeWriter(
                rcProfile,
                RemoteComposeBuffer(rcProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, rcProfile.operationsProfiles),
            )

        writer.addTextStyle(
            0xFFFF0000.toInt(),
            -1,
            30f,
            -1f,
            -1f,
            0,
            800f,
            null,
            CoreText.TEXT_ALIGN_CENTER,
            1,
            Int.MAX_VALUE,
            0f,
            0f,
            1f,
            0,
            0,
            0,
            false,
            false,
            null,
            null,
            false,
            -1,
        )

        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        val hasStyle = coreDoc.mOperations.any { it is TextStyle }
        assertThat(hasStyle).isTrue()
    }

    @Test
    fun initCoreDocumentFromBuffer_withExperimentalFeatures() {
        val rcProfile =
            Profile(7, RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL, rcPlatform)
            /* factory= */ { creationDisplayInfo, profile, _ ->
                RemoteComposeWriter(creationDisplayInfo, null, profile)
            }

        val writer =
            RemoteComposeWriter(
                rcProfile,
                RemoteComposeBuffer(rcProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, rcProfile.operationsProfiles),
            )

        val hello = writer.textCreateId("Hello")

        writer.root {
            // Use simplified startTextComponent
            writer.startTextComponent(
                RecordingModifier(),
                hello,
                -1, // textStyleId
                0, // flags
            )
            writer.endTextComponent()
        }

        // no crash; can read correct api level from buffer and init the core doc.
        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        assertThat(coreDoc.mBuffer.mApiLevel).isEqualTo(7)
        assertThat(coreDoc.mHeader?.profiles ?: 0).isEqualTo(rcProfile.operationsProfiles)
    }

    @Test
    fun initCoreDocumentFromBuffer_withAndroidNativeProfileAndCustomOperations() {
        val customMap =
            Operations.UniqueIntMap<CompanionOperation>().apply {
                put(Operations.HEADER, Header::read)
                put(Operations.DATA_TEXT, TextData::read)
                put(Operations.LAYOUT_ROOT, RootLayoutComponent::read)
                put(Operations.LAYOUT_CONTENT, LayoutComponentContent::read)
                put(Operations.COMPONENT_START, ComponentStart::read)
                put(Operations.LAYOUT_TEXT, TextLayout::read)
                put(Operations.COMPONENT_VALUE, ComponentValue::read)
                put(Operations.TEXT_STYLE, TextStyle::read)
                put(Operations.CORE_TEXT, CoreText::read)
                put(Operations.CONTAINER_END, ContainerEnd::read)
            }
        val customOps = customMap.keySet()
        val nativeProfile =
            Profile(
                7,
                RcProfiles.PROFILE_ANDROID_NATIVE,
                rcPlatform,
                { customOps },
                { creationDisplayInfo, profile, _ ->
                    RemoteComposeWriter(creationDisplayInfo, null, profile)
                },
            )

        val writer =
            RemoteComposeWriter(
                nativeProfile,
                RemoteComposeBuffer(nativeProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, nativeProfile.operationsProfiles),
            )

        val hello = writer.textCreateId("Hello Native")
        writer.root {
            writer.startTextComponent(RecordingModifier(), hello, -1, 0)
            writer.endTextComponent()
        }

        writer.buffer.setVersion(
            nativeProfile.apiLevel,
            nativeProfile.operationsProfiles,
            customMap,
        )

        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }
        assertThat(coreDoc.mBuffer.mApiLevel).isEqualTo(7)
        assertThat(coreDoc.mHeader?.profiles ?: 0).isEqualTo(RcProfiles.PROFILE_ANDROID_NATIVE)
    }

    @Test
    fun initCoreDocumentFromBuffer_dynamicCustomProfileHandling() {
        val customMap =
            Operations.UniqueIntMap<CompanionOperation>().apply {
                put(Operations.HEADER, Header::read)
                put(Operations.DATA_TEXT, TextData::read)
                put(Operations.LAYOUT_ROOT, RootLayoutComponent::read)
                put(Operations.LAYOUT_CONTENT, LayoutComponentContent::read)
                put(Operations.COMPONENT_START, ComponentStart::read)
                put(Operations.LAYOUT_TEXT, TextLayout::read)
                put(Operations.COMPONENT_VALUE, ComponentValue::read)
                put(Operations.TEXT_STYLE, TextStyle::read)
                put(Operations.CORE_TEXT, CoreText::read)
                put(Operations.CONTAINER_END, ContainerEnd::read)
            }
        val customOps = customMap.keySet()
        val nativeProfile =
            Profile(
                7,
                RcProfiles.PROFILE_ANDROID_NATIVE,
                rcPlatform,
                { customOps },
                { creationDisplayInfo, profile, _ ->
                    RemoteComposeWriter(creationDisplayInfo, null, profile)
                },
            )

        // 1. Writer creates a document with custom profile
        val writer =
            RemoteComposeWriter(
                nativeProfile,
                RemoteComposeBuffer(nativeProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, nativeProfile.operationsProfiles),
            )

        val hello = writer.textCreateId("Hello Dynamic")
        writer.root {
            writer.startTextComponent(RecordingModifier(), hello, -1, 0)
            writer.endTextComponent()
        }

        // 2. Simulate reading from a stream/file where we don't know the profile type beforehand
        val bytes = writer.buffer.getBuffer().cloneBytes()
        val stream = java.io.ByteArrayInputStream(bytes)

        // 3. Read JUST the header first to peek at API level and profiles
        val headerBuffer = RemoteComposeBuffer.fromInputStream(stream)
        val wireBuffer = headerBuffer.getBuffer()
        wireBuffer.setIndex(0) // Start from beginning

        val apiLevel = Header.peekApiLevel(wireBuffer)
        val header = Header.readDirect(wireBuffer)
        val profiles = header.profiles

        // 4. Verify it's our custom profile and set up custom operations if so
        if (profiles == RcProfiles.PROFILE_ANDROID_NATIVE) {
            headerBuffer.setVersion(apiLevel, profiles, customMap)
        }

        // 5. Continue to read/inflate the document
        val coreDoc = CoreDocument().apply { initFromBuffer(headerBuffer) }

        assertThat(coreDoc.mBuffer.mApiLevel).isEqualTo(7)
        assertThat(coreDoc.mHeader?.profiles ?: 0).isEqualTo(RcProfiles.PROFILE_ANDROID_NATIVE)
    }

    @Test
    fun imageComponent() {
        val writer =
            RemoteComposeWriter(
                androidXProfile,
                RemoteComposeBuffer(androidXProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, androidXProfile.operationsProfiles),
            )

        writer.root {
            val imageId = writer.addBitmapUrl("https://example.com/a.png")

            writer.image(
                /* modifier = */ RecordingModifier(),
                /* imageId = */ imageId,
                /* scaleType = */ ImageScaling.SCALE_FIT,
                /* alpha = */ 1f,
            )
        }

        // no crash; can read correct api level from buffer and init the core doc.
        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }

        val components = coreDoc.mRootLayoutComponent!!.mList
        assertThat(components).hasSize(2)

        val bitmapId = (components[0] as BitmapData).mImageId
        val imageLayout = components[1] as ImageLayout
        assertThat(imageLayout.componentId).isEqualTo(-3)
        assertThat(imageLayout.bitmapId).isEqualTo(bitmapId)
    }

    @Test
    fun bitmapMemory_isCorrect() {
        val writer =
            RemoteComposeWriter(
                androidXProfile,
                RemoteComposeBuffer(androidXProfile.apiLevel),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 188),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 200),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, androidXProfile.operationsProfiles),
            )

        writer.root {
            // Add a bitmap with explicit dimensions
            writer.addBitmapUrl("https://example.com/a.png", 100, 200)
        }

        val coreDoc = CoreDocument().apply { initFromBuffer(writer.buffer) }

        // Expected memory: 100 * 200 * 4 = 80000 bytes
        assertThat(coreDoc.bitmapMemory()).isEqualTo(80000L)
    }

    @Test
    fun setVersion_baseline_writeValidation() {
        val buffer = RemoteComposeBuffer(7)
        buffer.setVersion(7, RcProfiles.PROFILE_BASELINE, null as Set<Int>?)

        // DRAW_RECT (42) should be supported in baseline
        assertThat(buffer.getBuffer().mValidOperations[Operations.DRAW_RECT]).isTrue()

        // MACRO_DEFINE (246) should NOT be supported in baseline
        assertThat(buffer.getBuffer().mValidOperations[Operations.MACRO_DEFINE]).isFalse()
    }

    @Test
    fun setVersion_androidxExperimental_writeValidation() {
        val buffer = RemoteComposeBuffer(7)
        buffer.setVersion(
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            null as Set<Int>?,
        )

        // MACRO_DEFINE (246) should be supported in AndroidX + Experimental
        assertThat(buffer.getBuffer().mValidOperations[Operations.MACRO_DEFINE]).isTrue()
    }

    @Test
    fun setVersion_customMap_writeAndRead() {
        val buffer = RemoteComposeBuffer(7)
        val customMap = Operations.UniqueIntMap<CompanionOperation>()
        // Add a mock custom operation
        val customOpCode = 255
        customMap.put(customOpCode, mock<CompanionOperation>())

        buffer.setVersion(7, RcProfiles.PROFILE_ANDROID_NATIVE, customMap)

        // Only custom operation should be valid for writing
        assertThat(buffer.getBuffer().mValidOperations[customOpCode]).isTrue()
        assertThat(buffer.getBuffer().mValidOperations[Operations.DRAW_RECT]).isFalse()

        // Map should be set for reading
        assertThat(buffer.mMap).isSameInstanceAs(customMap)
        assertThat(buffer.mIsCustomMap).isTrue()
    }

    @Test
    fun setVersion_explicitSupportedOperations() {
        val buffer = RemoteComposeBuffer(7)
        val myOps = setOf(Operations.DRAW_RECT, Operations.DRAW_LINE)

        buffer.setVersion(7, RcProfiles.PROFILE_BASELINE, myOps)

        assertThat(buffer.getBuffer().mValidOperations[Operations.DRAW_RECT]).isTrue()
        assertThat(buffer.getBuffer().mValidOperations[Operations.DRAW_LINE]).isTrue()

        // DRAW_OVAL should be false even if baseline supports it, because we provided explicit list
        assertThat(buffer.getBuffer().mValidOperations[Operations.DRAW_OVAL]).isFalse()
    }
}
