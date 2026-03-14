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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.xr.scenecore.runtime.PointSourceParams;
import androidx.xr.scenecore.runtime.SoundFieldAttributes;
import androidx.xr.scenecore.runtime.SpatializerConstants;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.media.SpatializerExtensions;
import com.android.extensions.xr.node.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class MediaUtilsTest {

    @Test
    public void convertPointSourceParams_returnsExtensionsParams() {
        Node expected = XrExtensionsProvider.getXrExtensions().createNode();

        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(expected);
        PointSourceParams rtParams = new PointSourceParams();

        com.android.extensions.xr.media.PointSourceParams result =
                MediaUtils.convertPointSourceParamsToExtensions(rtParams, entity);

        assertThat(result.getNode()).isEqualTo(entity.getNode());
    }

    @Test
    public void convertSoundFieldAttributes_returnsExtensionsAttributes() {
        int extAmbisonicsOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER;

        SoundFieldAttributes rtAttributes =
                new SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);

        com.android.extensions.xr.media.SoundFieldAttributes result =
                MediaUtils.convertSoundFieldAttributesToExtensions(rtAttributes);

        assertThat(result.getAmbisonicsOrder()).isEqualTo(extAmbisonicsOrder);
    }

    @Test
    public void convertAmbisonicsOrderToExtensions_returnsExtensionsAmbisonicsOrder() {
        assertThat(
                        MediaUtils.convertAmbisonicsOrderToExtensions(
                                SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER))
                .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER);
        assertThat(
                        MediaUtils.convertAmbisonicsOrderToExtensions(
                                SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER))
                .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_SECOND_ORDER);
        assertThat(
                        MediaUtils.convertAmbisonicsOrderToExtensions(
                                SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER))
                .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER);
    }

    @Test
    public void convertAmbisonicsOrderToExtensions_throwsExceptionForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MediaUtils.convertAmbisonicsOrderToExtensions(100));
    }

    @Test
    public void convertExtensionsToSourceType_returnsRtSourceType() {
        assertThat(
                        MediaUtils.convertExtensionsToSourceType(
                                SpatializerExtensions.SOURCE_TYPE_DEFAULT))
                .isEqualTo(SpatializerConstants.SOURCE_TYPE_BYPASS);
        assertThat(
                        MediaUtils.convertExtensionsToSourceType(
                                SpatializerExtensions.SOURCE_TYPE_POINT_SOURCE))
                .isEqualTo(SpatializerConstants.SOURCE_TYPE_POINT_SOURCE);
        assertThat(
                        MediaUtils.convertExtensionsToSourceType(
                                SpatializerExtensions.SOURCE_TYPE_SOUND_FIELD))
                .isEqualTo(SpatializerConstants.SOURCE_TYPE_SOUND_FIELD);
    }

    @Test
    public void convertExtensionsToSourceType_throwsExceptionForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MediaUtils.convertExtensionsToSourceType(100));
    }
}
