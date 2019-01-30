/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.semantics

/**
 * Provides hint values which override the default hints on supported
 * platforms.
 *
 * On iOS, these values are always ignored.
 */
// @immutable
data class SemanticsHintOverrides(
    /**
     * The hint text for a tap action.
     *
     * If null, the standard hint is used instead.
     *
     * The hint should describe what happens when a tap occurs, not the
     * manner in which a tap is accomplished.
     *
     * Bad: 'Double tap to show movies'.
     * Good: 'show movies'.
     */
    val onTapHint: String?,
    /**
     * The hint text for a long press action.
     *
     * If null, the standard hint is used instead.
     *
     * The hint should describe what happens when a long press occurs, not
     * the manner in which the long press is accomplished.
     *
     * Bad: 'Double tap and hold to show tooltip'.
     * Good: 'show tooltip'.
     */
    val onLongPressHint: String?
) {

    init {
        assert(onTapHint != "")
        assert(onLongPressHint != "")
    }

    /** Whether there are any non-null hint values. */
    val isNotEmpty
        get() = onTapHint != null || onLongPressHint != null
}