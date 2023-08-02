/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.benchmark.text

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun initializeEmojiCompatWithBundledForTest(replaceAll: Boolean = true) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = BundledEmojiCompatConfig(context)
    config.setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL)
    config.setReplaceAll(replaceAll)
    val latch = CountDownLatch(1)
    config.registerInitCallback(object : EmojiCompat.InitCallback() {
        override fun onInitialized() {
            super.onInitialized()
            latch.countDown()
        }
    })
    EmojiCompat.reset(config).load()
    latch.await(2, TimeUnit.SECONDS)
}
