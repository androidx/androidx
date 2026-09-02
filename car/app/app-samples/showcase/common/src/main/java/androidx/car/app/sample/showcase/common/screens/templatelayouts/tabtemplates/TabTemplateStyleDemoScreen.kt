/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.tabtemplates

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarIconStyle
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Shape
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabStyle
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.TabTemplate.TabCallback
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.graphics.drawable.IconCompat

/** Creates a screen that demonstrates custom styling and colors for [TabTemplate] and [Tab]s. */
@OptIn(ExperimentalCarApi::class)
class TabTemplateStyleDemoScreen(carContext: CarContext) : Screen(carContext) {
    private var mActiveContentId: String = "0"

    private data class TabProperty(
        @StringRes val titleRes: Int,
        @StringRes val messageRes: Int,
        @DrawableRes val selectedIconRes: Int,
        @DrawableRes val unselectedIconRes: Int = selectedIconRes,
        val iconColor: CarColor? = null,
        val customStyle: TabStyle? = null,
    )

    private companion object {
        private val greenIndicatorCarColor =
            CarColor.createCustom(0xFFBDE6C1.toInt(), 0xFF2D6633.toInt())
        private val greenOnIndicatorColor =
            CarColor.createCustom(0xFF163319.toInt(), 0xFFBDE6C1.toInt())

        private val blueIndicatorCarColor =
            CarColor.createCustom(0xFFD3E3FD.toInt(), 0xFF0842A0.toInt()) // Background pill
        private val blueOnIndicatorColor =
            CarColor.createCustom(0xFF0A2F6E.toInt(), 0xFFD3E3FD.toInt()) // Text & Icon color

        private val tabTemplateStyle =
            TabStyle.Builder()
                .setSelectedBackgroundColor(greenIndicatorCarColor)
                .setTextColor(greenOnIndicatorColor)
                .build()

        private val tabCustomStyle =
            TabStyle.Builder(tabTemplateStyle)
                .setTextColor(blueOnIndicatorColor)
                .setSelectedBackgroundColor(blueIndicatorCarColor)
                .build()

        private val tabFailingStyle =
            TabStyle.Builder(tabTemplateStyle)
                .setTextColor(CarColor.RED)
                .setSelectedBackgroundColor(CarColor.RED)
                .build()

        private val TAB_PROPERTIES =
            listOf(
                TabProperty(
                    titleRes = R.string.tab_title_template_style_1,
                    messageRes = R.string.msg_tab_template_style_1_text,
                    selectedIconRes = R.drawable.ic_home_filled_24px,
                    unselectedIconRes = R.drawable.ic_home_24px,
                    iconColor = greenOnIndicatorColor,
                ),
                TabProperty(
                    titleRes = R.string.tab_title_template_style_2,
                    messageRes = R.string.msg_tab_template_style_2_text,
                    selectedIconRes = R.drawable.ic_event_note_filled_24px,
                    unselectedIconRes = R.drawable.ic_event_note_24px,
                    iconColor = greenOnIndicatorColor,
                ),
                TabProperty(
                    titleRes = R.string.tab_title_overridden_style,
                    messageRes = R.string.msg_tab_overridden_style_text,
                    selectedIconRes = R.drawable.ic_favorite_filled_white_24dp,
                    unselectedIconRes = R.drawable.ic_favorite_white_24dp,
                    iconColor = blueOnIndicatorColor,
                    customStyle = tabCustomStyle,
                ),
                TabProperty(
                    titleRes = R.string.tab_title_failing_style,
                    messageRes = R.string.msg_tab_failing_style_text,
                    selectedIconRes = R.drawable.ic_settings_filled_24px,
                    unselectedIconRes = R.drawable.ic_settings_24px,
                    iconColor = CarColor.RED,
                    customStyle = tabFailingStyle,
                ),
            )
    }

    /** Creates a [Tab] with configurable selected/unselected icon resources and tint colors. */
    private fun createTab(
        contentId: String,
        @StringRes titleRes: Int,
        @DrawableRes selectedIconRes: Int,
        @DrawableRes unselectedIconRes: Int = selectedIconRes,
        iconColor: CarColor? = null,
        isSelected: Boolean = false,
        customStyle: TabStyle? = null,
    ): Tab {
        val iconRes = if (isSelected) selectedIconRes else unselectedIconRes

        val iconCompat = IconCompat.createWithResource(carContext, iconRes)
        val iconStyle =
            CarIconStyle.Builder(CarIconStyle.TINTED)
                .apply {
                    if (iconColor != null) {
                        setTint(iconColor)
                    }
                }
                .build()
        val icon = CarIcon.Builder(iconCompat, iconStyle).build()

        val tabBuilder =
            Tab.Builder()
                .setContentId(contentId)
                .setTitle(carContext.getString(titleRes))
                .setIcon(icon)

        if (customStyle != null) {
            tabBuilder.setStyle(customStyle)
        }

        return tabBuilder.build()
    }

    private fun buildTabs(): List<Tab> {
        return TAB_PROPERTIES.mapIndexed { index, prop ->
            val contentId = "$index"
            createTab(
                contentId = contentId,
                titleRes = prop.titleRes,
                selectedIconRes = prop.selectedIconRes,
                unselectedIconRes = prop.unselectedIconRes,
                isSelected = (contentId == mActiveContentId),
                iconColor = prop.iconColor,
                customStyle = prop.customStyle,
            )
        }
    }

    override fun onGetTemplate(): Template {
        if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_9) {
            val backAction =
                Action.Builder()
                    .setTitle(carContext.getString(R.string.back_caps_action_title))
                    .setOnClickListener { screenManager.pop() }
                    .build()
            val header = Header.Builder().setStartHeaderAction(Action.BACK).build()
            return MessageTemplate.Builder(
                    "Tab styling requires Car API Level 9 or above. Current API level is " +
                        carContext.carAppApiLevel
                )
                .setHeader(header)
                .addAction(backAction)
                .build()
        }

        val activeProperty =
            mActiveContentId.toIntOrNull()?.let { TAB_PROPERTIES.getOrNull(it) }
                ?: throw IllegalStateException("Invalid tab id: $mActiveContentId")

        val builder =
            TabTemplate.Builder(
                object : TabCallback {
                    override fun onTabSelected(tabContentId: String) {
                        mActiveContentId = tabContentId
                        invalidate()
                    }
                }
            )
        buildTabs().forEach(builder::addTab)

        return builder
            .setActiveTabContentId(mActiveContentId)
            .setHeaderAction(Action.APP_ICON)
            .setTabContents(
                TabContents.Builder(createShortMessageTemplate(activeProperty.messageRes)).build()
            )
            .setStyle(tabTemplateStyle)
            .build()
    }

    private fun createShortMessageTemplate(@StringRes messageRes: Int): MessageTemplate {
        val action =
            Action.Builder()
                .setTitle(carContext.getString(R.string.back_caps_action_title))
                .setIcon(CarIcon.BACK)
                .setOnClickListener { screenManager.pop() }
                .build()
        return MessageTemplate.Builder(carContext.getString(messageRes))
            .setIcon(
                CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.test_android_media)
                    )
                    .setStyle(
                        CarIconStyle.Builder(CarIconStyle.ORIGINAL)
                            .setShape(Shape.CORNER_SMALL)
                            .build()
                    )
                    .build()
            )
            .addAction(action)
            .build()
    }
}
