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

package androidx.compose.ui.window

import androidx.compose.ui.util.fastForEachReversed
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIView

internal class FocusedViewsList {

    private var activeViews = emptyList<UIView>()
    private var resignedViews = emptyList<UIView>()
    private val mainScope = MainScope()

    /**
     * Add new view to list and focus on it.
     */
    fun addAndFocus(view: UIView) {
        activeViews += view
        resignedViews -= view
        view.becomeFirstResponder()
    }

    /**
     * Remove the view from the list and resigns first responder.
     * The last element in the list will become a new first responder.
     */
    fun remove(view: UIView, delayMillis: Long = 0) {
        if (activeViews.contains(view)) {
            resignedViews += view
            activeViews = activeViews.filter { it != view }

            mainScope.launch {
                delay(delayMillis)
                resignedViews.fastForEachReversed {
                    it.resignFirstResponder()
                }
                resignedViews = emptyList()
                activeViews.lastOrNull()?.let {
                    if (!it.isFirstResponder) {
                        it.becomeFirstResponder()
                    }
                }
            }
        }
    }
}
