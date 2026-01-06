/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.spec

import com.android.mechanics.haptics.SegmentHaptics

/** Returns a string representation of the [MotionSpec] for debugging by humans. */
fun MotionSpec.toDebugString(): String {
    return buildString {
            if (minDirection == maxDirection) {
                appendLine("unidirectional:")
                appendLine(minDirection.toDebugString().prependIndent("  "))
            } else {
                appendLine("maxDirection:")
                appendLine(maxDirection.toDebugString().prependIndent("  "))
                appendLine("minDirection:")
                appendLine(minDirection.toDebugString().prependIndent("  "))
            }

            if (segmentHandlers.isNotEmpty()) {
                appendLine("segmentHandlers:")
                segmentHandlers.keys.forEach {
                    appendIndent(2)
                    appendSegmentKey(it)
                    appendLine()
                }
            }
        }
        .trim()
}

/** Returns a string representation of the [DirectionalMotionSpec] for debugging by humans. */
fun DirectionalMotionSpec.toDebugString(): String {
    return buildString {
            appendBreakpointLine(breakpoints.first())
            for (i in mappings.indices) {
                appendMappingLine(mappings[i], indent = 2)
                appendSegmentHapticsLine(haptics[i], indent = 2)
                semantics.forEach { appendSemanticsLine(it.key, it.values[i], indent = 4) }
                appendBreakpointLine(breakpoints[i + 1])
            }
        }
        .trim()
}

private fun StringBuilder.appendIndent(indent: Int) {
    repeat(indent) { append(' ') }
}

private fun StringBuilder.appendBreakpointLine(breakpoint: Breakpoint, indent: Int = 0) {
    appendIndent(indent)
    append("@")
    append(breakpoint.position)

    append(" [")
    appendBreakpointKey(breakpoint.key)
    append("]")

    if (breakpoint.guarantee != Guarantee.None) {
        append(" guarantee=")
        append(breakpoint.key.debugLabel)
    }

    if (!breakpoint.spring.isSnapSpring) {
        append(" spring=")
        append(breakpoint.spring.stiffness)
        append("/")
        append(breakpoint.spring.dampingRatio)
    }

    append(" [")
    append("breakpointHaptics=")
    append(breakpoint.breakpointHaptics.toString())
    append("]")

    appendLine()
}

private fun StringBuilder.appendBreakpointKey(key: BreakpointKey) {
    if (key.debugLabel != null) {
        append(key.debugLabel)
        append("|")
    }
    append("id:0x")
    append(key.identity.hashCode().toString(16).padStart(8, '0'))
}

private fun StringBuilder.appendSegmentKey(key: SegmentKey) {
    appendBreakpointKey(key.minBreakpoint)
    if (key.direction == InputDirection.Min) append(" << ") else append(" >> ")
    appendBreakpointKey(key.maxBreakpoint)
}

private fun StringBuilder.appendMappingLine(mapping: Mapping, indent: Int = 0) {
    appendIndent(indent)
    append(mapping.toString())
    appendLine()
}

private fun StringBuilder.appendSegmentHapticsLine(
    segmentHaptics: SegmentHaptics,
    indent: Int = 0,
) {
    appendIndent(indent)
    append("segment haptics: $segmentHaptics")
    appendLine()
}

private fun StringBuilder.appendSemanticsLine(
    semanticKey: SemanticKey<*>,
    value: Any?,
    indent: Int = 0,
) {
    appendIndent(indent)

    append(semanticKey.debugLabel)
    append("[id:0x")
    append(semanticKey.identity.hashCode().toString(16).padStart(8, '0'))
    append("]")

    append("=")
    append(value)
    appendLine()
}
