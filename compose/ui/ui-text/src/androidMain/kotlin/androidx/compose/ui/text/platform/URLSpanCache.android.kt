/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.platform

import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.RestrictTo
import androidx.collection.LongObjectMap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.util.packInts
import java.util.WeakHashMap

/**
 * This class converts [UrlAnnotation]s to [URLSpan]s, ensuring that the same instance of [URLSpan]
 * will be returned for every instance of [UrlAnnotation]. This is required for [URLSpan]s (and
 * any ClickableSpan) to be handled correctly by accessibility services, which require every
 * ClickableSpan to have a stable ID across reads from the accessibility node. A11y services convert
 * these spans to parcelable ones, then look them up later using their ID. Since the ID is a hidden
 * property, the only way to satisfy this constraint is to actually use the same [URLSpan] instance
 * every time.
 *
 * See b/253292081.
 */
// "URL" violates naming guidelines, but that is intentional to match the platform API.
@Suppress("AcronymName")
@OptIn(ExperimentalTextApi::class)
@InternalTextApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class URLSpanCache {
    private val spansByAnnotation = WeakHashMap<UrlAnnotation, URLSpan>()
    private val urlSpansByAnnotation =
        WeakHashMap<AnnotatedString.Range<LinkAnnotation.Url>, URLSpan>()
    private val clickableSpansByAnnotation =
        WeakHashMap<AnnotatedString.Range<LinkAnnotation>, Pair<ComposeClickableSpan, Int>>()

    @Suppress("AcronymName")
    fun toURLSpan(urlAnnotation: UrlAnnotation): URLSpan =
        spansByAnnotation.getOrPut(urlAnnotation) { URLSpan(urlAnnotation.url) }

    @Suppress("AcronymName")
    fun toURLSpan(urlRange: AnnotatedString.Range<LinkAnnotation.Url>): URLSpan =
        urlSpansByAnnotation.getOrPut(urlRange) { URLSpan(urlRange.item.url) }

    /**
     * This method takes a [linkRange] which is an annotation that occupies range in Compose text
     * and converts it into a ClickableSpan passing the corresponding [linkActions] to the
     * ClickableSpan's onClick method.
     * We use [accessibilityNodeId] to invalidate cache entry for cases when the original Compose text was
     * disposed and so the corresponding link actions are not valid anymore
     */
    fun toClickableSpan(
        linkRange: AnnotatedString.Range<LinkAnnotation>,
        linkActions: LongObjectMap<() -> Unit>,
        accessibilityNodeId: Int
    ): ClickableSpan? {
        val spanWithNodeId = clickableSpansByAnnotation[linkRange]

        return if (spanWithNodeId == null || spanWithNodeId.second != accessibilityNodeId) {
            // either clickable span hasn't been created yet or the cache needs invalidation for a
            // given annotation
            linkActions[packInts(linkRange.start, linkRange.end)]?.let { action ->
                ComposeClickableSpan(action).also { newSpan ->
                    clickableSpansByAnnotation[linkRange] = Pair(newSpan, accessibilityNodeId)
                }
            }
        } else {
            spanWithNodeId.first
        }
    }
}

private class ComposeClickableSpan(private val linkAction: () -> Unit) : ClickableSpan() {
    override fun onClick(widget: View) {
        linkAction()
    }
}
