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

package androidx.work.impl.background

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.work.Configuration
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.testutils.TestOverrideClock
import androidx.work.impl.utils.PreferenceUtils
import androidx.work.testutils.GreedyScheduler
import androidx.work.testutils.TestEnv
import androidx.work.testutils.WorkManager
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class WorkManagerImplTestKt {
    val clock = TestOverrideClock()
    val configuration =
        Configuration.Builder().setClock(clock)
            .setTaskExecutor(Executors.newSingleThreadExecutor()).build()
    val env = TestEnv(configuration)
    val taskExecutor = env.taskExecutor
    val trackers = Trackers(context = env.context, taskExecutor = env.taskExecutor)
    val workManager = WorkManager(env, listOf(GreedyScheduler(env, trackers)), trackers)

    @Test
    fun testCancelAllWork_updatesLastCancelAllTimeLiveData() {
        clock.currentTimeMillis = 30
        val preferenceUtils = PreferenceUtils(env.db)
        preferenceUtils.lastCancelAllTimeMillis = 0L

        val testLifecycleOwner = TestLifecycleOwner()
        val cancelAllTimeLiveData = workManager.lastCancelAllTimeMillisLiveData
        val firstValueLatch = CountDownLatch(1)
        val secondValueLatch = CountDownLatch(1)
        var firstCancelAll = -1L
        var secondCancelAll = -1L
        var counter = 0
        env.taskExecutor.mainThreadExecutor.execute {
            cancelAllTimeLiveData.observe(testLifecycleOwner) {
                if (counter == 0) {
                    firstCancelAll = it
                    firstValueLatch.countDown()
                } else {
                    secondCancelAll = it
                    secondValueLatch.countDown()
                }
                counter++
            }
        }

        firstValueLatch.await()
        assertThat(firstCancelAll).isEqualTo(0)
        clock.currentTimeMillis = 50
        workManager.cancelAllWork()
        secondValueLatch.await()
        assertThat(secondCancelAll).isEqualTo(50)
    }
}
