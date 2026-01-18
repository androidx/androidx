/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.app.PictureInPictureParamsCompat
import androidx.core.app.PictureInPictureProvider
import androidx.core.app.PictureInPictureUiStateCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class PictureInPictureImplTest {

    private lateinit var provider: FakePictureInPictureProvider

    @Before
    fun setUp() {
        provider = FakePictureInPictureProvider()
    }

    @Test
    fun basicImpl_setAspectRatio() {
        val aspectRatio = Rational(16, 9)
        val impl = BasicPictureInPicture(provider)
        impl.setAspectRatio(aspectRatio)
        assertThat(provider.receivedParams!!.aspectRatio).isEqualTo(aspectRatio)
    }

    @Test
    fun basicImpl_setEnabled() {
        val impl = BasicPictureInPicture(provider)
        impl.setEnabled(false)
        assertThat(provider.receivedParams!!.isEnabled).isFalse()
        impl.setEnabled(true)
        assertThat(provider.receivedParams!!.isEnabled).isTrue()
    }

    @Test
    fun basicImpl_defaults() {
        BasicPictureInPicture(provider)
        val params = provider.receivedParams
        assertThat(params!!.isSeamlessResizeEnabled).isFalse()
    }

    @Test
    fun basicImpl_setActions() {
        val actions = createFakeActions()
        val impl = BasicPictureInPicture(provider)
        impl.setActions(actions)
        assertThat(provider.receivedParams!!.actions).isEqualTo(actions)
    }

    @Test
    fun videoPlaybackImpl_defaults() {
        VideoPlaybackPictureInPicture(provider)
        val params = provider.receivedParams
        assertThat(params!!.isSeamlessResizeEnabled).isTrue()
    }

    private fun createFakeActions(): List<RemoteAction> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent("TEST_ACTION")
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val action =
            RemoteAction(
                Icon.createWithContentUri("content://uri"),
                "title",
                "description",
                pendingIntent,
            )
        return listOf(action)
    }
}

private class FakePictureInPictureProvider : PictureInPictureProvider {
    var receivedParams: PictureInPictureParamsCompat? = null
        private set

    override fun setPictureInPictureParams(params: PictureInPictureParamsCompat) {
        this.receivedParams = params
    }

    override fun enterPictureInPictureMode(params: PictureInPictureParamsCompat) {}

    override fun addOnPictureInPictureUiStateChangedListener(
        listener: androidx.core.util.Consumer<PictureInPictureUiStateCompat>
    ) {}

    override fun removeOnPictureInPictureUiStateChangedListener(
        listener: androidx.core.util.Consumer<PictureInPictureUiStateCompat>
    ) {}

    override fun addOnUserLeaveHintListener(listener: Runnable) {}

    override fun removeOnUserLeaveHintListener(listener: Runnable) {}

    override fun addOnPictureInPictureModeChangedListener(
        listener: androidx.core.util.Consumer<PictureInPictureModeChangedInfo>
    ) {}

    override fun removeOnPictureInPictureModeChangedListener(
        listener: androidx.core.util.Consumer<PictureInPictureModeChangedInfo>
    ) {}
}
