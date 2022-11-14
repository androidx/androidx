/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.style

import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.CustomValueUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.CustomValueUserStyleSetting.CustomValueOption
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Assert.assertThrows
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private val redStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("red_style"), "Red", icon = null)

private val greenStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("green_style"), "Green", icon = null)

private val blueStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("blue_style"), "Blue", icon = null)

private val colorStyleList = listOf(redStyleOption, greenStyleOption, blueStyleOption)

private val colorStyleSetting = ListUserStyleSetting(
    UserStyleSetting.Id("color_style_setting"),
    "Colors",
    "Watchface colorization", /* icon = */
    null,
    colorStyleList,
    listOf(WatchFaceLayer.BASE)
)

private val classicStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("classic_style"), "Classic", icon = null)

private val modernStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("modern_style"), "Modern", icon = null)

private val gothicStyleOption =
    ListUserStyleSetting.ListOption(Option.Id("gothic_style"), "Gothic", icon = null)

private val watchHandStyleList =
    listOf(classicStyleOption, modernStyleOption, gothicStyleOption)

private val watchHandStyleSetting = ListUserStyleSetting(
    UserStyleSetting.Id("hand_style_setting"),
    "Hand Style",
    "Hand visual look", /* icon = */
    null,
    watchHandStyleList,
    listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
)
private val watchHandLengthStyleSetting =
    DoubleRangeUserStyleSetting(
        UserStyleSetting.Id("watch_hand_length_style_setting"),
        "Hand length",
        "Scale of watch hands",
        null,
        0.25,
        1.0,
        listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
        0.75
    )
private val booleanSetting = BooleanUserStyleSetting(
    UserStyleSetting.Id("setting"),
    "setting",
    "setting description",
    null,
    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
    true
)
private val booleanSettingCopy = BooleanUserStyleSetting(
    UserStyleSetting.Id("setting"),
    "setting",
    "setting description",
    null,
    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
    true
)
private val booleanSettingModifiedInfo = BooleanUserStyleSetting(
    UserStyleSetting.Id("setting"),
    "setting modified",
    "setting description modified",
    null,
    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
    false
)

private val booleanSettingModifiedId = BooleanUserStyleSetting(
    UserStyleSetting.Id("setting_modified"),
    "setting",
    "setting description",
    null,
    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
    true
)

private val optionTrue = BooleanUserStyleSetting.BooleanOption.TRUE
private val optionFalse = BooleanUserStyleSetting.BooleanOption.FALSE

@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
@RunWith(StyleTestRunner::class)
class CurrentUserStyleRepositoryTest {

    private val userStyleRepository =
        CurrentUserStyleRepository(
            UserStyleSchema(
                listOf(colorStyleSetting, watchHandStyleSetting, watchHandLengthStyleSetting)
            )
        )

    @Test
    fun assigning_userStyle() {
        val newStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to greenStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        userStyleRepository.updateUserStyle(newStyle)

        assertThat(userStyleRepository.userStyle.value[colorStyleSetting])
            .isEqualTo(greenStyleOption)
        assertThat(userStyleRepository.userStyle.value[watchHandStyleSetting])
            .isEqualTo(gothicStyleOption)
    }

