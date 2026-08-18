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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Typeface
import androidx.collection.emptyIntObjectMap
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.mutableFloatStateOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerCanvasTextTypefaceTest {

    private class CustomTestTypefaceResolver(val expectedTypeface: Typeface) : TypefaceResolver {
        var resolveCalled = false

        override fun resolve(
            fontType: Int,
            weight: Int,
            italic: Boolean,
            fallbackTypeface: Typeface?,
            fallbackWeight: Int,
            fallbackItalic: Boolean,
        ): FontInstance {
            resolveCalled = true
            return object : FontInstance {
                override fun getTypeface(): Typeface = expectedTypeface

                override fun applyVariationSettings(
                    tags: Array<String>,
                    values: FloatArray,
                ): Typeface = expectedTypeface

                override fun setOnLoadedListener(listener: Runnable) {}
            }
        }

        override fun resolve(
            fontName: String,
            weight: Int,
            italic: Boolean,
            fallbackTypeface: Typeface?,
            fallbackWeight: Int,
            fallbackItalic: Boolean,
        ): FontInstance {
            resolveCalled = true
            return object : FontInstance {
                override fun getTypeface(): Typeface = expectedTypeface

                override fun applyVariationSettings(
                    tags: Array<String>,
                    values: FloatArray,
                ): Typeface = expectedTypeface

                override fun setOnLoadedListener(listener: Runnable) {}
            }
        }
    }

    @Test
    fun toNativeTextPaintUsesContextTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val context =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }

        val nativePaint = paint.toNativeTextPaint(context)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }

    @Test
    fun toNativeTextPaintWithGraphContextUsesConfiguredTypefaceResolver() {
        val testTypeface = Typeface.MONOSPACE
        val testResolver = CustomTestTypefaceResolver(testTypeface)
        val playerContext =
            AndroidRemoteContext(RemoteClock.SYSTEM).apply { setTypefaceResolver(testResolver) }
        val snapshotState = SnapshotRemoteComposeState()
        val timeState = androidx.compose.runtime.mutableFloatStateOf(0f)
        val graphContext =
            GraphContext(
                    snapshotState,
                    emptyIntObjectMap(),
                    mutableFloatStateOf(0f),
                    RemoteClock.SYSTEM,
                )
                .apply { setTypefaceResolver(playerContext.typefaceResolver) }

        val paint = ComposeLocalPaint().apply { textSize = 24f }
        val nativePaint = paint.toNativeTextPaint(graphContext)

        assertThat(testResolver.resolveCalled).isTrue()
        assertThat(nativePaint.typeface).isEqualTo(testTypeface)
    }
}
