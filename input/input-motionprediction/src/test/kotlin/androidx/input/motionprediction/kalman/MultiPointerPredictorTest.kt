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

package androidx.input.motionprediction.kalman

import android.view.MotionEvent
import androidx.input.motionprediction.MotionEventGenerator
import androidx.input.motionprediction.common.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MultiPointerPredictorTest {

    // Ensures that the historical time is properly populated (b/302300930)
    @Test
    fun historicalTime() {
        val predictor = MultiPointerPredictor(Configuration.STRATEGY_BALANCED)
        val generator =
            MotionEventGenerator(
                { delta: Long -> delta.toFloat() },
                { delta: Long -> delta.toFloat() },
                null,
                { delta: Long -> delta.toFloat() },
                { delta: Long -> delta.toFloat() },
                null,
            )
        for (i in 1..INITIAL_FEED) {
            predictor.onTouchEvent(generator.next())
        }
        // Get some historical events
        val predicted = predictor.predict(PREDICT_SAMPLE * generator.getRateMs().toInt())!!
        assertThat(predicted.getPointerCount()).isEqualTo(2)
        var historicalTime = predicted.getEventTime()
        for (i in (PREDICT_SAMPLE - 2) downTo 0) {
            historicalTime -= generator.getRateMs().toInt()
            assertThat(predicted.getHistoricalEventTime(i)).isEqualTo(historicalTime)
        }
    }

    // Ensures that the down time is properly populated
    @Test
    fun downTime() {
        val predictor = MultiPointerPredictor(Configuration.STRATEGY_BALANCED)
        val generator =
            MotionEventGenerator(
                { delta: Long -> delta.toFloat() },
                { delta: Long -> delta.toFloat() },
                null,
                { delta: Long -> delta.toFloat() },
                { delta: Long -> delta.toFloat() },
                null,
            )
        var firstEvent: MotionEvent? = null
        for (i in 1..INITIAL_FEED) {
            val nextEvent = generator.next()
            if (firstEvent == null) {
                firstEvent = nextEvent
            }
            predictor.onTouchEvent(nextEvent)
        }
        val predicted = predictor.predict(PREDICT_SAMPLE * generator.getRateMs().toInt())!!
        assertThat(predicted.getPointerCount()).isEqualTo(2)
        assertThat(predicted.getDownTime()).isEqualTo(firstEvent?.getEventTime())
    }
}

private const val PREDICT_SAMPLE = 5

private const val INITIAL_FEED = 20
