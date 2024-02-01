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

package androidx.activity

import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityCallbacksTest {

    @get:Rule
    val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun onConfigurationChanged() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedFontScale = withActivity {
                resources.configuration.fontScale
            }
            val expectedFontScale = receivedFontScale * 2

            val listener = Consumer<Configuration> { newConfig ->
                receivedFontScale = newConfig.fontScale
            }
            withActivity {
                addOnConfigurationChangedListener(listener)
                val newConfig = Configuration(resources.configuration)
                newConfig.fontScale *= 2
                onConfigurationChanged(newConfig)
            }

            assertThat(receivedFontScale).isEqualTo(expectedFontScale)
        }
    }

    @Test
    fun onConfigurationChangedRemove() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedFontScale = withActivity {
                resources.configuration.fontScale
            }
            val expectedFontScale = receivedFontScale * 2

            val listener = Consumer<Configuration> { newConfig ->
                receivedFontScale = newConfig.fontScale
            }
            withActivity {
                addOnConfigurationChangedListener(listener)
                val newConfig = Configuration(resources.configuration)
                newConfig.fontScale *= 2
                onConfigurationChanged(newConfig)
            }

            assertThat(receivedFontScale).isEqualTo(expectedFontScale)

            withActivity {
                removeOnConfigurationChangedListener(listener)
                val newConfig = Configuration(resources.configuration)
                newConfig.fontScale *= 2
                onConfigurationChanged(newConfig)
            }

            assertThat(receivedFontScale).isEqualTo(expectedFontScale)
        }
    }

    @Test
    fun onConfigurationChangedReentrant() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            var receivedFontScale = withActivity {
                resources.configuration.fontScale
            }
            val expectedFontScale = receivedFontScale * 2

            val listener = object : Consumer<Configuration> {
                override fun accept(value: Configuration) {
                    receivedFontScale = value.fontScale
                    activity.removeOnConfigurationChangedListener(this)
                }
            }
            withActivity {
                addOnConfigurationChangedListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnConfigurationChangedListener { }
                val newConfig = Configuration(resources.configuration)
                newConfig.fontScale *= 2
                onConfigurationChanged(newConfig)
                val secondConfig = Configuration(newConfig)
                secondConfig.fontScale *= 2
                onConfigurationChanged(secondConfig)
            }

            // Only the first trim level should be received
            assertThat(receivedFontScale).isEqualTo(expectedFontScale)
        }
    }

    @Test
    fun onTrimMemory() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedLevel = -1

            val listener = Consumer<Int> { level ->
                receivedLevel = level
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
            }

            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }

    @Test
    fun onTrimMemoryRemove() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            var receivedLevel = -1

            val listener = Consumer<Int> { level ->
                receivedLevel = level
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
            }

            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)

            withActivity {
                removeOnTrimMemoryListener(listener)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }

            // Should still be MODERATE and not TRIM_MEMORY_COMPLETE
            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }

    @Test
    fun onTrimMemoryRemoveReentrant() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            var receivedLevel = -1

            val listener = object : Consumer<Int> {
                override fun accept(value: Int) {
                    receivedLevel = value
                    activity.removeOnTrimMemoryListener(this)
                }
            }
            withActivity {
                addOnTrimMemoryListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnTrimMemoryListener { }
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
                onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }

            // Only the first trim level should be received
            assertThat(receivedLevel).isEqualTo(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        }
    }

    @Test
    fun onNewIntent() {
       withUse(ActivityScenario.launch(SingleTopActivity::class.java)) {
            val receivedIntents = mutableListOf<Intent>()

            val listener = Consumer<Intent> { intent ->
                receivedIntents += intent
            }
            withActivity {
                addOnNewIntentListener(listener)
                onNewIntent(Intent(this, SingleTopActivity::class.java).apply {
                    putExtra("newExtra", 5)
                })
            }

            assertWithMessage("Should have received one intent")
                .that(receivedIntents)
                .hasSize(1)
            val receivedIntent = receivedIntents.first()
            assertThat(receivedIntent.getIntExtra("newExtra", -1))
                .isEqualTo(5)
        }
    }

    @Test
    fun onNewIntentRemove() {
       withUse(ActivityScenario.launch(SingleTopActivity::class.java)) {
            val receivedIntents = mutableListOf<Intent>()

            val listener = Consumer<Intent> { intent ->
                receivedIntents += intent
            }
            withActivity {
                addOnNewIntentListener(listener)
                onNewIntent(Intent(this, SingleTopActivity::class.java).apply {
                    putExtra("newExtra", 5)
                })
            }

            assertWithMessage("Should have received one intent")
                .that(receivedIntents)
                .hasSize(1)
            val receivedIntent = receivedIntents.first()
            assertThat(receivedIntent.getIntExtra("newExtra", -1))
                .isEqualTo(5)

            withActivity {
                removeOnNewIntentListener(listener)
                onNewIntent(Intent(this, SingleTopActivity::class.java).apply {
                    putExtra("newExtra", 5)
                })
            }

            assertWithMessage("Should have received only one intent")
                .that(receivedIntents)
                .hasSize(1)
        }
    }

    @Test
    fun onNewIntentReentrant() {
       withUse(ActivityScenario.launch(SingleTopActivity::class.java)) {
            val activity = withActivity { this }
            val receivedIntents = mutableListOf<Intent>()

            val listener = object : Consumer<Intent> {
                override fun accept(value: Intent) {
                    receivedIntents += value
                    activity.removeOnNewIntentListener(this)
                }
            }
            withActivity {
                addOnNewIntentListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnNewIntentListener { }
                onNewIntent(Intent(this, SingleTopActivity::class.java).apply {
                    putExtra("newExtra", 5)
                })
                onNewIntent(Intent(this, SingleTopActivity::class.java).apply {
                    putExtra("newExtra", 10)
                })
            }

            // Only the first Intent should be received
            assertWithMessage("Should have received only one intent")
                .that(receivedIntents)
                .hasSize(1)
            val receivedIntent = receivedIntents.first()
            assertThat(receivedIntent.getIntExtra("newExtra", -1))
                .isEqualTo(5)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onMultiWindowModeChanged() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: MultiWindowModeChangedInfo

            val listener = Consumer<MultiWindowModeChangedInfo> { info ->
                receivedInfo = info
            }
            withActivity {
                addOnMultiWindowModeChangedListener(listener)
                onMultiWindowModeChanged(true)
            }

            assertThat(receivedInfo.isInMultiWindowMode).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onMultiWindowModeChangedWithConfig() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: MultiWindowModeChangedInfo
            var dispatchCount = 0

            val listener = Consumer<MultiWindowModeChangedInfo> { info ->
                receivedInfo = info
                dispatchCount++
            }
            lateinit var newConfig: Configuration
            withActivity {
                addOnMultiWindowModeChangedListener(listener)
                newConfig = Configuration(resources.configuration)
                onMultiWindowModeChanged(true, newConfig)
            }

            assertThat(receivedInfo.isInMultiWindowMode).isTrue()
            assertThat(receivedInfo.newConfig).isSameInstanceAs(newConfig)
            assertThat(dispatchCount).isEqualTo(1)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onMultiWindowModeChangedRemove() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: MultiWindowModeChangedInfo

            val listener = Consumer<MultiWindowModeChangedInfo> { info ->
                receivedInfo = info
            }
            withActivity {
                addOnMultiWindowModeChangedListener(listener)
                onMultiWindowModeChanged(true)
            }

            assertThat(receivedInfo.isInMultiWindowMode).isTrue()

            withActivity {
                removeOnMultiWindowModeChangedListener(listener)
                onMultiWindowModeChanged(false)
            }

            // Should still be true and not false
            assertThat(receivedInfo.isInMultiWindowMode).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onMultiWindowModeChangedRemoveReentrant() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            lateinit var receivedInfo: MultiWindowModeChangedInfo

            val listener = object : Consumer<MultiWindowModeChangedInfo> {
                override fun accept(value: MultiWindowModeChangedInfo) {
                    receivedInfo = value
                    activity.removeOnMultiWindowModeChangedListener(this)
                }
            }
            withActivity {
                addOnMultiWindowModeChangedListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnMultiWindowModeChangedListener { }
                onMultiWindowModeChanged(true)
                onMultiWindowModeChanged(false)
            }

            // Only the first multi-window mode change should be received
            assertThat(receivedInfo.isInMultiWindowMode).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onPictureInPictureModeChanged() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: PictureInPictureModeChangedInfo

            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                receivedInfo = info
            }
            withActivity {
                addOnPictureInPictureModeChangedListener(listener)
                onPictureInPictureModeChanged(true)
            }

            assertThat(receivedInfo.isInPictureInPictureMode).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onPictureInPictureModeChangedWithConfig() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: PictureInPictureModeChangedInfo
            var dispatchCount = 0

            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                receivedInfo = info
                dispatchCount++
            }
            lateinit var newConfig: Configuration
            withActivity {
                addOnPictureInPictureModeChangedListener(listener)
                newConfig = Configuration(resources.configuration)
                onPictureInPictureModeChanged(true, newConfig)
            }

            assertThat(receivedInfo.isInPictureInPictureMode).isTrue()
            assertThat(receivedInfo.newConfig).isSameInstanceAs(newConfig)
            assertThat(dispatchCount).isEqualTo(1)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onPictureInPictureModeChangedRemove() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            lateinit var receivedInfo: PictureInPictureModeChangedInfo

            val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
                receivedInfo = info
            }
            withActivity {
                addOnPictureInPictureModeChangedListener(listener)
                onPictureInPictureModeChanged(true)
            }

            assertThat(receivedInfo.isInPictureInPictureMode).isTrue()

            withActivity {
                removeOnPictureInPictureModeChangedListener(listener)
                onPictureInPictureModeChanged(false)
            }

            // Should still be true and not false
            assertThat(receivedInfo.isInPictureInPictureMode).isTrue()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onPictureInPictureModeChangedRemoveReentrant() {
       withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            lateinit var receivedInfo: PictureInPictureModeChangedInfo

            val listener = object : Consumer<PictureInPictureModeChangedInfo> {
                override fun accept(value: PictureInPictureModeChangedInfo) {
                    receivedInfo = value
                    activity.removeOnPictureInPictureModeChangedListener(this)
                }
            }
            withActivity {
                addOnPictureInPictureModeChangedListener(listener)
                // Add a second listener to force a ConcurrentModificationException
                // if not properly handled by ComponentActivity
                addOnPictureInPictureModeChangedListener { }
                onPictureInPictureModeChanged(true)
                onPictureInPictureModeChanged(false)
            }

            // Only the first picture-in-picture mode change should be received
            assertThat(receivedInfo.isInPictureInPictureMode).isTrue()
        }
    }

    @Test
    fun onUserLeaveHint() {
        withUse(ActivityScenario.launch(OnUserLeaveHintActivity::class.java)) {
            var receivedOnUserLeaveHint = false

            val listener = Runnable { receivedOnUserLeaveHint = true }

            withActivity {
                addOnUserLeaveHintListener(listener)
                onUserLeaveHint()
            }

            assertThat(receivedOnUserLeaveHint).isEqualTo(true)
        }
    }

    @Test
    fun onUserLeaveHintRemove() {
        withUse(ActivityScenario.launch(OnUserLeaveHintActivity::class.java)) {
            var receivedOnUserLeaveHintCount = 0

            val listener = Runnable { receivedOnUserLeaveHintCount++ }

            withActivity {
                addOnUserLeaveHintListener(listener)
                onUserLeaveHint()
            }

            assertThat(receivedOnUserLeaveHintCount).isEqualTo(1)

            withActivity {
                removeOnUserLeaveHintListener(listener)
                onUserLeaveHint()
            }

            // should still be 1
            assertThat(receivedOnUserLeaveHintCount).isEqualTo(1)
        }
    }
}

class SingleTopActivity : ComponentActivity() {
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}

class OnUserLeaveHintActivity : ComponentActivity() {
    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }
}
