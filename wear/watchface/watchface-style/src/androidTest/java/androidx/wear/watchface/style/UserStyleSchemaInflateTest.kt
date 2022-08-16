/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.IllegalNodeException
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.iterate
import androidx.wear.watchface.complications.moveToStart
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.test.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4::class)
@MediumTest
class UserStyleSchemaInflateTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @OptIn(ExperimentalHierarchicalStyle::class)
    @Test
    public fun test_inflate_list_schema() {
        val parser = context.resources.getXml(R.xml.list_schema)

        // Parse next until start tag is found
        parser.moveToStart("UserStyleSchema")

        val schema = UserStyleSchema.inflate(context.resources, parser, 1.0f, 1.0f)

        assertThat(schema.userStyleSettings.size).isEqualTo(3)
        val setting0 = schema.userStyleSettings[0] as UserStyleSetting.ListUserStyleSetting
        assertThat(setting0.id.value).isEqualTo("ColorStyle")
        assertThat(setting0.displayName).isEqualTo("Colors")
        assertThat(setting0.description).isEqualTo("Watchface colorization")
        assertThat(setting0.defaultOptionIndex).isEqualTo(1)
        assertThat(setting0.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        assertThat(setting0.icon!!.resId).isEqualTo(R.drawable.color_style_icon)
        assertThat(setting0.watchFaceEditorData!!.icon!!.resId)
            .isEqualTo(R.drawable.color_style_icon_wf)
        assertThat(setting0.options.size).isEqualTo(2)
        val option00 = (setting0.options[0] as ListOption)
        assertThat(option00.id).isEqualTo(UserStyleSetting.Option.Id("red"))
        assertThat(option00.displayName).isEqualTo("Red Style")
        assertThat(option00.icon!!.resId).isEqualTo(R.drawable.red_icon)
        assertThat(option00.childSettings).isEmpty()
        assertThat(option00.watchFaceEditorData!!.icon!!.resId).isEqualTo(R.drawable.red_icon_wf)
        val option01 = (setting0.options[1] as ListOption)
        assertThat(option01.id).isEqualTo(UserStyleSetting.Option.Id("green"))
        assertThat(option01.displayName).isEqualTo("Green Style")
        assertThat(option01.icon!!.resId).isEqualTo(R.drawable.green_icon)
        assertThat(option01.childSettings).isEmpty()
        assertThat(option01.watchFaceEditorData!!.icon!!.resId).isEqualTo(R.drawable.green_icon_wf)

        val setting1 = schema.userStyleSettings[1] as UserStyleSetting.ListUserStyleSetting
        assertThat(setting1.id.value).isEqualTo("Thing2")
        assertThat(setting1.displayName).isEqualTo("thing2")
        assertThat(setting1.description).isEqualTo("description2")
        assertThat(setting1.defaultOptionIndex).isEqualTo(0)
        assertThat(setting1.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        assertThat(setting1.icon).isNull()
        assertThat(setting1.watchFaceEditorData).isNull()
        assertThat(setting1.options.size).isEqualTo(2)
        val option10 = (setting1.options[0] as ListOption)
        assertThat(option10.id).isEqualTo(UserStyleSetting.Option.Id("foo"))
        assertThat(option10.displayName).isEqualTo("Foo")
        assertThat(option10.icon).isNull()
        assertThat(option10.childSettings).isEmpty()
        assertThat(option10.watchFaceEditorData).isNull()
        val option11 = (setting1.options[1] as ListOption)
        assertThat(option11.id).isEqualTo(UserStyleSetting.Option.Id("bar"))
        assertThat(option11.displayName).isEqualTo("Bar")
        assertThat(option11.icon).isNull()
        assertThat(option11.childSettings).isEmpty()
        assertThat(option11.watchFaceEditorData).isNull()

        val setting2 = schema.userStyleSettings[2] as UserStyleSetting.ListUserStyleSetting
        assertThat(setting2.id.value).isEqualTo("TopLevel")
        assertThat(setting2.displayName).isEqualTo("A or B")
        assertThat(setting2.description).isEqualTo("Choose one")
        assertThat(setting2.defaultOptionIndex).isEqualTo(0)
        assertThat(setting2.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        assertThat(setting1.icon).isNull()
        assertThat(setting1.options.size).isEqualTo(2)
        val option20 = (setting2.options[0] as ListOption)
        assertThat(option20.id).isEqualTo(UserStyleSetting.Option.Id("a"))
        assertThat(option20.displayName).isEqualTo("A")
        assertThat(option20.icon).isNull()
        assertThat(option20.childSettings).containsExactly(setting0)
        val option21 = (setting2.options[1] as ListOption)
        assertThat(option21.id).isEqualTo(UserStyleSetting.Option.Id("b"))
        assertThat(option21.displayName).isEqualTo("B")
        assertThat(option21.icon).isNull()
        assertThat(option21.childSettings).containsExactly(setting1)
        parser.close()
    }

    @Test
    public fun test_inflate_mixed_schema() {
        val parser = context.resources.getXml(R.xml.mixed_schema)

        // Parse next until start tag is found
        parser.moveToStart("UserStyleSchema")

        val schema = UserStyleSchema.inflate(context.resources, parser, 1.0f, 1.0f)

        assertThat(schema.userStyleSettings.size).isEqualTo(4)
        val setting0 = schema.userStyleSettings[0] as UserStyleSetting.BooleanUserStyleSetting
        assertThat(setting0.id.value).isEqualTo("HourPips")
        assertThat(setting0.displayName).isEqualTo("Hour Pips")
        assertThat(setting0.description).isEqualTo("Whether pips for hours should be drawn")
        assertThat(setting0.defaultOptionIndex).isEqualTo(1)
        assertThat(setting0.affectedWatchFaceLayers).containsExactly(WatchFaceLayer.BASE)
        assertThat(setting0.icon!!.resId).isEqualTo(R.drawable.color_style_icon)

        val setting1 =
            schema.userStyleSettings[1] as UserStyleSetting.ComplicationSlotsUserStyleSetting
        assertThat(setting1.id.value).isEqualTo("ComplicationsId")
        assertThat(setting1.displayName).isEqualTo("Complications")
        assertThat(setting1.description).isEqualTo("Controls complication layout")
        assertThat(setting1.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.COMPLICATIONS
        )
        assertThat(setting1.icon).isNull()
        assertThat(setting1.options.size).isEqualTo(3)
        val option10 = (setting1.options[0] as ComplicationSlotsOption)
        assertThat(option10.id).isEqualTo(UserStyleSetting.Option.Id("one"))
        assertThat(option10.displayName).isEqualTo("One complication")
        assertThat(option10.icon).isNull()
        assertThat(option10.complicationSlotOverlays.size).isEqualTo(3)
        val overlays10 = option10.complicationSlotOverlays.toTypedArray()
        assertThat(overlays10.size).isEqualTo(3)
        assertThat(overlays10[0].complicationSlotId).isEqualTo(1)
        assertThat(overlays10[0].complicationSlotId).isEqualTo(context.resources.getInteger(
            R.integer.complication_slot_id1
        ))
        assertThat(overlays10[0].enabled).isFalse()
        assertThat(overlays10[0].accessibilityTraversalIndex).isNull()
        assertThat(overlays10[0].complicationSlotBounds).isNull()
        assertThat(overlays10[1].complicationSlotId).isEqualTo(2)
        assertThat(overlays10[1].enabled).isTrue()
        assertThat(overlays10[1].accessibilityTraversalIndex).isNull()
        assertThat(overlays10[1].complicationSlotBounds).isNull()
        assertThat(overlays10[2].complicationSlotId).isEqualTo(3)
        assertThat(overlays10[2].enabled).isNull()
        assertThat(overlays10[2].accessibilityTraversalIndex).isEqualTo(100)
        assertThat(overlays10[2].complicationSlotBounds).isNull()
        val option11 = (setting1.options[1] as ComplicationSlotsOption)
        assertThat(option11.id).isEqualTo(UserStyleSetting.Option.Id("two"))
        assertThat(option11.displayName).isEqualTo("Move complication 2")
        assertThat(option11.icon).isNull()
        assertThat(option11.complicationSlotOverlays.size).isEqualTo(1)
        val overlays11 = option11.complicationSlotOverlays.toTypedArray()
        assertThat(overlays11[0].complicationSlotId).isEqualTo(2)
        assertThat(overlays11[0].enabled).isNull()
        assertThat(overlays11[0].accessibilityTraversalIndex).isNull()
        assertThat(
            overlays11[0].complicationSlotBounds!!.perComplicationTypeBounds[
                ComplicationType.LONG_TEXT
            ]
        ).isEqualTo(RectF(0.2f, 0.4f, 0.3f, 0.1f))

        val option12 = (setting1.options[2] as ComplicationSlotsOption)
        assertThat(option12.id).isEqualTo(UserStyleSetting.Option.Id("three"))
        assertThat(option12.displayName).isEqualTo("Resize complication 3")
        assertThat(option12.icon).isNull()
        assertThat(option12.complicationSlotOverlays.size).isEqualTo(1)
        val overlays12 = option12.complicationSlotOverlays.toTypedArray()
        assertThat(
            overlays12[0].complicationSlotBounds!!.perComplicationTypeBounds[
                ComplicationType.SHORT_TEXT
            ]
        ).isEqualTo(RectF(0.2f, 0.4f, 0.3f, 0.1f))
        assertThat(
            overlays12[0].complicationSlotBounds!!.perComplicationTypeBounds[
                ComplicationType.LONG_TEXT
            ]
        ).isEqualTo(RectF(0.6f, 0.8f, 0.7f, 0.5f))

        val setting2 = schema.userStyleSettings[2] as UserStyleSetting.DoubleRangeUserStyleSetting
        assertThat(setting2.id.value).isEqualTo("DoubleRange")
        assertThat(setting2.displayName).isEqualTo("Double range")
        assertThat(setting2.description).isEqualTo("Double range description")
        assertThat(setting2.defaultValue).isEqualTo(2.5)
        assertThat(setting2.minimumValue).isEqualTo(-1.5)
        assertThat(setting2.maximumValue).isEqualTo(10.5)
        assertThat(setting2.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS
        )
        assertThat(setting2.icon).isNull()

        val setting3 = schema.userStyleSettings[3] as UserStyleSetting.LongRangeUserStyleSetting
        assertThat(setting3.id.value).isEqualTo("LongRange")
        assertThat(setting3.displayName).isEqualTo("Long range")
        assertThat(setting3.description).isEqualTo("Long range description")
        assertThat(setting3.defaultValue).isEqualTo(2)
        assertThat(setting3.minimumValue).isEqualTo(-1)
        assertThat(setting3.maximumValue).isEqualTo(10)
        assertThat(setting3.affectedWatchFaceLayers).containsExactly(
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        assertThat(setting3.icon).isNull()

        parser.close()
    }

    @Test
    public fun test_inflate_schema_with_parent() {
        val parser1 = context.resources.getXml(R.xml.schema_with_parent_1)
        val parser2 = context.resources.getXml(R.xml.schema_with_parent_2)

        parser1.moveToStart("UserStyleSchema")
        parser2.moveToStart("UserStyleSchema")

        val schema1 = UserStyleSchema.inflate(context.resources, parser1, 1.0f, 1.0f)
        val schema2 = UserStyleSchema.inflate(context.resources, parser2, 1.0f, 1.0f)

        assertThat(schema1.userStyleSettings.size).isEqualTo(6)
        assertThat(schema2.userStyleSettings.size).isEqualTo(2)

        // List
        val simpleListWithParent1 = schema1.userStyleSettings[0]
            as UserStyleSetting.ListUserStyleSetting
        val simpleListWithParent2 = schema2.userStyleSettings[0]
            as UserStyleSetting.ListUserStyleSetting

        assertThat(simpleListWithParent1).isEqualTo(simpleListWithParent2)

        val listParser = context.resources.getXml(R.xml.list_setting_common)
        listParser.moveToStart("ListUserStyleSetting")

        val simpleListSetting = UserStyleSetting.ListUserStyleSetting.inflate(
            context.resources, listParser, emptyMap()
        )

        assertThat(simpleListWithParent1).isEqualTo(simpleListSetting)
        assertThat(simpleListSetting.id.value).isEqualTo(
            context.resources.getString(R.string.list_setting_common_id)
        )
        assertThat(simpleListSetting.options[0].id.toString()).isEqualTo(
            context.resources.getString(R.string.list_setting_common_option_red_id)
        )
        assertThat(simpleListSetting.options[1].id.toString()).isEqualTo(
            context.resources.getString(R.string.list_setting_common_option_green_id)
        )

        // Check override
        val listSetting1 = schema1.userStyleSettings[1] as UserStyleSetting.ListUserStyleSetting
        val listSetting2 = schema1.userStyleSettings[2] as UserStyleSetting.ListUserStyleSetting
        val listSetting3 = schema1.userStyleSettings[3] as UserStyleSetting.ListUserStyleSetting

        assertThat(listSetting1.id.value).isEqualTo("list_id0")
        assertThat(listSetting1.displayName).isEqualTo("id0")
        assertThat(listSetting2.id.value).isEqualTo("list_id1")
        assertThat(listSetting2.description).isEqualTo("id1")
        assertThat(listSetting3.id.value).isEqualTo("list_id2")
        assertThat(listSetting3.affectedWatchFaceLayers).containsExactly(WatchFaceLayer.BASE)

        assertThat(listSetting1.description).isEqualTo(simpleListSetting.description)
        assertThat(listSetting1.icon!!.resId).isEqualTo(simpleListSetting.icon!!.resId)
        assertThat(listSetting1.defaultOptionIndex).isEqualTo(simpleListSetting.defaultOptionIndex)

        // Double
        val simpleDoubleWithParent1 = schema1.userStyleSettings[4]
            as UserStyleSetting.DoubleRangeUserStyleSetting
        val simpleDoubleWithParent2 = schema2.userStyleSettings[1]
            as UserStyleSetting.DoubleRangeUserStyleSetting

        assertThat(simpleDoubleWithParent1).isEqualTo(simpleDoubleWithParent2)

        val doubleParser = context.resources.getXml(R.xml.double_setting_common)
        doubleParser.moveToStart("DoubleRangeUserStyleSetting")

        val simpleDoubleSetting = UserStyleSetting.DoubleRangeUserStyleSetting.inflate(
            context.resources, doubleParser
        )

        assertThat(simpleDoubleWithParent1).isEqualTo(simpleDoubleSetting)

        // Check override
        val doubleSetting1 = schema1.userStyleSettings[5]
            as UserStyleSetting.DoubleRangeUserStyleSetting

        assertThat(doubleSetting1.id.value).isEqualTo("double_id0")
        assertThat(doubleSetting1.defaultValue).isEqualTo(0.0)
        assertThat(doubleSetting1.maximumValue).isEqualTo(0.0)
        assertThat(doubleSetting1.minimumValue).isEqualTo(-1.0)

        assertThat(doubleSetting1.displayName).isEqualTo(simpleDoubleSetting.displayName)
        assertThat(doubleSetting1.affectedWatchFaceLayers).isEqualTo(
            simpleDoubleSetting.affectedWatchFaceLayers
        )

        doubleParser.close()
        listParser.close()
        parser1.close()
        parser2.close()
    }

    @Test
    public fun test_inflate_simple_flavor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.simple_flavor)

        // Parse next until start tag is found
        parser.moveToStart("XmlWatchFace")

        var schema: UserStyleSchema? = null
        var flavors: UserStyleFlavors? = null

        parser.iterate {
            when (parser.name) {
                "UserStyleSchema" ->
                    schema = UserStyleSchema.inflate(context.resources, parser, 1.0f, 1.0f)
                "UserStyleFlavors" ->
                    flavors = UserStyleFlavors.inflate(context.resources, parser, schema!!)
                else -> throw IllegalNodeException(parser)
            }
        }

        assertThat(flavors!!.flavors.size).isEqualTo(2)
        assertThat(flavors!!.flavors[0].style.userStyleMap.keys).containsExactly(
            context.getString(R.string.list_setting_common_id)
        )
    }
}