    @Test
    fun assign_userStyle_with_distinctButMatchingRefs() {
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            colorStyleList,
            listOf(WatchFaceLayer.BASE)
        )
        val watchHandStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("hand_style_setting"),
            "Hand Style",
            "Hand visual look", /* icon = */
            null,
            watchHandStyleList,
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )

        val newStyle = UserStyle(
            hashMapOf(
                colorStyleSetting2 to greenStyleOption,
                watchHandStyleSetting2 to gothicStyleOption
            )
        )

        userStyleRepository.updateUserStyle(newStyle)

        assertThat(userStyleRepository.userStyle.value[colorStyleSetting])
            .isEqualTo(greenStyleOption)
        assertThat(userStyleRepository.userStyle.value[watchHandStyleSetting])
            .isEqualTo(gothicStyleOption)
    }

    @Test
    fun defaultValues() {
        val watchHandLengthOption =
            userStyleRepository.userStyle.value[watchHandLengthStyleSetting]!! as
                DoubleRangeUserStyleSetting.DoubleRangeOption
        assertThat(watchHandLengthOption.value).isEqualTo(0.75)
    }

    @Test
    fun userStyle_mapConstructor() {
        val userStyle = UserStyle(
            UserStyleData(
                mapOf(
                    "color_style_setting" to "blue_style".encodeToByteArray(),
                    "hand_style_setting" to "gothic_style".encodeToByteArray()
                )
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle[colorStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("blue_style")
        assertThat(userStyle[watchHandStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("gothic_style")
    }

    @Test
    fun userStyle_mapConstructor_badColorStyle() {
        val userStyle = UserStyle(
            UserStyleData(
                mapOf(
                    "color_style_setting" to "I DO NOT EXIST".encodeToByteArray(),
                    "hand_style_setting" to "gothic_style".encodeToByteArray()
                )
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle[colorStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("red_style")
        assertThat(userStyle[watchHandStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("gothic_style")
    }

    @Test
    fun userStyle_mapConstructor_missingColorStyle() {
        val userStyle = UserStyle(
            UserStyleData(
                mapOf("hand_style_setting" to "gothic_style".encodeToByteArray())
            ),
            userStyleRepository.schema
        )

        assertThat(userStyle[colorStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("red_style")
        assertThat(userStyle[watchHandStyleSetting]!!.id.value.decodeToString())
            .isEqualTo("gothic_style")
    }

    @Test
    fun userStyle_mapConstructor_customValueUserStyleSetting() {
        val customStyleSetting = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        val userStyleRepository = CurrentUserStyleRepository(
            UserStyleSchema(
                listOf(customStyleSetting)
            )
        )

        val userStyle = UserStyle(
            UserStyleData(
                mapOf(
                    CustomValueUserStyleSetting.CUSTOM_VALUE_USER_STYLE_SETTING_ID
                        to "TEST 123".encodeToByteArray()
                )
            ),
            userStyleRepository.schema
        )

        val customValue = userStyle[customStyleSetting]!! as CustomValueOption
        assertThat(customValue.customValue.decodeToString()).isEqualTo("TEST 123")
    }

    @Test
    fun userStyle_multiple_CustomValueUserStyleSetting_notAllowed() {
        val customStyleSetting1 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )
        val customStyleSetting2 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        try {
            UserStyleSchema(
                listOf(customStyleSetting1, customStyleSetting2)
            )
            fail(
                "Constructing a UserStyleSchema with more than one " +
                    "CustomValueUserStyleSetting should fail"
            )
        } catch (e: Exception) {
            // expected
        }
    }

    @Test
    fun userStyle_multiple_CustomValueUserStyleSettings_notAllowed() {
        val customStyleSetting1 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )
        val customStyleSetting2 = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        try {
            UserStyleSchema(
                listOf(customStyleSetting1, customStyleSetting2)
            )
            fail(
                "Constructing a UserStyleSchema with more than one CustomValueUserStyleSetting " +
                    "should fail"
            )
        } catch (e: Exception) {
            // expected
        }
    }

    @Test
    fun userStyle_get() {
        val userStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to greenStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        assertThat(userStyle[colorStyleSetting]).isEqualTo(greenStyleOption)
        assertThat(userStyle[watchHandStyleSetting]).isEqualTo(gothicStyleOption)
    }

    @Test
    fun userStyle_getById() {
        val userStyle = UserStyle(
            hashMapOf(
                colorStyleSetting to greenStyleOption,
                watchHandStyleSetting to gothicStyleOption
            )
        )

        assertThat(userStyle[colorStyleSetting.id]).isEqualTo(greenStyleOption)
        assertThat(userStyle[watchHandStyleSetting.id]).isEqualTo(gothicStyleOption)
    }

    @Test
    fun userStyle_getAndSetDifferentOptionInstances() {
        val option0 = ListUserStyleSetting.ListOption(
            Option.Id("0"),
            "option 0",
            icon = null
        )
        val option1 = ListUserStyleSetting.ListOption(
            Option.Id("1"),
            "option 1",
            icon = null
        )
        val option0Copy = ListUserStyleSetting.ListOption(
            Option.Id("0"),
            "option #0",
            icon = null
        )
        val option1Copy = ListUserStyleSetting.ListOption(
            Option.Id("1"),
            "option #1",
            icon = null
        )
        val setting = ListUserStyleSetting(
            UserStyleSetting.Id("setting"),
            "setting",
            "setting description",
            icon = null,
            listOf(option0, option1),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val userStyle = UserStyle(mapOf(setting to option0))
        assertThat(userStyle[setting]).isEqualTo(option0)
        assertThat(userStyle[setting]).isEqualTo(option0Copy)

        userStyle.toMutableUserStyle().apply {
            this[setting] = option1
        }.let { newUserStyle ->
            assertThat(userStyle[setting]).isEqualTo(option0)
            assertThat(newUserStyle[setting]).isEqualTo(option1)
            assertThat(newUserStyle[setting]).isEqualTo(option1Copy)
        }
    }

    @Test
    fun userStyle_getDifferentSettingInstances() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
        assertThat(userStyle[booleanSettingCopy]).isEqualTo(optionTrue)
        assertThat(userStyle[booleanSettingModifiedInfo]).isEqualTo(optionTrue)
        assertThat(userStyle[booleanSettingModifiedId]).isEqualTo(null)
    }

    @Test
    fun userStyle_setDifferentSettingInstances() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)

        userStyle.toMutableUserStyle().apply {
            this[booleanSetting] = optionFalse
        }.toUserStyle().let { newUserStyle: UserStyle ->
            assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
            assertThat(newUserStyle[booleanSetting]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingCopy]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedId]).isEqualTo(null)
        }

        userStyle.toMutableUserStyle().apply {
            this[booleanSettingCopy] = optionFalse
        }.toUserStyle().let { newUserStyle: UserStyle ->
            assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
            assertThat(newUserStyle[booleanSetting]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingCopy]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedId]).isEqualTo(null)
        }

        userStyle.toMutableUserStyle().apply {
            this[booleanSettingModifiedInfo] = optionFalse
        }.toUserStyle().let { newUserStyle: UserStyle ->
            assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
            assertThat(newUserStyle[booleanSetting]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingCopy]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
            assertThat(newUserStyle[booleanSettingModifiedId]).isEqualTo(null)
        }

        assertThrows(IllegalArgumentException::class.java) {
            userStyle.toMutableUserStyle().apply {
                this[booleanSettingModifiedId] = optionFalse
            }
        }
    }

    @Test
    fun currentUserStyleRepository_setUserStyleWithDifferentSettingInstances() {
        val userStyleSchema = UserStyleSchema(listOf(booleanSetting))

        CurrentUserStyleRepository(userStyleSchema).let { currentUserStyleRepository ->
            currentUserStyleRepository.updateUserStyle(
                UserStyle(mapOf(booleanSetting to optionFalse))
            )
            currentUserStyleRepository.userStyle.value.let { userStyle ->
                assertThat(userStyle[booleanSetting]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingCopy]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedId]).isEqualTo(null)
            }
        }

        CurrentUserStyleRepository(userStyleSchema).let { currentUserStyleRepository ->
            currentUserStyleRepository.updateUserStyle(
                UserStyle(mapOf(booleanSettingCopy to optionFalse))
            )
            currentUserStyleRepository.userStyle.value.let { userStyle ->
                assertThat(userStyle[booleanSetting]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingCopy]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedId]).isEqualTo(null)
            }
        }

        CurrentUserStyleRepository(userStyleSchema).let { currentUserStyleRepository ->
            currentUserStyleRepository.updateUserStyle(
                UserStyle(mapOf(booleanSettingModifiedInfo to optionFalse))
            )
            currentUserStyleRepository.userStyle.value.let { userStyle ->
                assertThat(userStyle[booleanSetting]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingCopy]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedInfo]).isEqualTo(optionFalse)
                assertThat(userStyle[booleanSettingModifiedId]).isEqualTo(null)
            }
        }

        CurrentUserStyleRepository(userStyleSchema).let { currentUserStyleRepository ->
            assertThrows(
                java.lang.IllegalArgumentException::class.java
            ) {
                currentUserStyleRepository.updateUserStyle(
                    UserStyle(mapOf(booleanSettingModifiedId to optionFalse))
                )
            }
        }
    }

    @Test
    fun userStyle_modifyUnderlyingMap() {
        val map = HashMap<UserStyleSetting, Option>().apply {
            this[booleanSetting] = optionTrue
        }
        val userStyle = UserStyle(map)

        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
        map[booleanSetting] = optionFalse
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
    }

    @Test
    fun userStyle_merge_overridesOneSetting() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        UserStyle.merge(userStyle, UserStyle(mapOf(booleanSetting to optionFalse))).let {
            // Style has changed.
            assertThat(it).isNotNull()
            // Now points to false.
            assertThat(it!![booleanSetting]).isEqualTo(optionFalse)
        }
    }

    @Test
    fun userStyle_merge_overridesWithSameValue() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        UserStyle.merge(userStyle, UserStyle(mapOf(booleanSetting to optionTrue))).let {
            // Style has not changed.
            assertThat(it).isNull()
        }
        // Still points to true.
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
    }

    @Test
    fun userStyle_merge_overridesUnknownSetting() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        UserStyle.merge(
            userStyle,
            UserStyle(mapOf(colorStyleSetting to blueStyleOption))
        ).let {
            // Style has not changed.
            assertThat(it).isNull()
        }
        // Still points to true.
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
        // Unknown setting was not added.
        assertThat(userStyle[colorStyleSetting]).isEqualTo(null)
    }

    @Test
    fun userStyle_merge_overridesEmpty() {
        val userStyle = UserStyle(mapOf(booleanSetting to optionTrue))
        // Override nothing.
        UserStyle.merge(userStyle, UserStyle(mapOf())).let {
            // Style has not changed.
            assertThat(it).isNull()
        }
        // Still points to true.
        assertThat(userStyle[booleanSetting]).isEqualTo(optionTrue)
    }

    @Test
    fun setAndGetCustomStyleSetting() {
        val customStyleSetting = CustomValueUserStyleSetting(
            listOf(WatchFaceLayer.BASE),
            "default".encodeToByteArray()
        )

        val userStyleRepository = CurrentUserStyleRepository(
            UserStyleSchema(
                listOf(customStyleSetting)
            )
        )

        userStyleRepository.updateUserStyle(
            UserStyle(
                mapOf(
                    customStyleSetting to CustomValueOption("test".encodeToByteArray())
                )
            )
        )

        assertThat(
            (userStyleRepository.userStyle.value[customStyleSetting]!! as CustomValueOption)
                .customValue.decodeToString()
        ).isEqualTo("test")
    }

    @Test
    fun userStyleData_equals() {
        assertThat(UserStyleData(mapOf("A" to "a".encodeToByteArray()))).isEqualTo(
            UserStyleData(mapOf("A" to "a".encodeToByteArray()))
        )

        assertThat(
            UserStyleData(mapOf("A" to "a".encodeToByteArray(), "B" to "b".encodeToByteArray()))
        ).isEqualTo(
            UserStyleData(mapOf("A" to "a".encodeToByteArray(), "B" to "b".encodeToByteArray()))
        )

        assertThat(UserStyleData(mapOf("A" to "a".encodeToByteArray()))).isNotEqualTo(
            UserStyleData(mapOf("A" to "b".encodeToByteArray()))
        )

        assertThat(UserStyleData(mapOf("A" to "a".encodeToByteArray()))).isNotEqualTo(
            UserStyleData(mapOf("B" to "a".encodeToByteArray()))
        )

        assertThat(UserStyleData(mapOf("A" to "a".encodeToByteArray()))).isNotEqualTo(
            UserStyleData(mapOf("A" to "a".encodeToByteArray(), "B" to "b".encodeToByteArray()))
        )

        assertThat(
            UserStyleData(mapOf("A" to "a".encodeToByteArray(), "B" to "b".encodeToByteArray()))
        ).isNotEqualTo(UserStyleData(mapOf("A" to "a".encodeToByteArray())))
    }

    @Test
    fun userStyleData_toString() {
        val userStyleData = UserStyleData(
            mapOf(
                "A" to "a".encodeToByteArray(),
                "B" to "b".encodeToByteArray()
            )
        )

        assertThat(userStyleData.toString()).contains("A=a")
        assertThat(userStyleData.toString()).contains("B=b")
    }

    @Test
    fun optionIdToStringTest() {
        assertThat(optionTrue.toString()).isEqualTo("true")
        assertThat(optionFalse.toString()).isEqualTo("false")
        assertThat(gothicStyleOption.toString()).isEqualTo("gothic_style")
        assertThat(DoubleRangeUserStyleSetting.DoubleRangeOption(12.3).toString())
            .isEqualTo("12.3")
        assertThat(LongRangeUserStyleSetting.LongRangeOption(123).toString())
            .isEqualTo("123")
        assertThat(CustomValueOption("test".encodeToByteArray()).toString()).isEqualTo("test")
    }

    @Test
    fun userStyleEquality() {
        val userStyleA = UserStyle(
            mapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to modernStyleOption,
                watchHandLengthStyleSetting to DoubleRangeUserStyleSetting.DoubleRangeOption(0.2)
            )
        )
        val userStyleA2 = UserStyle(
            mapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to modernStyleOption,
                watchHandLengthStyleSetting to DoubleRangeUserStyleSetting.DoubleRangeOption(0.2)
            )
        )

        val userStyleB = UserStyle(
            mapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to modernStyleOption,
                watchHandLengthStyleSetting to DoubleRangeUserStyleSetting.DoubleRangeOption(0.75)
            )
        )
        val userStyleC = UserStyle(
            mapOf(
                colorStyleSetting to blueStyleOption,
                watchHandStyleSetting to gothicStyleOption,
                watchHandLengthStyleSetting to DoubleRangeUserStyleSetting.DoubleRangeOption(0.2)
            )
        )
        val userStyleD = UserStyle(
            mapOf(
                colorStyleSetting to redStyleOption,
                watchHandStyleSetting to modernStyleOption,
                watchHandLengthStyleSetting to DoubleRangeUserStyleSetting.DoubleRangeOption(0.2)
            )
        )

        assertThat(userStyleA).isEqualTo(userStyleA2)
        assertThat(userStyleA).isNotEqualTo(userStyleB)
        assertThat(userStyleA).isNotEqualTo(userStyleC)
        assertThat(userStyleA).isNotEqualTo(userStyleD)
        assertThat(userStyleB).isNotEqualTo(userStyleC)
        assertThat(userStyleB).isNotEqualTo(userStyleD)
    }

    @Test
    fun hierarchicalStyle() {
        val twelveHourClockOption =
            ListUserStyleSetting.ListOption(Option.Id("12_style"), "12", icon = null)

        val twentyFourHourClockOption =
            ListUserStyleSetting.ListOption(Option.Id("24_style"), "24", icon = null)

        val digitalClockStyleSetting = ListUserStyleSetting(
            UserStyleSetting.Id("digital_clock_style"),
            "Clock style",
            "Clock style setting",
            null,
            listOf(twelveHourClockOption, twentyFourHourClockOption),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val digitalWatchFaceType = ListUserStyleSetting.ListOption(
            Option.Id("digital"),
           "Digital",
            icon = null,
            childSettings = listOf(digitalClockStyleSetting, colorStyleSetting)
        )

        val analogWatchFaceType = ListUserStyleSetting.ListOption(
            Option.Id("analog"),
            "Analog",
            icon = null,
            childSettings = listOf(watchHandLengthStyleSetting, watchHandStyleSetting)
        )

        val watchFaceType = ListUserStyleSetting(
            UserStyleSetting.Id("clock_type"),
            "Watch face type",
            "Analog or digital",
            icon = null,
            options = listOf(digitalWatchFaceType, analogWatchFaceType),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val schema = UserStyleSchema(
            listOf(
                watchFaceType,
                digitalClockStyleSetting,
                colorStyleSetting,
                watchHandLengthStyleSetting,
                watchHandStyleSetting
            )
        )

        assertThat(schema.rootUserStyleSettings).containsExactly(watchFaceType)

        assertThat(watchFaceType.hasParent).isFalse()
        assertThat(digitalClockStyleSetting.hasParent).isTrue()
        assertThat(colorStyleSetting.hasParent).isTrue()
        assertThat(watchHandLengthStyleSetting.hasParent).isTrue()
        assertThat(watchHandStyleSetting.hasParent).isTrue()
    }

    @Test
    fun invalid_multiple_ComplicationSlotsUserStyleSettings_same_level() {
        val leftAndRightComplications = ComplicationSlotsOption(
            Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
            displayName = "Both",
            icon = null,
            emptyList()
        )
        val complicationSetting1 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting1"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftAndRightComplications),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )
        val complicationSetting2 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting2"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftAndRightComplications),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )
        val optionA1 = ListUserStyleSetting.ListOption(
            Option.Id("a1_style"),
            displayName = "A1",
            icon = null,
            childSettings = listOf(complicationSetting1, complicationSetting2)
        )
        val optionA2 = ListUserStyleSetting.ListOption(
            Option.Id("a2_style"),
            displayName = "A2",
            icon = null,
            childSettings = listOf(complicationSetting2)
        )

        assertThrows(IllegalArgumentException::class.java) {
            UserStyleSchema(
                listOf(
                    ListUserStyleSetting(
                        UserStyleSetting.Id("a123"),
                        displayName = "A123",
                        description = "A123",
                        icon = null,
                        listOf(optionA1, optionA2),
                        WatchFaceLayer.ALL_WATCH_FACE_LAYERS
                    ),
                    complicationSetting1,
                    complicationSetting2
                )
            )
        }
    }

    @Test
    fun invalid_multiple_ComplicationSlotsUserStyleSettings_different_levels() {
        val leftAndRightComplications = ComplicationSlotsOption(
            Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
            displayName = "Both",
            icon = null,
            emptyList()
        )
        val complicationSetting1 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting1"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftAndRightComplications),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )
        val complicationSetting2 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting2"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftAndRightComplications),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )
        val optionA1 = ListUserStyleSetting.ListOption(
            Option.Id("a1_style"),
            displayName = "A1",
            icon = null,
            childSettings = listOf(complicationSetting1)
        )
        val optionA2 = ListUserStyleSetting.ListOption(
            Option.Id("a2_style"),
            displayName = "A2",
            icon = null
        )

        assertThrows(IllegalArgumentException::class.java) {
            UserStyleSchema(
                listOf(
                    ListUserStyleSetting(
                        UserStyleSetting.Id("a123"),
                        displayName = "A123",
                        description = "A123",
                        icon = null,
                        listOf(optionA1, optionA2),
                        WatchFaceLayer.ALL_WATCH_FACE_LAYERS
                    ),
                    complicationSetting1,
                    complicationSetting2
                )
            )
        }
    }

    @Test
    @Suppress("deprecation")
    fun multiple_ComplicationSlotsUserStyleSettings() {
        // The code below constructs the following hierarchy:
        //
        //                                  rootABChoice
        //          rootOptionA   ---------/            \---------  rootOptionB
        //               |                                               |
        //          a123Choice                                       b12Choice
        //         /    |     \                                      /      \
        // optionA1  optionA2  optionA3                        optionB1    optionB2
        //   |          |                                         |
        //   |      complicationSetting2                 complicationSetting1
        // complicationSetting1

        val leftComplicationID = 101
        val rightComplicationID = 102
        val leftAndRightComplications = ComplicationSlotsOption(
            Option.Id("LEFT_AND_RIGHT_COMPLICATIONS"),
            displayName = "Both",
            icon = null,
            emptyList()
        )
        val noComplications = ComplicationSlotsOption(
            Option.Id("NO_COMPLICATIONS"),
            displayName = "None",
            icon = null,
            listOf(
                ComplicationSlotOverlay(leftComplicationID, enabled = false),
                ComplicationSlotOverlay(rightComplicationID, enabled = false)
            )
        )
        val complicationSetting1 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftAndRightComplications, noComplications),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

        val leftComplication = ComplicationSlotsOption(
            Option.Id("LEFT_COMPLICATION"),
            displayName = "Left",
            icon = null,
            listOf(ComplicationSlotOverlay(rightComplicationID, enabled = false))
        )
        val rightComplication = ComplicationSlotsOption(
            Option.Id("RIGHT_COMPLICATION"),
            displayName = "Right",
            icon = null,
            listOf(ComplicationSlotOverlay(leftComplicationID, enabled = false))
        )
        val complicationSetting2 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting2"),
            displayName = "Complications",
            description = "Number and position",
            icon = null,
            complicationConfig = listOf(leftComplication, rightComplication),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

        val normal = ComplicationSlotsOption(
            Option.Id("Normal"),
            displayName = "Normal",
            icon = null,
            emptyList()
        )
        val traversal = ComplicationSlotsOption(
            Option.Id("Traversal"),
            displayName = "Traversal",
            icon = null,
            listOf(
                ComplicationSlotOverlay(leftComplicationID, accessibilityTraversalIndex = 3),
                ComplicationSlotOverlay(rightComplicationID, accessibilityTraversalIndex = 2)
            )
        )
        val complicationSetting3 = ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("complications_style_setting3"),
            displayName = "Traversal Order",
            description = "Traversal Order",
            icon = null,
            complicationConfig = listOf(normal, traversal),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )

        val optionA1 = ListUserStyleSetting.ListOption(
            Option.Id("a1_style"),
            displayName = "A1",
            icon = null,
            childSettings = listOf(complicationSetting1)
        )
        val optionA2 = ListUserStyleSetting.ListOption(
            Option.Id("a2_style"),
            displayName = "A2",
            icon = null,
            childSettings = listOf(complicationSetting2)
        )
        val optionA3 =
            ListUserStyleSetting.ListOption(Option.Id("a3_style"), "A3", icon = null)

        val a123Choice = ListUserStyleSetting(
            UserStyleSetting.Id("a123"),
            displayName = "A123",
            description = "A123",
            icon = null,
            listOf(optionA1, optionA2, optionA3),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val optionB1 = ListUserStyleSetting.ListOption(
            Option.Id("b1_style"),
            displayName = "B1",
            icon = null,
            childSettings = listOf(complicationSetting3)
        )
        val optionB2 =
            ListUserStyleSetting.ListOption(Option.Id("b2_style"), "B2", icon = null)

        val b12Choice = ListUserStyleSetting(
            UserStyleSetting.Id("b12"),
            displayName = "B12",
            "B12",
            icon = null,
            listOf(optionB1, optionB2),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val rootOptionA = ListUserStyleSetting.ListOption(
            Option.Id("a_style"),
            displayName = "A",
            icon = null,
            childSettings = listOf(a123Choice)
        )
        val rootOptionB = ListUserStyleSetting.ListOption(
            Option.Id("b_style"),
            displayName = "B",
            icon = null,
            childSettings = listOf(b12Choice)
        )

        val rootABChoice = ListUserStyleSetting(
            UserStyleSetting.Id("root_ab"),
            displayName = "AB",
            description = "AB",
            icon = null,
            listOf(rootOptionA, rootOptionB),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )

        val schema = UserStyleSchema(
            listOf(
                rootABChoice,
                a123Choice,
                b12Choice,
                complicationSetting1,
                complicationSetting2,
                complicationSetting3
            )
        )

        val userStyleMap = mutableMapOf(
            rootABChoice to rootOptionA,
            a123Choice to optionA1,
            b12Choice to optionB1,
            complicationSetting1 to leftAndRightComplications,
            complicationSetting2 to rightComplication,
            complicationSetting3 to traversal
        )

        // Test various userStyleMap permutations to ensure the correct ComplicationSlotsOption is
        // returned.
        assertThat(schema.findComplicationSlotsOptionForUserStyle(UserStyle(userStyleMap)))
            .isEqualTo(leftAndRightComplications)

        userStyleMap[a123Choice] = optionA2
        assertThat(schema.findComplicationSlotsOptionForUserStyle(UserStyle(userStyleMap)))
            .isEqualTo(rightComplication)

        userStyleMap[a123Choice] = optionA3
        assertThat(schema.findComplicationSlotsOptionForUserStyle(UserStyle(userStyleMap)))
            .isNull()

        userStyleMap[rootABChoice] = rootOptionB
        assertThat(schema.findComplicationSlotsOptionForUserStyle(UserStyle(userStyleMap)))
            .isEqualTo(traversal)

        userStyleMap[b12Choice] = optionB2
        assertThat(schema.findComplicationSlotsOptionForUserStyle(UserStyle(userStyleMap)))
            .isNull()
    }
}

@RunWith(StyleTestRunner::class)
public class MutableUserStyleTest {

    @Test
    public fun cantAssignUnknownSetting() {
        val mutableUserStyle = UserStyle(mapOf(booleanSetting to optionTrue)).toMutableUserStyle()

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[colorStyleSetting] = optionFalse
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[colorStyleSetting] = optionFalse.id
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[colorStyleSetting.id] = optionFalse
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[colorStyleSetting.id] = optionFalse.id
        }
    }

    @Test
    public fun cantAssignUnrelatedOption() {
        val mutableUserStyle = UserStyle(mapOf(booleanSetting to optionTrue)).toMutableUserStyle()

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[booleanSetting] = blueStyleOption
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[booleanSetting.id] = blueStyleOption
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[booleanSetting] = blueStyleOption.id
        }

        assertThrows(IllegalArgumentException::class.java) {
            mutableUserStyle[booleanSetting.id] = blueStyleOption.id
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@RunWith(StyleTestRunner::class)
class DigestHashTest {
    @Test
    public fun digestHashSensitiveToSettingChanges() {
        val schema1 = UserStyleSchema(listOf(colorStyleSetting))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting, watchHandStyleSetting))
        val schema3 = UserStyleSchema(
            listOf(colorStyleSetting, watchHandStyleSetting, watchHandLengthStyleSetting)
        )

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()
        val digestHash3 = schema3.getDigestHash()

        assertThat(digestHash1).isNotEqualTo(digestHash2)
        assertThat(digestHash1).isNotEqualTo(digestHash3)
        assertThat(digestHash2).isNotEqualTo(digestHash3)
    }

    @Test
    public fun digestHashSensitiveToOptionChanges() {
        val colorStyleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption, greenStyleOption,),
            listOf(WatchFaceLayer.BASE)
        )
        val colorStyleSetting3 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption, greenStyleOption, blueStyleOption),
            listOf(WatchFaceLayer.BASE)
        )

        val schema1 = UserStyleSchema(listOf(colorStyleSetting1))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting2))
        val schema3 = UserStyleSchema(listOf(colorStyleSetting3))

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()
        val digestHash3 = schema3.getDigestHash()

        assertThat(digestHash1).isNotEqualTo(digestHash2)
        assertThat(digestHash1).isNotEqualTo(digestHash3)
        assertThat(digestHash2).isNotEqualTo(digestHash3)
    }

    @Test
    public fun digestHashSensitiveToDisplayNameChanges() {
        val colorStyleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors2",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption, greenStyleOption,),
            listOf(WatchFaceLayer.BASE)
        )

        val schema1 = UserStyleSchema(listOf(colorStyleSetting1))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting2))

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()

        assertThat(digestHash1).isNotEqualTo(digestHash2)
    }

    @Test
    public fun digestHashSensitiveToDescriptionChanges() {
        val colorStyleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization2", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )

        val schema1 = UserStyleSchema(listOf(colorStyleSetting1))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting2))

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()

        assertThat(digestHash1).isNotEqualTo(digestHash2)
    }

    @Ignore // b/238635208
    @Test
    public fun digestHashSensitiveToIconChanges() {
        val colorStyleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            Icon.createWithContentUri("/path1"),
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            Icon.createWithContentUri("/path2"),
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE)
        )

        val schema1 = UserStyleSchema(listOf(colorStyleSetting1))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting2))

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()

        assertThat(digestHash1).isNotEqualTo(digestHash2)
    }

    @Test
    public fun digestHashInsensitiveToLayersOrderChanges() {
        val colorStyleSetting1 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS)
        )
        val colorStyleSetting2 = ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization", /* icon = */
            null,
            listOf(redStyleOption),
            listOf(WatchFaceLayer.COMPLICATIONS, WatchFaceLayer.BASE)
        )

        val schema1 = UserStyleSchema(listOf(colorStyleSetting1))
        val schema2 = UserStyleSchema(listOf(colorStyleSetting2))

        val digestHash1 = schema1.getDigestHash()
        val digestHash2 = schema2.getDigestHash()

        assertThat(digestHash1).isEqualTo(digestHash2)
    }
}
