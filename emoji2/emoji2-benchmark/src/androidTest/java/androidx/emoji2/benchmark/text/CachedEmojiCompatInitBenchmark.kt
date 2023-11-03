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
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CachedEmojiCompatInitBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * This is worst case lookup, there is no default emojicompat config which triggers two
     * volatile reads
     */
    @Test
    fun cachedEmojiCompatInit_returningNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        EmojiCompat.reset(null as EmojiCompat?)
        EmojiCompat.skipDefaultConfigurationLookup(true)
        benchmarkRule.measureRepeated {
            EmojiCompat.init(context)
        }
    }

    @Test
    fun cachedEmojiCompatInit_returningNonNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = NoFontTestEmojiConfig.emptyConfig()
            .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL)
        EmojiCompat.reset(config)
        EmojiCompat.skipDefaultConfigurationLookup(true)
        benchmarkRule.measureRepeated {
            EmojiCompat.init(context)
        }
    }

    @Test
    fun actualEmojiCompatContextInit_fromQuickFactory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = NoFontTestEmojiConfig.emptyConfig()
            .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL)
        val factory = TestEmojiCompatConfigFactory(config)

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                EmojiCompat.reset(null as EmojiCompat?)
                EmojiCompat.skipDefaultConfigurationLookup(false)
            }
            val result = EmojiCompat.init(context, factory)
            runWithTimingDisabled {
                assertNotNull(result)
            }
        }
    }

    class TestEmojiCompatConfigFactory(private val config: EmojiCompat.Config) :
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory(null) {
        override fun create(context: Context): EmojiCompat.Config {
            return config
        }
    }
}
