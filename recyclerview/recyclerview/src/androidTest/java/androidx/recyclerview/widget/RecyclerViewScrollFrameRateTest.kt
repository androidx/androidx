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
@file:Suppress("DEPRECATION")

package androidx.recyclerview.widget

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(AndroidJUnit4::class)
class RecyclerViewScrollFrameRateTest {
    @get:Rule val rule = ActivityTestRule(TestContentViewActivity::class.java)

    @Test
    fun smoothScrollFrameRateBoost() {
        val rv = RecyclerView(rule.activity)
        rule.runOnUiThread {
            rv.layoutManager =
                LinearLayoutManager(rule.activity, LinearLayoutManager.VERTICAL, false)
            rv.adapter =
                object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(
                        parent: ViewGroup,
                        viewType: Int
                    ): RecyclerView.ViewHolder {
                        val view = TextView(parent.context)
                        view.textSize = 40f
                        view.setTextColor(Color.WHITE)
                        return object : RecyclerView.ViewHolder(view) {}
                    }

                    override fun getItemCount(): Int = 10000

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        val view = holder.itemView as TextView
                        view.text = "Text $position"
                        val color = if (position % 2 == 0) Color.BLACK else 0xFF000080.toInt()
                        view.setBackgroundColor(color)
                    }
                }
            rule.activity.contentView.addView(rv)
        }
        runOnDraw(rv, { rv.smoothScrollBy(0, 1000) }) {
            // First Frame
            assertThat(rv.frameContentVelocity).isGreaterThan(0f)
        }

        // Second frame
        runOnDraw(rv) { assertThat(rv.frameContentVelocity).isGreaterThan(0f) }

        // Third frame
        runOnDraw(rv) { assertThat(rv.frameContentVelocity).isGreaterThan(0f) }
    }

    private fun runOnDraw(view: View, setup: () -> Unit = {}, onDraw: () -> Unit) {
        val latch = CountDownLatch(1)
        val onDrawListener =
            ViewTreeObserver.OnDrawListener {
                latch.countDown()
                onDraw()
            }
        rule.runOnUiThread {
            view.viewTreeObserver.addOnDrawListener(onDrawListener)
            setup()
        }
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        rule.runOnUiThread { view.viewTreeObserver.removeOnDrawListener(onDrawListener) }
    }
}
