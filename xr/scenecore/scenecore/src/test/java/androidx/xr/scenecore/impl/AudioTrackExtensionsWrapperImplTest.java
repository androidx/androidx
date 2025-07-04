/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.media.AudioTrack;

import androidx.xr.runtime.internal.AudioTrackExtensionsWrapper;
import androidx.xr.runtime.internal.PointSourceParams;
import androidx.xr.runtime.internal.SoundFieldAttributes;
import androidx.xr.runtime.internal.SpatializerConstants;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.media.AudioTrackExtensions;
import com.android.extensions.xr.media.ShadowAudioTrackExtensions;
import com.android.extensions.xr.media.SpatializerExtensions;
import com.android.extensions.xr.media.XrSpatialAudioExtensions;
import com.android.extensions.xr.node.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioTrackExtensionsWrapperImplTest {

    XrExtensions mXrExtensions;
    XrSpatialAudioExtensions mSpatialAudioExtensions;
    AudioTrackExtensions mAudioTrackExtensions;

    private EntityManager mEntityManager;

    @Mock private AudioTrack.Builder mBuilder;

    @Before
    public void setUp() {
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        mSpatialAudioExtensions = mXrExtensions.getXrSpatialAudioExtensions();
        mAudioTrackExtensions = mSpatialAudioExtensions.getAudioTrackExtensions();

        // Clear the sound fields before each test.
        // Because the mAudioTrackExtensions are fetched from the XrExtensions singleton it is
        // reused
        // across tests.
        // TODO(b/401557718): Consider adding a reset method to the XrExtensions shadow.
        ShadowAudioTrackExtensions.extract(mAudioTrackExtensions).setSoundFieldAttributes(null);

        mEntityManager = new EntityManager();
    }

    @Test
    public void setPointSourceParams_callsExtensionsSetPointSourceParams() {
        AudioTrack track = mock(AudioTrack.class);

        Node fakeNode = mXrExtensions.createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);

        PointSourceParams expectedRtParams = new PointSourceParams(entity);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);
        wrapper.setPointSourceParams(track, expectedRtParams);

        assertThat(mAudioTrackExtensions.getPointSourceParams(track).getNode()).isEqualTo(fakeNode);
    }

    @Test
    public void setPointSourceParamsBuilder_callsExtensionsSetPointSourceParamsBuilder() {
        AudioTrack track = mock(AudioTrack.class);
        Node fakeNode = mXrExtensions.createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);

        PointSourceParams expectedRtParams = new PointSourceParams(entity);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);
        AudioTrack.Builder actual = wrapper.setPointSourceParams(mBuilder, expectedRtParams);

        assertThat(actual).isEqualTo(mBuilder);
        assertThat(mAudioTrackExtensions.getPointSourceParams(track).getNode()).isEqualTo(fakeNode);
    }

    @Test
    public void setSoundFieldAttr_callsExtensionsSetSoundFieldAttr() {
        int expectedAmbisonicOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER;
        SoundFieldAttributes expectedRtAttr =
                new SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        AudioTrack.Builder actual = wrapper.setSoundFieldAttributes(mBuilder, expectedRtAttr);

        assertThat(actual).isEqualTo(mBuilder);
        assertThat(
                        mAudioTrackExtensions
                                .getSoundFieldAttributes(mock(AudioTrack.class))
                                .getAmbisonicsOrder())
                .isEqualTo(expectedAmbisonicOrder);
    }

    @Test
    public void getPointSourceParams_callsExtensionsGetPointSourceParams() {
        AudioTrack track = mock(AudioTrack.class);

        Node fakeNode = mXrExtensions.createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);
        mEntityManager.setEntityForNode(fakeNode, entity);

        AudioTrack.Builder unused =
                mAudioTrackExtensions.setPointSourceParams(
                        new AudioTrack.Builder(),
                        new com.android.extensions.xr.media.PointSourceParams.Builder()
                                .setNode(fakeNode)
                                .build());

        PointSourceParams expectedRtParams = new PointSourceParams(entity);
        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        PointSourceParams actual = wrapper.getPointSourceParams(track);

        assertThat(actual.getEntity()).isEqualTo(expectedRtParams.getEntity());
    }

    @Test
    public void getPointSourceParams_returnsNullIfNotInExtensions() {
        AudioTrack track = mock(AudioTrack.class);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        PointSourceParams actual = wrapper.getPointSourceParams(track);

        assertThat(actual).isNull();
    }

    @Test
    public void getSoundFieldAttributes_callsExtensionsGetSoundFieldAttributes() {
        AudioTrack track = mock(AudioTrack.class);

        AudioTrack.Builder unused =
                mAudioTrackExtensions.setSoundFieldAttributes(
                        new AudioTrack.Builder(),
                        new com.android.extensions.xr.media.SoundFieldAttributes.Builder()
                                .setAmbisonicsOrder(
                                        SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER)
                                .build());

        SoundFieldAttributes expectedRtAttr =
                new SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);
        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        SoundFieldAttributes actual = wrapper.getSoundFieldAttributes(track);

        assertThat(actual.getAmbisonicsOrder()).isEqualTo(expectedRtAttr.getAmbisonicsOrder());
    }

    @Test
    public void getSoundFieldAttributes_returnsNullIfNotInExtensions() {
        AudioTrack track = mock(AudioTrack.class);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        SoundFieldAttributes actual = wrapper.getSoundFieldAttributes(track);

        assertThat(actual).isNull();
    }

    @Test
    public void getSourceType_returnsFromExtensions() {
        AudioTrack track = mock(AudioTrack.class);

        int expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD;
        ShadowAudioTrackExtensions.extract(mAudioTrackExtensions).setSourceType(expected);

        AudioTrackExtensionsWrapper wrapper =
                new AudioTrackExtensionsWrapperImpl(mAudioTrackExtensions, mEntityManager);

        int actualSourceType = wrapper.getSpatialSourceType(track);
        assertThat(actualSourceType).isEqualTo(expected);
    }
}
