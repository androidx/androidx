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
import kotlin.math.pow
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SinglePointerPredictorTest {

    @Test
    fun simplePrediction() {
        val generators =
            arrayOf(
                // Constant
                { _: Long -> 0f },
                // Velocity
                { delta: Long -> delta.toFloat() },
                { delta: Long -> -delta.toFloat() },
                { delta: Long -> 2 * delta.toFloat() },
                // Acceleration
                { delta: Long -> delta.toFloat().pow(2) / 4 },
                { delta: Long -> -delta.toFloat().pow(2) / 4 },
                { delta: Long -> delta.toFloat().pow(2) / 2 },
                // Acceleration & velocity
                { delta: Long -> delta.toFloat() + delta.toFloat().pow(2) / 4 },
                { delta: Long -> -delta.toFloat() - delta.toFloat().pow(2) / 4 }
            )
        for ((xIndex, xGenerator) in generators.withIndex()) {
            for ((yIndex, yGenerator) in generators.withIndex()) {
                if (xIndex == 0 && yIndex == 0) {
                    // Predictions won't be generated in this case
                    continue
                }
                val predictor = constructPredictor()
                val generator = MotionEventGenerator(xGenerator, yGenerator, null)
                for (i in 1..INITIAL_FEED) {
                    predictor.onTouchEvent(generator.next())
                    predictor.predict(generator.getRateMs().toInt())
                }
                for (i in 1..PREDICT_LENGTH) {
                    val predicted = predictor.predict(generator.getRateMs().toInt())!!
                    val nextEvent = generator.next()
                    assertThat(predicted.eventTime).isEqualTo(nextEvent.eventTime)
                    assertThat(predicted.x).isWithin(0.5f).of(nextEvent.x)
                    assertThat(predicted.y).isWithin(0.5f).of(nextEvent.y)

                    predictor.onTouchEvent(nextEvent)
                }
            }
        }
    }

    @Test
    fun predictionNeverGoesBackwards() {
        val predictor = constructPredictor()
        val coordGenerator = { delta: Long -> delta.toFloat() }
        val motionGenerator = MotionEventGenerator(coordGenerator, coordGenerator, null)
        var lastPredictedTime = 0L
        for (i in 1..INITIAL_FEED) {
            predictor.onTouchEvent(motionGenerator.next())
            val predicted = predictor.predict(motionGenerator.getRateMs().toInt() * 10)
            if (predicted != null) {
                assertThat(predicted.eventTime).isAtLeast(lastPredictedTime)
                lastPredictedTime = predicted.eventTime
            }
        }

        predictor.onTouchEvent(motionGenerator.next())
        val predicted = predictor.predict(motionGenerator.getRateMs().toInt())!!
        assertThat(predicted.eventTime).isAtLeast(lastPredictedTime)
    }

    @Test
    fun liftingDoesNotAffectPredictionDistance() {
        val predictor = constructPredictor()
        val coordGenerator = { delta: Long -> delta.toFloat() }
        // Pressure will be 1 at the beginning and trend to zero while never getting there
        val pressureGenerator =
            fun(delta: Long): Float {
                if (delta > 500) {
                    return ((700 - delta) / 500).toFloat()
                }
                return 1f
            }
        val motionGenerator =
            MotionEventGenerator(coordGenerator, coordGenerator, pressureGenerator)
        var lastPredictedTime = 0L
        var lastPredictedEvent: MotionEvent? = null
        var predicted: MotionEvent?
        for (i in 1..MAX_ITERATIONS) {
            predictor.onTouchEvent(motionGenerator.next())
            predicted = predictor.predict(motionGenerator.getRateMs().toInt() * 10)
            if (predicted != null) {
                assertThat(predicted.eventTime).isAtLeast(lastPredictedTime)
                lastPredictedTime = predicted.eventTime
            } else if (lastPredictedEvent != null) {
                assertThat(lastPredictedEvent.getHistorySize()).isEqualTo(0)
            }
            lastPredictedEvent = predicted
            if (i > INITIAL_FEED) {
                assertThat(predicted).isNotNull()
            }
        }
    }
}

private fun constructPredictor(): SinglePointerPredictor =
    SinglePointerPredictor(Configuration.STRATEGY_BALANCED, 0, MotionEvent.TOOL_TYPE_STYLUS)

private const val INITIAL_FEED = 20
private const val MAX_ITERATIONS = 10000
private const val PREDICT_LENGTH = 10
