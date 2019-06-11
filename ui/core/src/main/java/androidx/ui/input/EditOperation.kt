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

package androidx.ui.input

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import java.util.Objects

/**
 * Constants for editing operation types.
 */
/** @hide */ @RestrictTo(LIBRARY) const val COMMIT_TEXT = 0
/** @hide */ @RestrictTo(LIBRARY) const val SET_COMPOSING_REGION = 1
/** @hide */ @RestrictTo(LIBRARY) const val SET_COMPOSING_TEXT = 2
/** @hide */ @RestrictTo(LIBRARY) const val DELETE_SURROUNDING_TEXT = 3
/** @hide */ @RestrictTo(LIBRARY) const val DELETE_SURROUNDING_TEXT_IN_CODE_POINTS = 4
/** @hide */ @RestrictTo(LIBRARY) const val SET_SELECTION = 5
/** @hide */ @RestrictTo(LIBRARY) const val FINISH_COMPOSING_TEXT = 6

/**
 * A base class of all EditOperations
 *
 * An EditOperation is a representation of platform IME API call. For example, in Android,
 * InputConnection#commitText API call is translated to CommitTextEditOp object.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
open class EditOperation(val type: Int)

/**
 * An edit operation represent commitText callback from InputMethod.
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class CommitTextEditOp(
    /**
     * The text to commit. We ignore any styles in the original API.
     */
    val text: String,

    /**
     * The cursor position after inserted text.
     * See original commitText API docs for more details.
     */
    val newCursorPostion: Int
) : EditOperation(COMMIT_TEXT)

/**
 * An edit operation represents setComposingRegion callback from InputMethod.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class SetComposingRegionEditOp(
    /**
     * The inclusive start offset of the composing region.
     */
    val start: Int,

    /**
     * The exclusive end offset of the composing region
     */
    val end: Int
) : EditOperation(SET_COMPOSING_REGION)

/**
 * An edit operation represents setComposingText callback from InputMethod
 *
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setComposingText(java.lang.CharSequence,%2520int)
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class SetComposingTextEditOp(
    /**
     * The composing text.
     */
    val text: String,
    /**
     * The cursor position after setting composing text.
     * See original setComposingText API docs for more details.
     */
    val newCursorPosition: Int
) : EditOperation(SET_COMPOSING_TEXT)

/**
 * An edit operation represents deleteSurroundingText callback from InputMethod
 *
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingText(int,%2520int)
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class DeleteSurroundingTextEditOp(
    /**
     * The number of characters in UTF-16 before the cursor to be deleted.
     */
    val beforeLength: Int,
    /**
     * The number of characters in UTF-16 after the cursor to be deleted.
     */
    val afterLength: Int
) : EditOperation(DELETE_SURROUNDING_TEXT)

/**
 * An edit operation represents deleteSurroundingTextInCodePoitns callback from InputMethod
 *
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#deleteSurroundingTextInCodePoints(int,%2520int)
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class DeleteSurroundingTextInCodePointsEditOp(
    /**
     * The number oc characters in Unicode code points before the cursor to be deleted.
     */
    val beforeLength: Int,
    /**
     * The number oc characters in Unicode code points after the cursor to be deleted.
     */
    val afterLength: Int
) : EditOperation(DELETE_SURROUNDING_TEXT_IN_CODE_POINTS)

/**
 * An edit operation represents setSelection callback from InputMethod
 *
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#setSelection(int,%2520int)
 *
 * @hide
 */
@RestrictTo(LIBRARY)
data class SetSelectionEditOp(
    /**
     * The inclusive start offset of the selection region.
     */
    val start: Int,
    /**
     * The exclusive end offset of the selection region.
     */
    val end: Int
) : EditOperation(SET_SELECTION)

/**
 * An edit operation represents finishComposingText callback from InputMEthod
 *
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#finishComposingText()
 *
 * @hide
 */
@RestrictTo(LIBRARY)
class FinishComposingTextEditOp : EditOperation(FINISH_COMPOSING_TEXT) {

    // Class with empty arguments default ctor cannot be data class.
    // Treating all FinishComposingTextEditOp are equal object.
    override fun equals(other: Any?): Boolean = other is FinishComposingTextEditOp
    override fun hashCode(): Int = Objects.hashCode(this.javaClass)
}