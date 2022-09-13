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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class FullyLoadedReporterTest {
    @Test
    fun findFullyLoadedReporterOwner() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val provider1 = object : FullyLoadedReporterOwner {
                override val fullyLoadedReporter: FullyLoadedReporter
                    get() = withActivity { fullyLoadedReporter }
            }
            val provider2 = object : FullyLoadedReporterOwner {
                override val fullyLoadedReporter: FullyLoadedReporter
                    get() = withActivity { fullyLoadedReporter }
            }
            val view = withActivity {
                val view = View(this)
                setContentView(view)
                window.decorView.setViewTreeFullyLoadedReporterOwner(provider1)
                view.setViewTreeFullyLoadedReporterOwner(provider2)
                view
            }
            withActivity {
                assertThat(view.findViewTreeFullyLoadedReporterOwner())
                    .isSameInstanceAs(provider2)
                assertThat(window.decorView.findViewTreeFullyLoadedReporterOwner())
                    .isSameInstanceAs(provider1)
                assertThat((view.parent as View).findViewTreeFullyLoadedReporterOwner())
                    .isSameInstanceAs(provider1)
            }
        }
    }

    @Test
    fun reportWhenComplete() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            lateinit var fullyLoadedReporter: FullyLoadedReporter
            withActivity {
                val view = View(this)
                setContentView(view)
                fullyLoadedReporter = this.fullyLoadedReporter
            }
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyLoadedReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L)
                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isFalse()
                }
                mutex.unlock()
                delay(1L)
                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun addReporter() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity { fullyLoadedReporter }
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.removeReporter()

            waitForPostAnimation {
                assertThat(fullyDrawnReported).isFalse()
            }
            fullyLoadedReporter.removeReporter()
            waitForPostAnimation {
                assertThat(fullyDrawnReported).isTrue()
            }
        }
    }

    @Test
    fun reporterAndReportWhen() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity { fullyLoadedReporter }
            fullyLoadedReporter.addReporter()
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyLoadedReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L) // wait for launch
                fullyLoadedReporter.removeReporter()

                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isFalse()
                }
                mutex.unlock()
                delay(1L) // allow launch to continue
                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun reportWhenAndReporter() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity { fullyLoadedReporter }
            fullyLoadedReporter.addReporter()
            val mutex = Mutex(true)
            runBlocking {
                launch {
                    fullyLoadedReporter.reportWhenComplete {
                        mutex.lock()
                        mutex.unlock()
                    }
                }
                delay(1L) // wait for launch
                mutex.unlock()
                delay(1L) // allow launch to continue
                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isFalse()
                }

                fullyLoadedReporter.removeReporter()
                waitForPostAnimation {
                    assertThat(fullyDrawnReported).isTrue()
                }
            }
        }
    }

    @Test
    fun reportListener() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            var report1 = false
            var report2 = false
            var report3 = false
            val reportListener1 = { report1 = true }
            val reportListener2 = { report2 = true }
            val reportListener3 = { report3 = true }

            withActivity {
                setContentView(View(this))
            }
            val fullyLoadedReporter = withActivity { fullyLoadedReporter }
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.addOnReportLoadedListener(reportListener1)
            fullyLoadedReporter.addOnReportLoadedListener(reportListener2)
            fullyLoadedReporter.removeOnReportLoadedListener(reportListener2)
            fullyLoadedReporter.removeReporter()
            waitForPostAnimation {
                assertThat(report1).isTrue()
                assertThat(report2).isFalse()
            }
            fullyLoadedReporter.addOnReportLoadedListener(reportListener3)
            assertThat(report3).isTrue()
        }
    }

    private fun ActivityScenario<FullyLoadedActivity>.waitForPostAnimation(
        block: FullyLoadedActivity.() -> Unit = {}
    ) {
        val countDownLatch = CountDownLatch(1)
        withActivity {
            runOnUiThread {
                window.decorView.postOnAnimation {
                    countDownLatch.countDown()
                }
                window.decorView.invalidate()
            }
        }
        assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue()
        withActivity {
            block()
        }
    }
}

class FullyLoadedActivity : ComponentActivity() {
    var fullyDrawnReported = false

    override fun reportFullyDrawn() {
        fullyDrawnReported = true
        super.reportFullyDrawn()
    }
}