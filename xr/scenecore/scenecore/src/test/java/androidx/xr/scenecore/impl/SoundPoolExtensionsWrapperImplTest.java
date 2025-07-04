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

import android.media.SoundPool;

import androidx.xr.runtime.internal.PointSourceParams;
import androidx.xr.runtime.internal.SoundFieldAttributes;
import androidx.xr.runtime.internal.SoundPoolExtensionsWrapper;
import androidx.xr.runtime.internal.SpatializerConstants;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.media.ShadowSoundPoolExtensions;
import com.android.extensions.xr.media.SoundPoolExtensions;
import com.android.extensions.xr.media.XrSpatialAudioExtensions;
import com.android.extensions.xr.node.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SoundPoolExtensionsWrapperImplTest {

    private static final int TEST_SOUND_ID = 0;
    private static final float TEST_VOLUME = 0F;
    private static final int TEST_PRIORITY = 0;
    private static final int TEST_LOOP = 0;
    private static final float TEST_RATE = 0F;

    XrExtensions mXrExtensions;
    XrSpatialAudioExtensions mSpatialAudioExtensions;
    SoundPoolExtensions mSoundPoolExtensions;

    @Before
    public void setUp() {
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        mSpatialAudioExtensions = mXrExtensions.getXrSpatialAudioExtensions();
        mSoundPoolExtensions = mSpatialAudioExtensions.getSoundPoolExtensions();
    }

    @Test
    public void playWithPointSource_callsExtensionsPlayWithPointSource() {
        int expected = 123;

        Node fakeNode = mXrExtensions.createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);
        PointSourceParams rtParams = new PointSourceParams(entity);

        SoundPool soundPool = new SoundPool.Builder().build();

        ShadowSoundPoolExtensions.extract(mSoundPoolExtensions)
                .setPlayAsPointSourceResult(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mSoundPoolExtensions);
        int actual =
                wrapper.play(
                        soundPool,
                        TEST_SOUND_ID,
                        rtParams,
                        TEST_VOLUME,
                        TEST_PRIORITY,
                        TEST_LOOP,
                        TEST_RATE);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void playWithSoundField_callsExtensionsPlayWithSoundField() {
        int expected = 312;

        SoundPool soundPool = new SoundPool.Builder().build();

        ShadowSoundPoolExtensions.extract(mSoundPoolExtensions).setPlayAsSoundFieldResult(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mSoundPoolExtensions);
        SoundFieldAttributes attributes =
                new SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);

        int actual =
                wrapper.play(
                        soundPool,
                        TEST_SOUND_ID,
                        attributes,
                        TEST_VOLUME,
                        TEST_PRIORITY,
                        TEST_LOOP,
                        TEST_RATE);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getSpatialSourceType_returnsFromExtensions() {
        int expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD;
        SoundPool soundPool = new SoundPool.Builder().build();

        ShadowSoundPoolExtensions.extract(mSoundPoolExtensions).setSourceType(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mSoundPoolExtensions);
        int actualSourceType = wrapper.getSpatialSourceType(soundPool, /* streamId= */ 0);
        assertThat(actualSourceType).isEqualTo(SpatializerConstants.SOURCE_TYPE_SOUND_FIELD);
    }
}
