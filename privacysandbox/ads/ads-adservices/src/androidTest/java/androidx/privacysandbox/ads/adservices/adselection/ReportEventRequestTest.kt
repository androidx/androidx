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

package androidx.privacysandbox.ads.adservices.adselection

import android.view.InputEvent
import android.view.KeyEvent
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class ReportEventRequestTest {
    private val adSelectionId: Long = 1234L
    private val eventKey: String = "click"
    private val eventData: String = "{\"key\":\"value\"}"
    private val reportingDestinations: Int = ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
    private val inputEvent: InputEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_1)

    @Test
    fun testToString() {
        val result =
            "ReportEventRequest: adSelectionId=$adSelectionId, eventKey=$eventKey, " +
                "eventData=$eventData, reportingDestinations=$reportingDestinations" +
                "inputEvent=$inputEvent"
        val request =
            ReportEventRequest(
                adSelectionId,
                eventKey,
                eventData,
                reportingDestinations,
                inputEvent
            )
        Truth.assertThat(request.toString()).isEqualTo(result)
    }

    @Test
    fun testEquals() {
        val reportEventRequest =
            ReportEventRequest(
                adSelectionId,
                eventKey,
                eventData,
                reportingDestinations,
                inputEvent
            )
        var reportEventRequest2 =
            ReportEventRequest(
                1234L,
                "click",
                "{\"key\":\"value\"}",
                ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER,
                inputEvent
            )

        Truth.assertThat(reportEventRequest == reportEventRequest2).isTrue()
    }

    @Test
    fun testInvalidReportingDestinations() {
        assertThrows<IllegalArgumentException> {
                ReportEventRequest(
                    adSelectionId,
                    eventKey,
                    eventData,
                    0 /* unset reporting destinations */
                )
            }
            .hasMessageThat()
            .contains("Invalid reporting destinations bitfield.")

        assertThrows<IllegalArgumentException> {
                ReportEventRequest(
                    adSelectionId,
                    eventKey,
                    eventData,
                    4 /* undefined reporting destinations */
                )
            }
            .hasMessageThat()
            .contains("Invalid reporting destinations bitfield.")
    }
}
