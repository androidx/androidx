/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.testutils

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Extension of [AppCompatActivity] that keeps track of when it is recreated.
 * In order to use this class, have your activity extend it and call
 * [recreate] API.
 */
open class RecreatedAppCompatActivity(
    @LayoutRes contentLayoutId: Int = 0
) : AppCompatActivity(contentLayoutId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
    }

    override fun onResume() {
        super.onResume()
        resumedLatch?.countDown()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyedLatch?.countDown()
    }

    companion object {
        @JvmStatic
        var activity: RecreatedAppCompatActivity? = null
        @JvmStatic
        internal var resumedLatch: CountDownLatch? = null
        @JvmStatic
        internal var destroyedLatch: CountDownLatch? = null

        @JvmStatic
        internal fun clearState() {
            activity = null
            resumedLatch = null
            destroyedLatch = null
        }
    }
}

/**
 * Restarts the [RecreatedAppCompatActivity] and waits for the new activity to be resumed.
 *
 * @return The newly-restarted [RecreatedAppCompatActivity]
 */
@Suppress("UNCHECKED_CAST")
fun <T : RecreatedAppCompatActivity> ActivityTestRule<T>.recreate(): T {
    // Now switch the orientation
    RecreatedAppCompatActivity.resumedLatch = CountDownLatch(1)
    RecreatedAppCompatActivity.destroyedLatch = CountDownLatch(1)

    runOnUiThreadRethrow { activity.recreate() }
    assertTrue(RecreatedAppCompatActivity.resumedLatch!!.await(1, TimeUnit.SECONDS))
    assertTrue(RecreatedAppCompatActivity.destroyedLatch!!.await(1, TimeUnit.SECONDS))
    val newActivity = RecreatedAppCompatActivity.activity as T

    waitForExecution()

    RecreatedAppCompatActivity.clearState()
    return newActivity
}
