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
import kotlinx.coroutines.launch
import platform.UIKit.UIView

/**
 * Stack to remember previously focused UIView.
 */
internal interface FocusStack<V> {

    /**
     * Add new view to stack and focus on it.
     */
    fun pushAndFocus(view: V)

    /**
     * Pop all elements until some element. Also pop this element too.
     * Last remaining element in Stack will be focused.
     */
    fun popUntilNext(view: V)

    /**
     * Return first added view or null
     */
    fun first(): V?
}

internal class FocusStackImpl : FocusStack<UIView> {

    private var activeViews = emptyList<UIView>()
    private var resignedViews = emptyList<UIView>()
    private val mainScope = MainScope()

    override fun pushAndFocus(view: UIView) {
        activeViews += view
        resignedViews -= view
        view.becomeFirstResponder()
    }

    override fun popUntilNext(view: UIView) {
        if (activeViews.contains(view)) {
            val index = activeViews.indexOf(view)
            resignedViews += activeViews.subList(index, activeViews.size)
            activeViews = activeViews.subList(0, index)

            mainScope.launch {
                resignedViews.fastForEachReversed {
                    it.resignFirstResponder()
                }
                resignedViews = emptyList()
                activeViews.lastOrNull()?.becomeFirstResponder()
            }
        }
    }

    override fun first(): UIView? = activeViews.firstOrNull()
}
