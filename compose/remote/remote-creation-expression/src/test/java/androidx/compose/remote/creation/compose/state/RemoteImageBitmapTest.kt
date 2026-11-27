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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.document.RemoteDocumentWriter
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteImageBitmapTest {

    @Test
    fun urlFactory_defaultAndExplicitDimensions() {
        val url = "https://example.com/banner.png"
        val defaultBitmap = RemoteImageBitmap(url)
        val explicitOneByOne = RemoteImageBitmap(url, 1, 1)
        val sizedBitmap = RemoteImageBitmap(url, 320, 180)
        val intrinsicSizeBitmap = RemoteImageBitmap(url, 0, 0)

        assertThat(defaultBitmap.urlOrNull).isEqualTo(url)
        assertThat(defaultBitmap.declaredWidth).isEqualTo(1)
        assertThat(defaultBitmap.declaredHeight).isEqualTo(1)
        assertThat(defaultBitmap.hasIntrinsicDimensions).isFalse()
        assertThat(defaultBitmap.cacheKey).isEqualTo(RemoteConstantCacheKey(url))
        assertThat(explicitOneByOne).isEqualTo(defaultBitmap)

        assertThat(sizedBitmap.urlOrNull).isEqualTo(url)
        assertThat(sizedBitmap.declaredWidth).isEqualTo(320)
        assertThat(sizedBitmap.declaredHeight).isEqualTo(180)
        assertThat(sizedBitmap.hasIntrinsicDimensions).isFalse()
        assertThat(sizedBitmap).isNotEqualTo(defaultBitmap)
        assertThat(sizedBitmap).isEqualTo(RemoteImageBitmap(url, 320, 180))

        assertThat(intrinsicSizeBitmap.urlOrNull).isEqualTo(url)
        assertThat(intrinsicSizeBitmap.declaredWidth).isNull()
        assertThat(intrinsicSizeBitmap.declaredHeight).isNull()
        assertThat(intrinsicSizeBitmap.hasIntrinsicDimensions).isTrue()
        assertThat(intrinsicSizeBitmap).isNotEqualTo(defaultBitmap)
        assertThat(intrinsicSizeBitmap).isNotEqualTo(sizedBitmap)
        assertThat(sizedBitmap.cacheKey.getValueKind()).isEqualTo(RemoteValueKind.IMAGE_BITMAP)
        assertThat(sizedBitmap.cacheKey.isResultWholeNumber).isFalse()
    }

    @Test
    fun urlFactory_writesExplicitDimensionsToDocumentWithoutColliding() {
        data class RecordedUrlBitmap(val url: String, val width: Int, val height: Int)

        val recorded = LinkedHashMap<Int, RecordedUrlBitmap>()
        var nextId = 42
        val writer =
            Proxy.newProxyInstance(
                RemoteDocumentWriter::class.java.classLoader,
                arrayOf(RemoteDocumentWriter::class.java),
            ) { _, method, args ->
                if (method.name.startsWith("addBitmapUrl")) {
                    val id = nextId++
                    recorded[id] =
                        RecordedUrlBitmap(
                            url = args[0] as String,
                            width = args[1] as Int,
                            height = args[2] as Int,
                        )
                    id
                } else {
                    throw UnsupportedOperationException(method.name)
                }
            } as RemoteDocumentWriter
        val creationState = RemoteComposeCreationState().apply { document = writer }
        val url = "https://example.com/photo.png"
        val defaultBitmap = RemoteImageBitmap(url)
        val sizedBitmap = RemoteImageBitmap(url, 640, 480)
        val intrinsicBitmap = RemoteImageBitmap(url, 0, 0)

        val defaultId = creationState.getId(defaultBitmap)
        val sizedId = creationState.getId(sizedBitmap)
        val intrinsicId = creationState.getId(intrinsicBitmap)
        assertThat(sizedId).isNotEqualTo(defaultId)
        assertThat(intrinsicId).isNotEqualTo(defaultId)
        assertThat(intrinsicId).isNotEqualTo(sizedId)

        assertThat(recorded[defaultId]).isEqualTo(RecordedUrlBitmap(url, 1, 1))
        assertThat(recorded[sizedId]).isEqualTo(RecordedUrlBitmap(url, 640, 480))
        assertThat(recorded[intrinsicId]).isEqualTo(RecordedUrlBitmap(url, 0, 0))
    }

    @Test
    fun namedUrlBitmap_exposesUrlAndDeclaredDimensions() {
        val url = "https://example.com/avatar.png"
        val namedDefault = RemoteImageBitmap.createNamedRemoteImageBitmap("avatar_default", url)
        val namedSized =
            RemoteImageBitmap.createNamedRemoteImageBitmap(
                name = "avatar_sized",
                url = url,
                width = 256,
                height = 128,
                domain = RemoteValue.Domain.System,
            )

        val namedIntrinsic =
            RemoteImageBitmap.createNamedRemoteImageBitmap(
                name = "avatar_intrinsic",
                url = url,
                width = 0,
                height = 0,
            )

        assertThat(namedDefault.urlOrNull).isEqualTo(url)
        assertThat(namedDefault.declaredWidth).isEqualTo(1)
        assertThat(namedDefault.declaredHeight).isEqualTo(1)
        assertThat(namedDefault.hasIntrinsicDimensions).isFalse()
        assertThat(namedDefault.cacheKey)
            .isEqualTo(RemoteNamedCacheKey(RemoteValue.Domain.User, "avatar_default"))

        assertThat(namedSized.urlOrNull).isEqualTo(url)
        assertThat(namedSized.declaredWidth).isEqualTo(256)
        assertThat(namedSized.declaredHeight).isEqualTo(128)
        assertThat(namedSized.hasIntrinsicDimensions).isFalse()
        assertThat(namedSized.cacheKey)
            .isEqualTo(RemoteNamedCacheKey(RemoteValue.Domain.System, "avatar_sized"))

        assertThat(namedIntrinsic.urlOrNull).isEqualTo(url)
        assertThat(namedIntrinsic.declaredWidth).isNull()
        assertThat(namedIntrinsic.declaredHeight).isNull()
        assertThat(namedIntrinsic.hasIntrinsicDimensions).isTrue()
    }

    @Test
    fun nonUrlBitmaps_returnNullUrlAndExpectedDimensions() {
        val offscreen = RemoteImageBitmap.createOffscreenRemoteBitmap(200, 100)
        assertThat(offscreen.urlOrNull).isNull()
        assertThat(offscreen.declaredWidth).isEqualTo(200)
        assertThat(offscreen.declaredHeight).isEqualTo(100)
        assertThat(offscreen.hasIntrinsicDimensions).isFalse()

        val byId = RemoteImageBitmap.createForId(42)
        assertThat(byId.urlOrNull).isNull()
        assertThat(byId.declaredWidth).isNull()
        assertThat(byId.declaredHeight).isNull()
        assertThat(byId.hasIntrinsicDimensions).isFalse()
    }

    @Test
    fun sizedUrlBitmap_widthHeightPropertiesStillWork() {
        val sized = RemoteImageBitmap("https://example.com/icon.png", 48, 32)
        assertThat(sized.width.cacheKey.getValueKind()).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(sized.height.cacheKey.getValueKind()).isEqualTo(RemoteValueKind.FLOAT)
        assertThat(sized.width.cacheKey.isResultWholeNumber).isTrue()
        assertThat(sized.height.cacheKey.isResultWholeNumber).isTrue()
    }
}
