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

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class FullyLoadedReporterTest {
    @Test
    fun findFullyLoadedReporter() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity {
                val view = View(this)
                setContentView(view)
                val reporter = FullyLoadedReporter.findFullyLoadedReporter(this)
                assertNotNull(reporter)
                reporter!!
            }
            withActivity {
                assertSame(fullyLoadedReporter, FullyLoadedReporter.findFullyLoadedReporter(this))
            }
        }
    }

    @Test
    fun findFullyLoadedReporterInDialog() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            lateinit var dialogContext: Context
            withActivity {
                val view1 = View(this)
                setContentView(view1)
                val dialog = Dialog(this)
                val view2 = View(dialog.context)
                dialog.setContentView(view2)
                dialogContext = dialog.context
            }
            withActivity {
                val activityReporter = FullyLoadedReporter.findFullyLoadedReporter(this)
                val dialogReporter = FullyLoadedReporter.findFullyLoadedReporter(dialogContext)
                assertSame(activityReporter, dialogReporter)
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
                fullyLoadedReporter = FullyLoadedReporter.findFullyLoadedReporter(this)!!
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
                    assertFalse(fullyDrawnReported)
                }
                mutex.unlock()
                delay(1L)
                waitForPostAnimation {
                    assertTrue(fullyDrawnReported)
                }
            }
        }
    }

    @Test
    fun addReporter() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity {
                FullyLoadedReporter.findFullyLoadedReporter(this)!!
            }
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.removeReporter()

            waitForPostAnimation {
                assertFalse(fullyDrawnReported)
            }
            fullyLoadedReporter.removeReporter()
            waitForPostAnimation {
                assertTrue(fullyDrawnReported)
            }
        }
    }

    @Test
    fun reporterAndReportWhen() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity {
                FullyLoadedReporter.findFullyLoadedReporter(this)!!
            }
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
                    assertFalse(fullyDrawnReported)
                }
                mutex.unlock()
                delay(1L) // allow launch to continue
                waitForPostAnimation {
                    assertTrue(fullyDrawnReported)
                }
            }
        }
    }

    @Test
    fun reportWhenAndReporter() {
        with(ActivityScenario.launch(FullyLoadedActivity::class.java)) {
            val fullyLoadedReporter = withActivity {
                FullyLoadedReporter.findFullyLoadedReporter(this)!!
            }
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
                    assertFalse(fullyDrawnReported)
                }

                fullyLoadedReporter.removeReporter()
                waitForPostAnimation {
                    assertTrue(fullyDrawnReported)
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

            val fullyLoadedReporter = withActivity {
                setContentView(View(this))
                FullyLoadedReporter.findFullyLoadedReporter(this)!!
            }
            fullyLoadedReporter.addReporter()
            fullyLoadedReporter.addOnReportLoadedListener(reportListener1)
            fullyLoadedReporter.addOnReportLoadedListener(reportListener2)
            fullyLoadedReporter.removeOnReportLoadedListener(reportListener2)
            fullyLoadedReporter.removeReporter()
            waitForPostAnimation {
                assertTrue(report1)
                assertFalse(report2)
            }
            fullyLoadedReporter.addOnReportLoadedListener(reportListener3)
            assertTrue(report3)
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
        assertTrue(countDownLatch.await(10, TimeUnit.SECONDS))
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