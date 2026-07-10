/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.pip

import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.PictureInPictureParamsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PictureInPictureDelegateTest {
    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun addOnPictureInPictureEventListener_stash() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val stashedState = Parcel.obtain()
            stashedState.writeBoolean(true /* isStashed */)
            stashedState.writeBoolean(false /* isTransitioningToPip */)
            stashedState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.STASHED)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(stashedState)
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun addOnPictureInPictureEventListener_orphanUnstash() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val unstashedState = Parcel.obtain()
            unstashedState.writeBoolean(false /* isStashed */)
            unstashedState.writeBoolean(false /* isTransitioningToPip */)
            unstashedState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        fail("No UNSTASHED without STASHED first")
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(unstashedState)
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun addOnPictureInPictureEventListener_stashThenUnstash() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val stashedState = Parcel.obtain()
            stashedState.writeBoolean(true /* isStashed */)
            stashedState.writeBoolean(false /* isTransitioningToPip */)
            stashedState.setDataPosition(0)

            val unstashedState = Parcel.obtain()
            unstashedState.writeBoolean(false /* isStashed */)
            unstashedState.writeBoolean(false /* isTransitioningToPip */)
            unstashedState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.UNSTASHED)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(stashedState)
                )
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(unstashedState)
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test
    fun addOnPictureInPictureEventListener_enterAnimationStart() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val transitionStartState = Parcel.obtain()
            transitionStartState.writeBoolean(false /* isStashed */)
            transitionStartState.writeBoolean(true /* isTransitioningToPip */)
            transitionStartState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event)
                            .isEqualTo(PictureInPictureDelegate.Event.ENTER_ANIMATION_START)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(transitionStartState)
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test
    fun addOnPictureInPictureEventListener_orphanEnterAnimationEnd() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val transitionEndState = Parcel.obtain()
            transitionEndState.writeBoolean(false /* isStashed */)
            transitionEndState.writeBoolean(false /* isTransitioningToPip */)
            transitionEndState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        fail("No ENTER_ANIMATION_END without ENTER_ANIMATION_START first")
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(transitionEndState)
                )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test
    fun addOnPictureInPictureEventListener_enterAnimationStartThenEnd() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val transitionStartState = Parcel.obtain()
            transitionStartState.writeBoolean(false /* isStashed */)
            transitionStartState.writeBoolean(true /* isTransitioningToPip */)
            transitionStartState.setDataPosition(0)

            val transitionEndState = Parcel.obtain()
            transitionEndState.writeBoolean(false /* isStashed */)
            transitionEndState.writeBoolean(false /* isTransitioningToPip */)
            transitionEndState.setDataPosition(0)

            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event)
                            .isEqualTo(PictureInPictureDelegate.Event.ENTER_ANIMATION_END)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(transitionStartState)
                )
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(transitionEndState)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun addOnPictureInPictureEventListener_enterPip() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.ENTERED)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                if (Build.VERSION.SDK_INT >= 26) {
                    onPictureInPictureModeChanged(true, Configuration(resources.configuration))
                } else {
                    onPictureInPictureModeChanged(true)
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun addOnPictureInPictureEventListener_enterPipWithConfig() {
        lateinit var pipConfiguration: Configuration

        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.ENTERED)
                        assertThat(config).isSameInstanceAs(pipConfiguration)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                pipConfiguration = Configuration(resources.configuration)
                onPictureInPictureModeChanged(true, pipConfiguration)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun addOnPictureInPictureEventListener_exitPip() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.EXITED)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                if (Build.VERSION.SDK_INT >= 26) {
                    onPictureInPictureModeChanged(false, Configuration(resources.configuration))
                } else {
                    onPictureInPictureModeChanged(false)
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun addOnPictureInPictureEventListener_exitPipWithConfig() {
        lateinit var pipConfiguration: Configuration

        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        assertThat(event).isEqualTo(PictureInPictureDelegate.Event.EXITED)
                        assertThat(config).isSameInstanceAs(pipConfiguration)
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                pipConfiguration = Configuration(resources.configuration)
                onPictureInPictureModeChanged(false, pipConfiguration)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun addOnPictureInPictureEventListener_stashThenExitThenStash() {
        lateinit var pipConfiguration: Configuration
        val stashedState = Parcel.obtain()
        stashedState.writeBoolean(true /* isStashed */)
        stashedState.writeBoolean(false /* isTransitioningToPip */)
        stashedState.setDataPosition(0)

        val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
            object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                override fun onPictureInPictureEvent(
                    event: PictureInPictureDelegate.Event,
                    config: Configuration?,
                ) {
                    assertThat(event).isEqualTo(PictureInPictureDelegate.Event.STASHED)
                }
            }

        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(stashedState)
                )
                pipConfiguration = Configuration(resources.configuration)
                onPictureInPictureModeChanged(false, pipConfiguration)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                onPictureInPictureUiStateChanged(
                    PictureInPictureUiState.CREATOR.createFromParcel(stashedState)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun removeOnPictureInPictureEventListener() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            val listener: PictureInPictureDelegate.OnPictureInPictureEventListener =
                object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                    override fun onPictureInPictureEvent(
                        event: PictureInPictureDelegate.Event,
                        config: Configuration?,
                    ) {
                        fail("Listener should have been removed")
                    }
                }
            withActivity {
                val delegate = PictureInPictureDelegate(this)
                delegate.addOnPictureInPictureEventListener(
                    Executors.newSingleThreadExecutor(),
                    listener,
                )
                delegate.removeOnPictureInPictureEventListener(listener)
                if (Build.VERSION.SDK_INT >= 26) {
                    onPictureInPictureModeChanged(true, Configuration(resources.configuration))
                } else {
                    onPictureInPictureModeChanged(true)
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onUserLeaveHint_enablePip() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            withActivity {
                assumeTrue(
                    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                )
                val delegate = PictureInPictureDelegate(this)
                delegate.setPictureInPictureParams(PictureInPictureParamsCompat(isEnabled = true))
                onUserLeaveHint()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    assertThat(explicitlyRequestedEnterPictureInPicture).isTrue()
                } else {
                    assertThat(explicitlyRequestedEnterPictureInPicture).isFalse()
                }
            }
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onUserLeaveHint_disablePip() {
        withUse(ActivityScenario.launch(PictureInPictureActivity::class.java)) {
            withActivity {
                assumeTrue(
                    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                )
                val delegate = PictureInPictureDelegate(this)
                delegate.setPictureInPictureParams(PictureInPictureParamsCompat(isEnabled = false))
                onUserLeaveHint()
                assertThat(explicitlyRequestedEnterPictureInPicture).isFalse()
            }
        }
    }
}

class PictureInPictureActivity : ComponentActivity() {
    var explicitlyRequestedEnterPictureInPicture: Boolean = false

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun enterPictureInPictureMode(params: PictureInPictureParams): Boolean {
        explicitlyRequestedEnterPictureInPicture = true
        return super.enterPictureInPictureMode(params)
    }
}
