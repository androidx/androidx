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

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIView

/**
 * A class that manages a hierarchical list of focused views, enabling nested focus handling
 * and priority management for UI elements.
 * The FocusedViewsList makes a first responder the last active view in the last child in the tree.
 */
internal class FocusedViewsList {

    private var activeViews = emptyList<UIView>()
    private var resignedViews = emptyList<UIView>()
    private val mainScope = MainScope()

    private var parent: FocusedViewsList? = null
    private val children = mutableListOf<FocusedViewsList>()

    /**
     * Add new view to list and focus on it.
     */
    fun addAndFocus(view: UIView) {
        activeViews += view
        resignedViews -= view
        rootList().onListHierarchyChanged(delayMillis = null)
    }

    /**
     * Remove the view from the list and resigns first responder.
     * The last element in the list will become a new first responder.
     */
    fun remove(view: UIView, delayMillis: Long = 0) {
        if (activeViews.contains(view)) {
            resignedViews += view
            activeViews = activeViews.filter { it != view }

            rootList().onListHierarchyChanged(delayMillis)
        }
    }

    /**
     * Create a new child list that will have focus priority over the parent list and previously
     * created children of the parent list.
     */
    fun childFocusedViewsList(): FocusedViewsList {
        return FocusedViewsList().also {
            children.add(it)
            it.parent = this
        }
    }

    /**
     * Dispose the child list, providing focus back to the parent list.
     */
    fun disposeChild() {
        assert(parent != null) { "Only child focused views list can be disposed." }
        parent?.children?.remove(this)
        parent?.let {
            parent = null
            it.rootList().onListHierarchyChanged(delayMillis = 0)
        }
        children.fastForEach { it.disposeChild() }

        resignedViews += activeViews
        activeViews = emptyList()
        mainScope.launch {
            resignScheduledViews()
        }
    }

    private fun onListHierarchyChanged(delayMillis: Long?) {
        fun refocusOnLastViewInHierarchy() {
            val viewToFocus = lastViewToFocus()
            if (viewToFocus != null) {
                viewToFocus.becomeFirstResponder()
                viewToFocus.window?.makeKeyWindow()
            } else {
                resignScheduledViews()
            }
        }
        if (delayMillis == null) {
            refocusOnLastViewInHierarchy()
        } else {
            mainScope.launch {
                delay(delayMillis)
                refocusOnLastViewInHierarchy()
            }
        }
    }

    private fun lastViewToFocus(): UIView? {
        return children.reversed().firstNotNullOfOrNull { it.lastViewToFocus() }
            ?: activeViews.lastOrNull()
    }

    private fun resignScheduledViews() {
        resignedViews.fastForEachReversed {
            it.resignFirstResponder()
        }
        resignedViews = emptyList()
        children.fastForEach { it.resignScheduledViews() }
    }

    private fun rootList(): FocusedViewsList {
        var root = this
        while (root.parent != null) {
            root = root.parent!!
        }
        return root
    }
}
