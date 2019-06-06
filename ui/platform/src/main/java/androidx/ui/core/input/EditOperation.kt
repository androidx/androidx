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

package androidx.ui.core.input

/**
 * An enum class for the type of edit operations.
 */
internal enum class OpType {
    COMMIT_TEXT,
    // TODO(nona): Introduce other API callback, setComposingRange, etc.
}

/**
 * A base class of all EditOperations
 *
 * An EditOperation is a representation of platform IME API call. For example, in Android,
 * InputConnection#commitText API call is translated to CommitTextEditOp object.
 */
internal open class EditOperation(val type: OpType)

/**
 * An edit opration represent commitText callback from InputMethod.
 * @see https://developer.android.com/reference/android/view/inputmethod/InputConnection.html#commitText(java.lang.CharSequence,%20int)
 */
internal data class CommitTextEditOp(
    /**
     * The text to commit. We ignore any styles in the original API.
     */
    val text: String,

    /**
     * The cursor position after inserted text.
     * See original commitText API docs for more details.
     */
    val newCursorPostion: Int
) : EditOperation(OpType.COMMIT_TEXT)
