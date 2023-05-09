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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import androidx.privacysandbox.sdkruntime.client.EmptyActivity
import androidx.privacysandbox.sdkruntime.client.activity.ComponentActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkActivityHandlerWrapperTest {

    private lateinit var wrapperFactory: SdkActivityHandlerWrapper

    @Before
    fun setUp() {
        wrapperFactory = SdkActivityHandlerWrapper.createFor(javaClass.classLoader!!)
    }

    @Test
    fun wrapSdkSandboxActivityHandlerCompat_passActivityToOriginalHandler() {
        val catchingHandler = TestHandler()

        val wrappedHandler = wrapperFactory.wrapSdkSandboxActivityHandlerCompat(catchingHandler)

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                val activityHolder = ComponentActivityHolder(this)

                wrappedHandler.onActivityCreated(activityHolder)
                val receivedActivityHolder = catchingHandler.result!!

                assertThat(receivedActivityHolder.getActivity())
                    .isSameInstanceAs(activityHolder.getActivity())
            }
        }
    }

    private class TestHandler : SdkSandboxActivityHandlerCompat {
        var result: ActivityHolder? = null

        override fun onActivityCreated(activityHolder: ActivityHolder) {
            result = activityHolder
        }
    }
}