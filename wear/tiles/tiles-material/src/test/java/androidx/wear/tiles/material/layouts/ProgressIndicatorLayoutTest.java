/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material.layouts;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.LayoutElementBuilders.Box;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.material.CircularProgressIndicator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
public class ProgressIndicatorLayoutTest {
    private final DeviceParameters mDeviceParameters =
            new DeviceParameters.Builder().setScreenWidthDp(192).setScreenHeightDp(192).build();

    @Test
    public void testAll() {
        LayoutElement content = new Box.Builder().build();
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        ProgressIndicatorLayout layout =
                new ProgressIndicatorLayout.Builder(mDeviceParameters)
                        .setContent(content)
                        .setProgressIndicatorContent(progressIndicator)
                        .build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(content.toLayoutElementProto());
        assertThat(layout.getProgressIndicatorContent()).isNotNull();
        assertThat(layout.getProgressIndicatorContent().toLayoutElementProto())
                .isEqualTo(progressIndicator.toLayoutElementProto());
    }

    @Test
    public void testContentOnly() {
        LayoutElement content = new Box.Builder().build();
        ProgressIndicatorLayout layout =
                new ProgressIndicatorLayout.Builder(mDeviceParameters).setContent(content).build();

        assertThat(layout.getContent()).isNotNull();
        assertThat(layout.getContent().toLayoutElementProto())
                .isEqualTo(content.toLayoutElementProto());
        assertThat(layout.getProgressIndicatorContent()).isNull();
    }

    @Test
    public void testIndicatorOnly() {
        CircularProgressIndicator progressIndicator =
                new CircularProgressIndicator.Builder().build();
        ProgressIndicatorLayout layout =
                new ProgressIndicatorLayout.Builder(mDeviceParameters)
                        .setProgressIndicatorContent(progressIndicator)
                        .build();

        assertThat(layout.getContent()).isNull();
        assertThat(layout.getProgressIndicatorContent()).isNotNull();
        assertThat(layout.getProgressIndicatorContent().toLayoutElementProto())
                .isEqualTo(progressIndicator.toLayoutElementProto());
    }

    @Test
    public void testEmpty() {
        ProgressIndicatorLayout layout =
                new ProgressIndicatorLayout.Builder(mDeviceParameters).build();

        assertThat(layout.getContent()).isNull();
        assertThat(layout.getProgressIndicatorContent()).isNull();
    }
}
