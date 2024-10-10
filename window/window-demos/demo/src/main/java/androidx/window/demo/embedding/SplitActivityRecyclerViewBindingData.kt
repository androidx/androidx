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

package androidx.window.demo.embedding

/**
 * SplitActivityRecyclerViewBindingData represents the data model for an item in the RecyclerView
 * item layout. It facilitates communication between the UI layout and the
 * [SplitActivityRecyclerViewAdapter], ensuring that the necessary data is bound to the
 * corresponding views in each item.
 */
class SplitActivityRecyclerViewBindingData {
    data class Item(
        val type: Int,
        val withDivider: Boolean = false,
    ) {
        companion object {
            const val TYPE_TEXT = 0
            const val TYPE_CHECKBOX = 1
            const val TYPE_BUTTON = 2
        }

        interface OnItemChangedListener {
            fun onItemChangedListener()
        }

        private var _text: String? = null
        var text: String?
            get() = _text
            set(value) {
                if (_text != value) {
                    _text = value
                    listener?.onItemChangedListener()
                }
            }

        private var _isChecked: Boolean = false
        var isChecked: Boolean
            get() = _isChecked
            set(value) {
                if (_isChecked != value) {
                    _isChecked = value
                    listener?.onItemChangedListener()
                }
            }

        private var _isEnabled: Boolean = true
        var isEnabled: Boolean
            get() = _isEnabled
            set(value) {
                if (_isEnabled != value) {
                    _isEnabled = value
                    listener?.onItemChangedListener()
                }
            }

        private var _isVisible: Boolean = true
        var isVisible: Boolean
            get() = _isVisible
            set(value) {
                if (_isVisible != value) {
                    _isVisible = value
                    listener?.onItemChangedListener()
                }
            }

        var listener: OnItemChangedListener? = null

        @JvmField var onClicked: (() -> Unit)? = null

        @JvmField var onCheckedChange: ((Boolean) -> Unit)? = null

        fun onTriggered() {
            when (type) {
                TYPE_BUTTON -> onClicked?.invoke()
                TYPE_CHECKBOX -> onCheckedChange?.invoke(isChecked)
                else -> Unit
            }
        }
    }

    @JvmField
    val embeddedStatusTextView =
        Item(Item.TYPE_TEXT).apply {
            text = "Activity is embedded"
            isVisible = false
        }
    @JvmField
    val embeddedBoundsTextView =
        Item(Item.TYPE_TEXT).apply {
            text = "Embedded bounds not available"
            isVisible = false
        }
    @JvmField
    val splitMainCheckBox =
        Item(Item.TYPE_CHECKBOX).apply { text = "Split Main with other activities" }
    @JvmField
    val dividerCheckBox =
        Item(Item.TYPE_CHECKBOX).apply { text = "Add a divider between containers" }
    @JvmField
    val draggableDividerCheckBox =
        Item(Item.TYPE_CHECKBOX).apply {
            text = "Make the divider draggable"
            isEnabled = false
        }
    @JvmField
    val openAnimationJumpCutCheckBox =
        Item(Item.TYPE_CHECKBOX).apply {
            text = "Use jump cut for split open animation"
            isEnabled = false
        }
    @JvmField
    val closeAnimationJumpCutCheckBox =
        Item(Item.TYPE_CHECKBOX).apply {
            text = "Use jump cut for split close animation"
            isEnabled = false
        }
    @JvmField
    val changeAnimationJumpCutCheckBox =
        Item(Item.TYPE_CHECKBOX, withDivider = true).apply {
            text = "Use jump cut for split change animation"
            isEnabled = false
        }
    @JvmField val launchBButton = Item(Item.TYPE_BUTTON).apply { text = "Launch B" }
    @JvmField
    val usePlaceholderCheckBox = Item(Item.TYPE_CHECKBOX).apply { text = "Use a placeholder for B" }
    @JvmField
    val useStickyPlaceholderCheckBox =
        Item(Item.TYPE_CHECKBOX, withDivider = true).apply { text = "Placeholder is sticky" }
    @JvmField val launchBCButton = Item(Item.TYPE_BUTTON).apply { text = "Launch B and C" }
    @JvmField val splitBCCheckBox = Item(Item.TYPE_CHECKBOX).apply { text = "Split B with C" }
    @JvmField
    val finishBCCheckBox =
        Item(Item.TYPE_CHECKBOX, withDivider = true).apply {
            text = "Finish B and C together"
            isEnabled = false
        }
    @JvmField val launchEButton = Item(Item.TYPE_BUTTON).apply { text = "Launch E" }
    @JvmField
    val fullscreenECheckBox =
        Item(Item.TYPE_CHECKBOX).apply { text = "Always launch E in fullscreen" }
    @JvmField
    val launchingEInActivityStackCheckBox =
        Item(Item.TYPE_CHECKBOX, withDivider = true).apply { text = "Launch in current container" }
    @JvmField val launchFButton = Item(Item.TYPE_BUTTON).apply { text = "Launch F" }
    @JvmField
    val launchFPendingIntentButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch F via Pending Intent" }
    @JvmField
    val splitWithFCheckBox =
        Item(Item.TYPE_CHECKBOX, withDivider = true).apply { text = "Split everything with F" }
    @JvmField val secondAppTextView = Item(Item.TYPE_TEXT).apply { text = "Second app (UID)" }
    @JvmField
    val launchUid2TrustedButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch with known certificate" }
    @JvmField
    val launchUid2UntrustedButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch in untrusted mode" }
    @JvmField
    val launchUid2UntrustedDisplayFeaturesButton =
        Item(Item.TYPE_BUTTON, withDivider = true).apply { text = "Launch display features" }
    @JvmField
    val launchExpandedDialogButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch Expanded Dialog" }
    @JvmField
    val launchDialogActivityButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch Dialog Activity" }
    @JvmField
    val launchDialogButton =
        Item(Item.TYPE_BUTTON, withDivider = true).apply { text = "Launch Dialog" }
    @JvmField
    val pinTopActivityStackButton = Item(Item.TYPE_BUTTON).apply { text = "Pin Top ActivityStack" }
    @JvmField
    val stickyPinRuleCheckBox = Item(Item.TYPE_CHECKBOX).apply { text = "Set Pin Rule Sticky" }
    @JvmField
    val unpinTopActivityStackButton =
        Item(Item.TYPE_BUTTON, withDivider = true).apply { text = "Unpin Top ActivityStack" }
    @JvmField
    val launchOverlayAssociatedActivityButton =
        Item(Item.TYPE_BUTTON).apply { text = "Launch Overlay Associated Activity A" }

    fun getItems() =
        listOf(
            embeddedStatusTextView,
            embeddedBoundsTextView,
            splitMainCheckBox,
            dividerCheckBox,
            draggableDividerCheckBox,
            launchBButton,
            usePlaceholderCheckBox,
            useStickyPlaceholderCheckBox,
            launchBCButton,
            splitBCCheckBox,
            finishBCCheckBox,
            launchEButton,
            fullscreenECheckBox,
            launchingEInActivityStackCheckBox,
            launchFButton,
            launchFPendingIntentButton,
            splitWithFCheckBox,
            secondAppTextView,
            launchUid2TrustedButton,
            launchUid2UntrustedButton,
            launchUid2UntrustedDisplayFeaturesButton,
            launchExpandedDialogButton,
            launchDialogActivityButton,
            launchDialogButton,
            pinTopActivityStackButton,
            stickyPinRuleCheckBox,
            unpinTopActivityStackButton,
            launchOverlayAssociatedActivityButton,
        )
}
