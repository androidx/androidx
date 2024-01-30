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
package androidx.activity

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver.OnDrawListener
import androidx.core.view.OneShotPreDrawListener
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class FullyDrawnReporterTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun findFullyDrawnReporterOwner() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            val provider1 = object : FullyDrawnReporterOwner {
                override val fullyDrawnReporter: FullyDrawnReporter
                    get() = withActivity { fullyDrawnReporter }
            }
            val provider2 = object : FullyDrawnReporterOwner {
                override val fullyDrawnReporter: FullyDrawnReporter
                    get() = withActivity { fullyDrawnReporter }
            }
            val view = withActivity {
                val view = View(this)
                setContentView(view)
                window.decorView.setViewTreeFullyDrawnReporterOwner(provider1)
                view.setViewTreeFullyDrawnReporterOwner(provider2)
                view
            }
            withActivity {
                assertThat(view.findViewTreeFullyDrawnReporterOwner())
                    .isSameInstanceAs(provider2)
                assertThat(window.decorView.findViewTreeFullyDrawnReporterOwner())
                    .isSameInstanceAs(provider1)
                assertThat((view.parent as View).findViewTreeFullyDrawnReporterOwner())
                    .isSameInstanceAs(provider1)
            }
        }
    }

    @Test
    fun reportWhenComplete() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            lateinit var fullyDrawnReporter: FullyDrawnReporter
            withActivity {
                val view = View(this)
                setContentView(view)
                fullyDrawnReporter = this.fullyDrawnReporter
            }
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyDrawnReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L)
                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isFalse()
                }
                mutex.unlock()
                delay(1L)
                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun addReporter() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }
            fullyDrawnReporter.addReporter()
            fullyDrawnReporter.addReporter()
            fullyDrawnReporter.removeReporter()

            waitForOnDrawComplete {
                assertThat(fullyDrawnReported).isFalse()
            }
            fullyDrawnReporter.removeReporter()
            waitForOnDrawComplete {
                assertThat(fullyDrawnReported).isTrue()
            }
        }
    }

    @Test
    fun reporterAndReportWhen() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }
            fullyDrawnReporter.addReporter()
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyDrawnReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L) // wait for launch
                fullyDrawnReporter.removeReporter()

                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isFalse()
                }
                mutex.unlock()
                delay(1L) // allow launch to continue
                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun reportWhenAndReporter() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }
            fullyDrawnReporter.addReporter()
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyDrawnReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L) // wait for launch
                mutex.unlock()
                delay(1L) // allow launch to continue
                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isFalse()
                }

                fullyDrawnReporter.removeReporter()
                waitForOnDrawComplete {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun reportListener() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            var report1 = false
            var report2 = false
            var report3 = false
            val reportListener1 = { report1 = true }
            val reportListener2 = { report2 = true }
            val reportListener3 = { report3 = true }

            withActivity {
                setContentView(View(this))
            }
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }
            fullyDrawnReporter.addReporter()
            fullyDrawnReporter.addOnReportDrawnListener(reportListener1)
            fullyDrawnReporter.addOnReportDrawnListener(reportListener2)
            fullyDrawnReporter.removeOnReportDrawnListener(reportListener2)
            fullyDrawnReporter.removeReporter()
            waitForOnDrawComplete {
                assertThat(report1).isTrue()
                assertThat(report2).isFalse()
            }
            fullyDrawnReporter.addOnReportDrawnListener(reportListener3)
            assertThat(report3).isTrue()
        }
    }

    /**
     * Removing the last reporter and then adding another one within the same frame should not
     * trigger the reportFullyDrawn().
     */
    @Test
    fun fakeoutReport() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            withActivity {
                setContentView(View(this))
            }
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }

            onActivity {
                fullyDrawnReporter.addReporter()
                fullyDrawnReporter.removeReporter()
                fullyDrawnReporter.addReporter()
            }
            waitForOnDrawComplete {
                assertThat(fullyDrawnReporter.isFullyDrawnReported).isFalse()
            }
            onActivity {
                fullyDrawnReporter.removeReporter()
            }
            waitForOnDrawComplete {
                assertThat(fullyDrawnReporter.isFullyDrawnReported).isTrue()
            }
        }
    }

    /**
     * The [ComponentActivity.reportFullyDrawn] should be called during OnDraw.
     */
    @Test
    fun reportedInOnDraw() {
        withUse(ActivityScenario.launch(FullyDrawnActivity::class.java)) {
            withActivity {
                setContentView(View(this))
            }
            val fullyDrawnReporter = withActivity { fullyDrawnReporter }

            var fullyDrawnInOnDraw = false
            var fullyDrawnInOnPreDraw = false
            onActivity { activity ->
                fullyDrawnReporter.addReporter()
                fullyDrawnReporter.removeReporter()
                OneShotPreDrawListener.add(activity.window.decorView) {
                    fullyDrawnInOnPreDraw = fullyDrawnReporter.isFullyDrawnReported
                }
                val onDrawListener = object : OnDrawListener {
                    override fun onDraw() {
                        fullyDrawnInOnDraw = fullyDrawnReporter.isFullyDrawnReported
                        activity.window.decorView.post {
                            activity.window.decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
                activity.window.decorView.viewTreeObserver.addOnDrawListener(onDrawListener)
            }
            waitForOnDrawComplete {
                assertThat(fullyDrawnInOnPreDraw).isFalse()
                assertThat(fullyDrawnInOnDraw).isTrue()
            }
        }
    }

    private fun ActivityScenario<FullyDrawnActivity>.waitForOnDrawComplete(
        block: FullyDrawnActivity.() -> Unit = {}
    ) {
        val countDownLatch = CountDownLatch(1)
        val observer = OnDrawListener {
            countDownLatch.countDown()
        }
        withActivity {
            runOnUiThread {
                window.decorView.viewTreeObserver.addOnDrawListener(observer)
                window.decorView.invalidate()
            }
        }
        assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue()
        withActivity {
            runOnUiThread {
                window.decorView.viewTreeObserver.removeOnDrawListener(observer)
            }
            block()
        }
    }
}

class FullyDrawnActivity : ComponentActivity() {
    var fullyDrawnReported = false

    override fun reportFullyDrawn() {
        fullyDrawnReported = true
        super.reportFullyDrawn()
    }
}
