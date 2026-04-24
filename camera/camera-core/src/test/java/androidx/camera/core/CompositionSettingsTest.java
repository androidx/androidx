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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Color;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CompositionSettingsTest {

    @Test
    public void defaultValuesAreCorrect() {
        CompositionSettings settings = new CompositionSettings.Builder().build();
        assertThat(settings.getAlpha()).isEqualTo(1.0f);
        assertThat(settings.getOffset().first).isEqualTo(0.0f);
        assertThat(settings.getOffset().second).isEqualTo(0.0f);
        assertThat(settings.getScale().first).isEqualTo(1.0f);
        assertThat(settings.getScale().second).isEqualTo(1.0f);
        assertThat(settings.getRoundedCornerRatio()).isEqualTo(0.0f);
        assertThat(settings.getBorderWidthRatio()).isEqualTo(0.0f);
        assertThat(settings.getBorderColor()).isEqualTo(Color.WHITE);
        assertThat(settings.getZOrder()).isEqualTo(0);
    }

    @Test
    public void canSetAlpha() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setAlpha(0.5f)
                .build();
        assertThat(settings.getAlpha()).isEqualTo(0.5f);
    }

    @Test
    public void canSetOffset() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setOffset(0.1f, 0.2f)
                .build();
        assertThat(settings.getOffset().first).isEqualTo(0.1f);
        assertThat(settings.getOffset().second).isEqualTo(0.2f);
    }

    @Test
    public void canSetScale() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setScale(0.3f, 0.4f)
                .build();
        assertThat(settings.getScale().first).isEqualTo(0.3f);
        assertThat(settings.getScale().second).isEqualTo(0.4f);
    }

    @Test
    public void canSetCornerRadiusRatio() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setRoundedCornerRatio(0.5f)
                .build();
        assertThat(settings.getRoundedCornerRatio()).isEqualTo(0.5f);
    }

    @Test
    public void canSetBorderWidthRatio() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setBorderWidthRatio(0.1f)
                .build();
        assertThat(settings.getBorderWidthRatio()).isEqualTo(0.1f);
    }

    @Test
    public void canSetBorderColor() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setBorderColor(Color.RED)
                .build();
        assertThat(settings.getBorderColor()).isEqualTo(Color.RED);
    }

    @Test
    public void canSetZOrder() {
        CompositionSettings settings = new CompositionSettings.Builder()
                .setZOrder(10)
                .build();
        assertThat(settings.getZOrder()).isEqualTo(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setZOrderWithNegativeValue_throwsException() {
        new CompositionSettings.Builder().setZOrder(-1);
    }

    @Test
    public void staticDefaultIsCorrect() {
        CompositionSettings settings = CompositionSettings.DEFAULT;
        assertThat(settings.getAlpha()).isEqualTo(1.0f);
        assertThat(settings.getOffset().first).isEqualTo(0.0f);
        assertThat(settings.getOffset().second).isEqualTo(0.0f);
        assertThat(settings.getScale().first).isEqualTo(1.0f);
        assertThat(settings.getScale().second).isEqualTo(1.0f);
        assertThat(settings.getRoundedCornerRatio()).isEqualTo(0.0f);
        assertThat(settings.getBorderWidthRatio()).isEqualTo(0.0f);
        assertThat(settings.getBorderColor()).isEqualTo(Color.WHITE);
        assertThat(settings.getZOrder()).isEqualTo(0);
    }
}
