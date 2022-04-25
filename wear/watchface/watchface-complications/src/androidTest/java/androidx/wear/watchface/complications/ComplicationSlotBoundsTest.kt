/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications

import android.content.Context
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.test.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RequiresApi(Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4::class)
@MediumTest
class ComplicationSlotBoundsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    public fun test_inflate_list_schema() {
        val parser = context.resources.getXml(R.xml.complication_slot_bounds)

        // Parse next until start tag is found
        var nodeType: Int
        do {
            nodeType = parser.next()
        } while (nodeType != XmlPullParser.END_DOCUMENT && nodeType != XmlPullParser.START_TAG)

        val bounds = ComplicationSlotBounds.inflate(parser)!!

        // SHORT_TEXT and LONG_TEXT should match the input
        assertThat(
            bounds.perComplicationTypeBounds[ComplicationType.SHORT_TEXT]
        ).isEqualTo(RectF(0.2f, 0.4f, 0.3f, 0.1f))

        assertThat(
            bounds.perComplicationTypeBounds[ComplicationType.LONG_TEXT]
        ).isEqualTo(RectF(0.6f, 0.8f, 0.7f, 0.5f))

        // All other types should have been backfilled with an empty rect.
        for (type in ComplicationType.values()) {
            if (type != ComplicationType.SHORT_TEXT && type != ComplicationType.LONG_TEXT) {
                assertThat(bounds.perComplicationTypeBounds[type]).isEqualTo(RectF())
            }
        }
    }
}