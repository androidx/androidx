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

package androidx.xr.scenecore.spatial.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.media.MediaPlayer;

import androidx.xr.scenecore.runtime.MediaPlayerExtensionsWrapper;
import androidx.xr.scenecore.runtime.PointSourceParams;
import androidx.xr.scenecore.runtime.SoundFieldAttributes;
import androidx.xr.scenecore.runtime.SpatializerConstants;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.media.MediaPlayerExtensions;
import com.android.extensions.xr.media.ShadowMediaPlayerExtensions;
import com.android.extensions.xr.media.SpatializerExtensions;
import com.android.extensions.xr.media.XrSpatialAudioExtensions;
import com.android.extensions.xr.node.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class MediaPlayerExtensionsWrapperImplTest {
    XrExtensions mXrExtensions;
    XrSpatialAudioExtensions mSpatialAudioExtensions;
    MediaPlayerExtensions mMediaPlayerExtensions;

    @Before
    public void setUp() {
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        mSpatialAudioExtensions = mXrExtensions.getXrSpatialAudioExtensions();
        mMediaPlayerExtensions = mSpatialAudioExtensions.getMediaPlayerExtensions();
    }

    @Test
    public void setPointSourceParams_callsExtensionsSetPointSourceParams() {
        MediaPlayer mediaPlayer = new MediaPlayer();

        Node fakeNode = mXrExtensions.createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);

        PointSourceParams expectedRtParams = new PointSourceParams();

        MediaPlayerExtensionsWrapper wrapper =
                new MediaPlayerExtensionsWrapperImpl(mMediaPlayerExtensions);
        wrapper.setPointSourceParams(mediaPlayer, expectedRtParams, entity);

        assertThat(
                        ShadowMediaPlayerExtensions.extract(mMediaPlayerExtensions)
                                .getPointSourceParams()
                                .getNode())
                .isEqualTo(fakeNode);
    }

    @Test
    public void setSoundFieldAttr_callsExtensionsSetSoundFieldAttr() {
        MediaPlayer mediaPlayer = new MediaPlayer();

        int expectedAmbisonicOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER;
        SoundFieldAttributes expectedRtAttr =
                new SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);

        MediaPlayerExtensionsWrapper wrapper =
                new MediaPlayerExtensionsWrapperImpl(mMediaPlayerExtensions);
        wrapper.setSoundFieldAttributes(mediaPlayer, expectedRtAttr);

        assertThat(
                        ShadowMediaPlayerExtensions.extract(mMediaPlayerExtensions)
                                .getSoundFieldAttributes()
                                .getAmbisonicsOrder())
                .isEqualTo(expectedAmbisonicOrder);
    }
}
