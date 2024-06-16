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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import com.google.common.collect.ImmutableMap
import com.google.common.primitives.ImmutableIntArray

/** Emoji picker popup with multi-skintone selection panel. */
internal class EmojiPickerPopupMultiSkintoneDesign(
    override val context: Context,
    override val targetEmojiView: View,
    override val variants: List<String>,
    override val popupView: LinearLayout,
    override val emojiViewOnClickListener: View.OnClickListener,
    targetEmoji: String
) : EmojiPickerPopupDesign() {

    private val inflater = LayoutInflater.from(context)
    private val resultRow =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
        }

    private var selectedLeftSkintone = -1
    private var selectedRightSkintone = -1

    init {
        val triggerVariantIndex: Int = variants.indexOf(targetEmoji)
        if (triggerVariantIndex > 0) {
            selectedLeftSkintone = (triggerVariantIndex - 1) / getNumberOfColumns()
            selectedRightSkintone =
                triggerVariantIndex - selectedLeftSkintone * getNumberOfColumns() - 1
        }
    }

    override fun addRowsToPopupView() {
        for (row in 0 until getActualNumberOfRows()) {
            val rowLayout =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams =
                        LinearLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                }
            for (column in 0 until getNumberOfColumns()) {
                inflater.inflate(R.layout.emoji_picker_popup_image_view, rowLayout)
                val imageView = rowLayout.getChildAt(column) as ImageView
                imageView.apply {
                    layoutParams =
                        LinearLayout.LayoutParams(targetEmojiView.width, targetEmojiView.height)
                    isClickable = true
                    contentDescription = getImageContentDescription(context, row, column)
                    if (
                        (hasLeftSkintone() && row == 0 && selectedLeftSkintone == column) ||
                            (hasRightSkintone() && row == 1 && selectedRightSkintone == column)
                    ) {
                        isSelected = true
                        isClickable = false
                    }
                    setImageDrawable(getDrawableRes(context, row, column))
                    setOnClickListener {
                        var unSelectedView: View? = null
                        if (row == 0) {
                            if (hasLeftSkintone()) {
                                unSelectedView = rowLayout.getChildAt(selectedLeftSkintone)
                            }
                            selectedLeftSkintone = column
                        } else {
                            if (hasRightSkintone()) {
                                unSelectedView = rowLayout.getChildAt(selectedRightSkintone)
                            }
                            selectedRightSkintone = column
                        }
                        if (unSelectedView != null) {
                            unSelectedView.isSelected = false
                            unSelectedView.isClickable = true
                        }
                        isClickable = false
                        isSelected = true
                        processResultView()
                    }
                }
            }
            popupView.addView(rowLayout)
        }
    }

    private fun processResultView() {
        val childCount = resultRow.childCount
        if (childCount < 1 || childCount > 2) {
            Log.e(TAG, "processResultEmojiForRectangleLayout(): unexpected emoji result row size")
            return
        }
        // Remove the result emoji if it's already available. It will be available after the row is
        // inflated the first time.
        if (childCount == 2) {
            resultRow.removeViewAt(1)
        }
        if (hasLeftSkintone() && hasRightSkintone()) {
            inflater.inflate(R.layout.emoji_picker_popup_emoji_view, resultRow)
            val layout = resultRow.getChildAt(1) as LinearLayout
            layout.findViewById<EmojiView>(R.id.emoji_picker_popup_emoji_view).apply {
                willDrawVariantIndicator = false
                isClickable = true
                emoji =
                    variants[
                        selectedLeftSkintone * getNumberOfColumns() + selectedRightSkintone + 1]
                setOnClickListener(emojiViewOnClickListener)
                layoutParams =
                    LinearLayout.LayoutParams(targetEmojiView.width, targetEmojiView.height)
            }
            layout.findViewById<LinearLayout>(R.id.emoji_picker_popup_emoji_view_wrapper).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        targetEmojiView.width * getNumberOfColumns() / 2,
                        targetEmojiView.height
                    )
            }
        } else if (hasLeftSkintone()) {
            drawImageView(
                /* row= */ 0,
                /*column=*/ selectedLeftSkintone,
                /* applyGrayTint= */ false
            )
        } else if (hasRightSkintone()) {
            drawImageView(
                /* row= */ 1,
                /*column=*/ selectedRightSkintone,
                /* applyGrayTint= */ false
            )
        } else {
            drawImageView(/* row= */ 0, /* column= */ 0, /* applyGrayTint= */ true)
        }
    }

    private fun drawImageView(row: Int, column: Int, applyGrayTint: Boolean) {
        inflater
            .inflate(R.layout.emoji_picker_popup_image_view, resultRow)
            .findViewById<ImageView>(R.id.emoji_picker_popup_image_view)
            .apply {
                layoutParams = LinearLayout.LayoutParams(0, targetEmojiView.height, 1f)
                setImageDrawable(getDrawableRes(context, row, column))
                if (applyGrayTint) {
                    imageTintList = ColorStateList.valueOf(Color.GRAY)
                }

                var contentDescriptionRow = selectedLeftSkintone
                var contentDescriptionColumn = selectedRightSkintone
                if (hasLeftSkintone()) {
                    contentDescriptionRow = 0
                    contentDescriptionColumn = selectedLeftSkintone
                } else if (hasRightSkintone()) {
                    contentDescriptionRow = 1
                    contentDescriptionColumn = selectedRightSkintone
                }
                contentDescription =
                    getImageContentDescription(
                        context,
                        contentDescriptionRow,
                        contentDescriptionColumn
                    )
            }
    }

    override fun addLayoutFooter() {
        inflater.inflate(R.layout.emoji_picker_popup_emoji_view, resultRow)
        val layout = resultRow.getChildAt(0) as LinearLayout
        layout.findViewById<EmojiView>(R.id.emoji_picker_popup_emoji_view).apply {
            willDrawVariantIndicator = false
            emoji = variants[0]
            layoutParams = LinearLayout.LayoutParams(targetEmojiView.width, targetEmojiView.height)
            isClickable = true
            setOnClickListener(emojiViewOnClickListener)
        }
        layout.findViewById<LinearLayout>(R.id.emoji_picker_popup_emoji_view_wrapper).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    targetEmojiView.width * getNumberOfColumns() / 2,
                    targetEmojiView.height
                )
        }
        processResultView()
        popupView.addView(resultRow)
    }

    override fun getNumberOfRows(): Int {
        // Add one extra row for the neutral skin tone combination
        return LAYOUT_ROWS + 1
    }

    override fun getNumberOfColumns(): Int {
        return LAYOUT_COLUMNS
    }

    private fun getActualNumberOfRows(): Int {
        return LAYOUT_ROWS
    }

    private fun hasLeftSkintone(): Boolean {
        return selectedLeftSkintone != -1
    }

    private fun hasRightSkintone(): Boolean {
        return selectedRightSkintone != -1
    }

    private fun getDrawableRes(context: Context, row: Int, column: Int): Drawable? {
        val resArray: ImmutableIntArray? = SKIN_TONES_EMOJI_TO_RESOURCES[variants[0]]
        if (resArray != null) {
            val contextThemeWrapper = ContextThemeWrapper(context, VARIANT_STYLES[column])
            return ResourcesCompat.getDrawable(
                context.resources,
                resArray[row],
                contextThemeWrapper.getTheme()
            )
        }
        return null
    }

    private fun getImageContentDescription(context: Context, row: Int, column: Int): String {
        return context.getString(
            R.string.emoji_variant_content_desc_template,
            context.getString(getSkintoneStringRes(/* isLeft= */ true, row, column)),
            context.getString(getSkintoneStringRes(/* isLeft= */ false, row, column))
        )
    }

    @StringRes
    private fun getSkintoneStringRes(isLeft: Boolean, row: Int, column: Int): Int {
        // When there is no column, the selected position -1 will be passed in as column.
        if (column == -1) {
            return R.string.emoji_skin_tone_shadow_content_desc
        }
        return if (isLeft) {
            if (row == 0) SKIN_TONE_CONTENT_DESC_RES_IDS[column]
            else R.string.emoji_skin_tone_shadow_content_desc
        } else {
            if (row == 0) R.string.emoji_skin_tone_shadow_content_desc
            else SKIN_TONE_CONTENT_DESC_RES_IDS[column]
        }
    }

    companion object {
        private const val TAG = "MultiSkintoneDesign"
        private const val LAYOUT_ROWS = 2
        private const val LAYOUT_COLUMNS = 5

        private val SKIN_TONE_CONTENT_DESC_RES_IDS =
            ImmutableIntArray.of(
                R.string.emoji_skin_tone_light_content_desc,
                R.string.emoji_skin_tone_medium_light_content_desc,
                R.string.emoji_skin_tone_medium_content_desc,
                R.string.emoji_skin_tone_medium_dark_content_desc,
                R.string.emoji_skin_tone_dark_content_desc
            )

        private val VARIANT_STYLES =
            ImmutableIntArray.of(
                R.style.EmojiSkintoneSelectorLight,
                R.style.EmojiSkintoneSelectorMediumLight,
                R.style.EmojiSkintoneSelectorMedium,
                R.style.EmojiSkintoneSelectorMediumDark,
                R.style.EmojiSkintoneSelectorDark
            )

        /**
         * Map from emoji that use the square layout strategy with skin tone swatches or rectangle
         * strategy to their resources.
         */
        private val SKIN_TONES_EMOJI_TO_RESOURCES =
            ImmutableMap.Builder<String, ImmutableIntArray>()
                .put(
                    "🤝",
                    ImmutableIntArray.of(
                        R.drawable.handshake_skintone_shadow,
                        R.drawable.handshake_shadow_skintone
                    )
                )
                .put(
                    "👭",
                    ImmutableIntArray.of(
                        R.drawable.holding_women_skintone_shadow,
                        R.drawable.holding_women_shadow_skintone
                    )
                )
                .put(
                    "👫",
                    ImmutableIntArray.of(
                        R.drawable.holding_woman_man_skintone_shadow,
                        R.drawable.holding_woman_man_shadow_skintone
                    )
                )
                .put(
                    "👬",
                    ImmutableIntArray.of(
                        R.drawable.holding_men_skintone_shadow,
                        R.drawable.holding_men_shadow_skintone
                    )
                )
                .put(
                    "🧑‍🤝‍🧑",
                    ImmutableIntArray.of(
                        R.drawable.holding_people_skintone_shadow,
                        R.drawable.holding_people_shadow_skintone
                    )
                )
                .put(
                    "💏",
                    ImmutableIntArray.of(
                        R.drawable.kiss_people_skintone_shadow,
                        R.drawable.kiss_people_shadow_skintone
                    )
                )
                .put(
                    "👩‍❤️‍💋‍👨",
                    ImmutableIntArray.of(
                        R.drawable.kiss_woman_man_skintone_shadow,
                        R.drawable.kiss_woman_man_shadow_skintone
                    )
                )
                .put(
                    "👨‍❤️‍💋‍👨",
                    ImmutableIntArray.of(
                        R.drawable.kiss_men_skintone_shadow,
                        R.drawable.kiss_men_shadow_skintone
                    )
                )
                .put(
                    "👩‍❤️‍💋‍👩",
                    ImmutableIntArray.of(
                        R.drawable.kiss_women_skintone_shadow,
                        R.drawable.kiss_women_shadow_skintone
                    )
                )
                .put(
                    "💑",
                    ImmutableIntArray.of(
                        R.drawable.couple_heart_people_skintone_shadow,
                        R.drawable.couple_heart_people_shadow_skintone
                    )
                )
                .put(
                    "👩‍❤️‍👨",
                    ImmutableIntArray.of(
                        R.drawable.couple_heart_woman_man_skintone_shadow,
                        R.drawable.couple_heart_woman_man_shadow_skintone
                    )
                )
                .put(
                    "👨‍❤️‍👨",
                    ImmutableIntArray.of(
                        R.drawable.couple_heart_men_skintone_shadow,
                        R.drawable.couple_heart_men_shadow_skintone
                    )
                )
                .put(
                    "👩‍❤️‍👩",
                    ImmutableIntArray.of(
                        R.drawable.couple_heart_women_skintone_shadow,
                        R.drawable.couple_heart_women_shadow_skintone
                    )
                )
                .buildOrThrow()
    }
}
