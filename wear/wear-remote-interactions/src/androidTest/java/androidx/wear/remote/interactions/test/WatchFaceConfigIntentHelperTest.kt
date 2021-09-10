/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.remote.interactions.test

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.wear.remote.interactions.WatchFaceConfigIntentHelper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class WatchFaceConfigIntentHelperTest {
    @Test
    fun testParseWatchFaceComponent() {
        val intent = Intent()
        val componentName = ComponentName("test.package", "TestClass")
        WatchFaceConfigIntentHelper.putWatchFaceComponentExtra(intent, componentName)
        assert(componentName == WatchFaceConfigIntentHelper.getWatchFaceComponentExtra(intent))
    }

    @Test
    fun testParseWatchPeerId() {
        val intent = Intent()
        val peerId = "testPeerId"
        WatchFaceConfigIntentHelper.putPeerIdExtra(intent, peerId)
        assert(peerId == WatchFaceConfigIntentHelper.getPeerIdExtra(intent))
    }

    @Test
    fun testParseWatchFaceComponentNull() {
        assert(null == WatchFaceConfigIntentHelper.getWatchFaceComponentExtra(Intent()))
    }

    @Test
    fun testParseWatchPeerIdNull() {
        assert(null == WatchFaceConfigIntentHelper.getPeerIdExtra(Intent()))
    }
}