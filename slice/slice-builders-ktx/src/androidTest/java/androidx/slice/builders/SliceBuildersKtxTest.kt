/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.builders

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.SliceProvider
import androidx.slice.SliceSpecs
import androidx.slice.builders.ListBuilder.ICON_IMAGE
import androidx.slice.builders.ktx.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test

@SdkSuppress(minSdkVersion = 19)
@MediumTest
class SliceBuildersKtxTest {
    private val testUri = Uri.parse("content://com.example.android.sliceuri")
    private val context = ApplicationProvider.getApplicationContext() as android.content.Context

    init {
        SliceProvider.setSpecs(setOf(SliceSpecs.LIST))
        SliceProvider.setClock { 0 }
    }

    private fun pendingIntentToTestActivity() =
        PendingIntent.getActivity(
            context, 0, Intent(context, TestActivity::class.java), FLAG_IMMUTABLE
        )

    private val accentColor = 0xff3949

    private fun createIcon(id: Int) =
        IconCompat.createWithResource(context, id)

    @Test
    fun helloWorldSlice() {
        val sliceKtx = list(context = context, uri = testUri, ttl = ListBuilder.INFINITY) {
            header {
                setTitle("Hello World")
            }
        }

        val slice = ListBuilder(context, testUri, ListBuilder.INFINITY).setHeader(
            ListBuilder.HeaderBuilder().apply {
                setTitle("Hello World")
            }
        ).build()

        // Compare toString()s because Slice.equals is not implemented.
        assertEquals(sliceKtx.toString(), slice.toString())
    }

    // Temporarily disabled due to b/116146018.
    // @Test
    fun allBuildersTogether() {
        val pendingIntent = pendingIntentToTestActivity()
        val tapAction = tapSliceAction(
            pendingIntentToTestActivity(),
            createIcon(R.drawable.ic_android_black_24dp),
            ListBuilder.SMALL_IMAGE,
            "Title"
        )
        val sliceKtx = list(context = context, uri = testUri, ttl = ListBuilder.INFINITY) {
            header { setPrimaryAction(tapAction) }
            row { }
            gridRow { cell { } }
            inputRange { setInputAction(pendingIntent) }
            range { }
        }

        val slice = ListBuilder(context, testUri, ListBuilder.INFINITY).apply {
            setHeader(ListBuilder.HeaderBuilder().setPrimaryAction(tapAction))
            addRow(ListBuilder.RowBuilder())
            addGridRow(GridRowBuilder().addCell(GridRowBuilder.CellBuilder()))
            addInputRange(ListBuilder.InputRangeBuilder().setInputAction(pendingIntent))
            addRange(ListBuilder.RangeBuilder())
        }.build()
        assertEquals(slice.toString(), sliceKtx.toString())
    }

    // Temporarily disabled due to b/116146018.
    // @Test
    fun sanity_withGridRow() {
        val tapAction = tapSliceAction(
            pendingIntentToTestActivity(),
            createIcon(R.drawable.ic_android_black_24dp),
            ListBuilder.SMALL_IMAGE,
            "Title"
        )
        val toggleAction = toggleSliceAction(
            pendingIntentToTestActivity(),
            createIcon(R.drawable.ic_android_black_24dp),
            "Title",
            false
        )
        val titleText1 = "cell title text 1"
        val titleText2 = "cell title text 2"

        val text1 = "cell text 1"
        val text2 = "cell text 2"
        val sliceKtx = list(context = context, uri = testUri, ttl = ListBuilder.INFINITY) {
            gridRow {
                setPrimaryAction(tapAction)
                cell {
                    addTitleText(titleText1)
                    addImage(createIcon(R.drawable.ic_android_black_24dp), ListBuilder.SMALL_IMAGE)
                    addText(text1)
                }
                cell {
                    addTitleText(titleText2)
                    addImage(createIcon(R.drawable.ic_android_black_24dp), ListBuilder.SMALL_IMAGE)
                    addText(text2)
                }
            }
            addAction(toggleAction)
        }

        val slice = ListBuilder(context, testUri, ListBuilder.INFINITY).addGridRow(
            GridRowBuilder().apply {
                setPrimaryAction(tapAction)
                addCell(
                    GridRowBuilder.CellBuilder().apply {
                        addTitleText(titleText1)
                        addImage(
                            createIcon(R.drawable.ic_android_black_24dp),
                            ListBuilder.SMALL_IMAGE
                        )
                        addText(text1)
                    }
                )
                addCell(
                    GridRowBuilder.CellBuilder().apply {
                        addTitleText(titleText2)
                        addImage(
                            createIcon(R.drawable.ic_android_black_24dp),
                            ListBuilder.SMALL_IMAGE
                        )
                        addText(text2)
                    }
                )
            }
        ).addAction(toggleAction).build()

        // Compare toString()s because Slice.equals is not implemented.
        assertEquals(sliceKtx.toString(), slice.toString())
    }

    @Test
    fun sanity_withMixedGridRowAndRow() {
        val resIds = intArrayOf(
            R.drawable.ic_all_out_black_24dp,
            R.drawable.ic_android_black_24dp,
            R.drawable.ic_attach_money_black_24dp
        )

        val pendingIntent = pendingIntentToTestActivity()
        val rowPrimaryAction = tapSliceAction(
            pendingIntent,
            createIcon(R.drawable.ic_android_black_24dp),
            ListBuilder.LARGE_IMAGE,
            "Droid"
        )

        val sliceAction1 = SliceAction(
            pendingIntent,
            createIcon(R.drawable.ic_all_out_black_24dp),
            ListBuilder.LARGE_IMAGE,
            "Action1"
        )

        val sliceAction2 = SliceAction(
            pendingIntent,
            createIcon(R.drawable.ic_all_out_black_24dp),
            "Action2",
            false
        )

        val icons = (resIds + resIds).map {
            createIcon(it)
        }

        val sliceKtx = list(context = context, uri = testUri, ttl = ListBuilder.INFINITY) {
            setAccentColor(accentColor)
            row {
                setTitle("Title")
                setSubtitle("Subtitle")
                setPrimaryAction(rowPrimaryAction)
            }
            addAction(sliceAction1)
            addAction(sliceAction2)
            gridRow {
                icons.forEach {
                    cell { addImage(it, ICON_IMAGE) }
                }
            }
        }

        val slice = ListBuilder(context, testUri, ListBuilder.INFINITY).apply {
            setAccentColor(accentColor)
            addRow(
                ListBuilder.RowBuilder()
                    .setTitle("Title")
                    .setSubtitle("Subtitle")
                    .setPrimaryAction(rowPrimaryAction)
            )
            addAction(sliceAction1)
            addAction(sliceAction2)
            addGridRow(
                GridRowBuilder().apply {
                    icons.forEach { icon ->
                        GridRowBuilder.CellBuilder().addImage(icon, ICON_IMAGE)
                        addCell(
                            GridRowBuilder.CellBuilder().apply {
                                addImage(icon, ICON_IMAGE)
                            }
                        )
                    }
                }
            )
        }.build()

        // Compare toString()s because Slice.equals is not implemented.
        assertEquals(slice.toString(), sliceKtx.toString())
    }
}
