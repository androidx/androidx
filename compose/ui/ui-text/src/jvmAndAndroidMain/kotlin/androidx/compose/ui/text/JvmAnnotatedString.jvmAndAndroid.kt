/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntIntMap
import androidx.collection.mutableIntIntMapOf
import androidx.compose.ui.text.AnnotatedString.Annotation
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import java.util.SortedSet

/**
 * The core function of [AnnotatedString] transformation.
 *
 * @param transform the transformation method
 * @return newly allocated transformed AnnotatedString
 */
internal actual fun AnnotatedString.transform(
    transform: (String, Int, Int) -> String
): AnnotatedString {
    if (!PackedTextStylesLatch.isEnabled) {
        return legacyTransform(transform)
    }
    val ranges = annotations
    if (ranges.isNullOrEmpty()) {
        return AnnotatedString(
            text = if (text.isNotEmpty()) transform(text, 0, text.length) else "",
            annotations = null,
        )
    }
    val cutsSize = 2 * ranges.size + 2
    val cuts = IntArray(cutsSize)
    cuts[1] = text.length
    for (i in ranges.indices) {
        val range = ranges[i]
        cuts[2 * i + 2] = range.start
        cuts[2 * i + 3] = range.end
    }
    cuts.sort()
    val builder = StringBuilder(text.length)
    var offsetMap: MutableIntIntMap? = null
    var previous = 0
    for (i in 1 until cutsSize) {
        val cut = cuts[i]
        if (cut == previous) continue
        builder.append(transform(text, previous, cut))
        if (offsetMap == null && builder.length != cut) {
            offsetMap = MutableIntIntMap(cutsSize)
            for (j in 0 until i) offsetMap.put(cuts[j], cuts[j])
        }
        offsetMap?.put(cut, builder.length)
        previous = cut
    }
    val map = offsetMap ?: return AnnotatedString(text = builder.toString(), annotations = ranges)
    val newAnnotations = ArrayList<Range<out Annotation>>(ranges.size)
    ranges.fastForEach { newAnnotations.add(Range(it.item, map[it.start], map[it.end], it.tag)) }
    return AnnotatedString(text = builder.toString(), annotations = newAnnotations)
}

/** Legacy flag-off implementation of [AnnotatedString.transform]. */
@VisibleForTesting
internal fun AnnotatedString.legacyTransform(
    transform: (String, Int, Int) -> String
): AnnotatedString {
    val transitions = sortedSetOf(0, text.length)
    collectRangeTransitions(annotations, transitions)

    var resultStr = ""
    val offsetMap = mutableIntIntMapOf()
    offsetMap[0] = 0
    transitions.windowed(size = 2) { (start, end) ->
        resultStr += transform(text, start, end)
        offsetMap.put(end, resultStr.length)
    }
    // The offset map must have mapping entry from all style start, end position.
    val newAnnotations = annotations?.fastMap {
        Range(it.item, offsetMap[it.start], offsetMap[it.end], it.tag)
    }

    return AnnotatedString(text = resultStr, annotations = newAnnotations)
}

/**
 * Adds all [AnnotatedString.Range] transition points
 *
 * @param ranges The list of AnnotatedString.Range
 * @param target The output list
 */
private fun collectRangeTransitions(ranges: List<Range<*>>?, target: SortedSet<Int>) {
    ranges?.fastFold(target) { acc, range ->
        acc.apply {
            add(range.start)
            add(range.end)
        }
    }
}
