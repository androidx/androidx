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

package androidx.compose.foundation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

/**
 * [PointerMatcher] represents a single condition or a set of conditions which a [PointerEvent] has to match
 * in order to count as an appropriate event for a gesture.
 *
 *  Supported matchers:
 * - [mouse] - will match an event with [PointerType.Mouse] with a required [PointerButton]
 * - [touch] - will match any event with [PointerType.Touch]
 * - [stylus] - will match an event with [PointerType.Stylus]. And optional [PointerButton] can be specified.
 * - [eraser] - will match any event with [PointerType.Eraser]
 * - [pointer] - takes in [PointerType] and optional [PointerButton]
 *
 * Their combination is supported using plus operator:
 * ```
 * mouse(PointerButton.Primary) + touch + stylus + eraser
 * ```
 * See [Primary].
 *
 * Note: Currently, Compose for Desktop receives all event types as Mouse events:
 * Touch, Stylus and Eraser events are emulated as Mouse events.
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalComposeUiApi::class)
fun interface PointerMatcher {

    @ExperimentalFoundationApi
    fun matches(event: PointerEvent): Boolean

    @ExperimentalFoundationApi
    operator fun plus(pointerMatcher: PointerMatcher): PointerMatcher {
        val sources = buildList {
            for (matcher in listOf(this@PointerMatcher, pointerMatcher)) {
                if (matcher is CombinedPointerMatcher)
                    addAll(matcher.sources)
                else
                    add(matcher)
            }
        }
        return CombinedPointerMatcher(sources)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        @ExperimentalFoundationApi
        fun pointer(
            pointerType: PointerType,
            button: PointerButton? = null
        ): PointerMatcher = PointerTypeAndButtonMatcher(pointerType, button)

        @ExperimentalFoundationApi
        fun mouse(button: PointerButton): PointerMatcher = pointer(PointerType.Mouse, button)

        @ExperimentalFoundationApi
        fun stylus(button: PointerButton? = null): PointerMatcher =
            pointer(PointerType.Stylus, button)

        @ExperimentalFoundationApi
        val stylus: PointerMatcher = stylus(button = null)

        @ExperimentalFoundationApi
        val touch: PointerMatcher = PointerTypeAndButtonMatcher(PointerType.Touch)

        @ExperimentalFoundationApi
        val eraser: PointerMatcher =
            PointerTypeAndButtonMatcher(PointerType.Eraser, matchAllButtons = true)

        private class PointerTypeAndButtonMatcher(
            val pointerType: PointerType,
            val button: PointerButton? = null,
            val matchAllButtons: Boolean = false,
        ) : PointerMatcher {
            override fun matches(event: PointerEvent): Boolean {
                return (matchAllButtons || (event.button == button))
                    && event.changes.fastAll { it.type == pointerType }
            }
        }

        private class CombinedPointerMatcher(val sources: List<PointerMatcher>) : PointerMatcher {
            override fun matches(event: PointerEvent): Boolean {
                return sources.any { it.matches(event) }
            }
        }

        /**
         * The Primary [PointerMatcher] covers the most common cases of pointer inputs.
         * [Primary] will match [PointerEvent]s, which match at least one of the following conditions:
         * - [PointerType] is [PointerType.Mouse] and [PointerEvent.button] is [PointerButton.Primary]
         * - [PointerType] is [PointerType.Touch]
         * - [PointerType] is [PointerType.Stylus], no buttons pressed
         * - [PointerType] is [PointerType.Eraser]
         */
        @ExperimentalFoundationApi
        val Primary: PointerMatcher = CombinedPointerMatcher(
            listOf(
                mouse(PointerButton.Primary),
                touch,
                stylus,
                eraser
            )
        )
    }
}
