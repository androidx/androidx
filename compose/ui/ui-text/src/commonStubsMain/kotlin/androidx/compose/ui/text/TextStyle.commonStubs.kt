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

package androidx.compose.ui.text

internal actual fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle = implementedInJetBrainsFork()

actual class PlatformTextStyle {
    actual val spanStyle: PlatformSpanStyle?
        get() = implementedInJetBrainsFork()

    actual val paragraphStyle: PlatformParagraphStyle?
        get() = implementedInJetBrainsFork()
}

actual class PlatformParagraphStyle {
    actual companion object {
        actual val Default: PlatformParagraphStyle = implementedInJetBrainsFork()
    }

    actual fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle =
        implementedInJetBrainsFork()
}

actual class PlatformSpanStyle {
    actual companion object {
        actual val Default: PlatformSpanStyle = implementedInJetBrainsFork()
    }

    actual fun merge(other: PlatformSpanStyle?): PlatformSpanStyle = implementedInJetBrainsFork()
}

actual fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle = implementedInJetBrainsFork()

actual fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle = implementedInJetBrainsFork()
