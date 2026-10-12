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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream

/** Builds a raw draw-list [CoreDocument] (AndroidX + experimental profile) from [content]. */
internal fun buildDocument(content: RemoteComposeContextAndroid.() -> Unit): CoreDocument {
    val docContext =
        RemoteComposeContextAndroid(
            100,
            100,
            "test",
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) {
            content()
        }
    return CoreDocument().apply {
        ByteArrayInputStream(docContext.writer.encodeToByteArray()).use {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
        }
    }
}

/** Renders [document] in a 100x100px [RcPlayer] (at 1.0 density) and returns the [Bitmap]. */
internal fun ComposeContentTestRule.renderDocument(document: CoreDocument): Bitmap {
    setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
        }
    }
    return onRoot().captureToImage().asAndroidBitmap()
}

/** Builds a raw draw-list document from [content] and renders it, see [renderDocument]. */
internal fun ComposeContentTestRule.renderDocument(
    content: RemoteComposeContextAndroid.() -> Unit
): Bitmap = renderDocument(buildDocument(content))

/** Asserts the pixel at ([x], [y]) is red. */
internal fun assertRed(bitmap: Bitmap, x: Int, y: Int) {
    val pixel = bitmap.getPixel(x, y)
    assertWithMessage("Red channel at ($x, $y)").that(AndroidColor.red(pixel)).isGreaterThan(200)
    assertWithMessage("Green channel at ($x, $y)").that(AndroidColor.green(pixel)).isLessThan(60)
    assertWithMessage("Blue channel at ($x, $y)").that(AndroidColor.blue(pixel)).isLessThan(60)
}

/** Asserts the pixel at ([x], [y]) is not red. */
internal fun assertNotRed(bitmap: Bitmap, x: Int, y: Int) {
    val pixel = bitmap.getPixel(x, y)
    val isRed =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.green(pixel) < 60 &&
            AndroidColor.blue(pixel) < 60
    assertWithMessage(
            "Expected pixel at ($x, $y) not to be red, but was #${Integer.toHexString(pixel)} " +
                "(r=${AndroidColor.red(pixel)}, g=${AndroidColor.green(pixel)}, b=${AndroidColor.blue(pixel)})"
        )
        .that(isRed)
        .isFalse()
}
