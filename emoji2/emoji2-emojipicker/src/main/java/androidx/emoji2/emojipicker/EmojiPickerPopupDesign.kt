/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Emoji picker popup view UI design. Each UI design needs to inherit this abstract class.
 */
internal abstract class EmojiPickerPopupDesign {
    abstract val context: Context
    abstract val targetEmojiView: View
    abstract val variants: List<String>
    abstract val popupView: LinearLayout
    abstract val emojiViewOnClickListener: View.OnClickListener
    lateinit var template: Array<IntArray>
    open fun addLayoutHeader() {
        // no-ops
    }
    open fun addRowsToPopupView() {
        for (row in template) {
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (item in row) {
                val cell =
                    if (item == 0) {
                        EmojiView(context)
                    } else {
                        EmojiView(context).apply {
                            willDrawVariantIndicator = false
                            emoji = variants[item - 1]
                            setOnClickListener(emojiViewOnClickListener)
                            if (item == 1) {
                                // Hover on the first emoji in the popup
                                popupView.post {
                                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                                }
                            }
                        }
                    }.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            targetEmojiView.width, targetEmojiView.height)
                    }
                rowLayout.addView(cell)
            }
            popupView.addView(rowLayout)
        }
    }

    open fun addLayoutFooter() {
        // no-ops
    }

    abstract fun getNumberOfRows(): Int
    abstract fun getNumberOfColumns(): Int
}
