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
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.CountDownLatch

/**
 * Extension of [FragmentActivity] that keeps track of when it is recreated.
 * In order to use this class, have your activity extend it and call
 * [FragmentActivityUtils.recreateActivity] API.
 */
open class RecreatedActivity(
    @LayoutRes contentLayoutId: Int = 0
) : FragmentActivity(contentLayoutId) {

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
        // These must be cleared after each test using clearState()
        @JvmStatic
        var activity: RecreatedActivity? = null
        @JvmStatic
        var resumedLatch: CountDownLatch? = null
        @JvmStatic
        var destroyedLatch: CountDownLatch? = null

        @JvmStatic
        fun clearState() {
            activity = null
            resumedLatch = null
            destroyedLatch = null
        }
    }
}
