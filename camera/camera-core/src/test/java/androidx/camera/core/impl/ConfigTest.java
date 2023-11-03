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

package androidx.camera.core.impl;

import static androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE;
import static androidx.camera.core.impl.Config.OptionPriority.OPTIONAL;
import static androidx.camera.core.impl.Config.OptionPriority.REQUIRED;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionFilter;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ConfigTest {
    private static final String OPTION_ID = "OptionID";
    private static final String KEY = "Key";

    @Test
    public void configCreateWithIdAndClass() {
        Config.Option<Integer> option = Config.Option.create(OPTION_ID, Integer.class);
        assertThat(option.getId()).isEqualTo(OPTION_ID);
        assertThat(option.getValueClass()).isEqualTo(Integer.class);
        assertThat(option.getToken()).isNull();
    }

    @Test
    public void configCreateWithIdAndClassAndKey() {
        Config.Option<Integer> option = Config.Option.create(OPTION_ID, Integer.class, KEY);
        assertThat(option.getId()).isEqualTo(OPTION_ID);
        assertThat(option.getValueClass()).isEqualTo(Integer.class);
        assertThat(option.getToken()).isEqualTo(KEY);
    }

    @Test
    public void optionPriorityIsDeclaredCorrectly() {
        Set<Config.OptionPriority> set = new HashSet<>();
        set.add(ALWAYS_OVERRIDE);
        set.add(OPTIONAL);
        set.add(REQUIRED);

        assertThat(Collections.min(set)).isEqualTo(ALWAYS_OVERRIDE);

        set.clear();
        set.add(ALWAYS_OVERRIDE);
        set.add(REQUIRED);
        assertThat(Collections.min(set)).isEqualTo(ALWAYS_OVERRIDE);

        set.clear();
        set.add(OPTIONAL);
        set.add(REQUIRED);
        assertThat(Collections.min(set)).isEqualTo(REQUIRED);

        set.clear();
        set.add(OPTIONAL);
        set.add(ALWAYS_OVERRIDE);
        assertThat(Collections.min(set)).isEqualTo(ALWAYS_OVERRIDE);
    }

    @Test
    public void noConflict_whenTwoValueAreALWAYSOVERRIDE() {
        assertThat(Config.hasConflict(ALWAYS_OVERRIDE, ALWAYS_OVERRIDE)).isFalse();
    }

    @Test
    public void hasConflict_whenTwoValueAreREQUIRED() {
        assertThat(Config.hasConflict(REQUIRED, REQUIRED)).isTrue();
    }

    @Test
    public void noConflict_whenTwoValueAreOPTIONAL() {
        assertThat(Config.hasConflict(OPTIONAL, OPTIONAL)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreOPTIONAL_REQUIRED() {
        assertThat(Config.hasConflict(OPTIONAL, REQUIRED)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreOPTIONAL_ALWAYSOVERRIDE() {
        assertThat(Config.hasConflict(OPTIONAL, ALWAYS_OVERRIDE)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreREQUIRED_OPTIONAL() {
        assertThat(Config.hasConflict(REQUIRED, OPTIONAL)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreREQUIRED_ALWAYSOVERRIDE() {
        assertThat(Config.hasConflict(REQUIRED, ALWAYS_OVERRIDE)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreALWAYSREQUIRED_OPTIONAL() {
        assertThat(Config.hasConflict(ALWAYS_OVERRIDE, OPTIONAL)).isFalse();
    }

    @Test
    public void noConflict_whenTwoValueAreALWAYSOVERRIDE_REQUIRED() {
        assertThat(Config.hasConflict(ALWAYS_OVERRIDE, REQUIRED)).isFalse();
    }

    @Test
    public void overrideResolutionSelectorCorrectly() {
        MutableOptionsBundle mergedConfig = MutableOptionsBundle.create();
        MutableConfig baseConfig = new FakeUseCaseConfig.Builder().getMutableConfig();
        baseConfig.insertOption(OPTION_RESOLUTION_SELECTOR,
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                                ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build());
        MutableConfig extendedConfig = new FakeUseCaseConfig.Builder().getMutableConfig();
        ResolutionFilter resolutionFilter = (supportedSizes, rotationDegrees) -> null;
        extendedConfig.insertOption(OPTION_RESOLUTION_SELECTOR,
                new ResolutionSelector.Builder().setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionFilter(resolutionFilter).build());
        Config.mergeOptionValue(mergedConfig, baseConfig, extendedConfig,
                OPTION_RESOLUTION_SELECTOR);

        ResolutionSelector mergedResolutionSelector =
                mergedConfig.retrieveOption(OPTION_RESOLUTION_SELECTOR);

        assertThat(mergedResolutionSelector.getAspectRatioStrategy()).isEqualTo(
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY);
        assertThat(mergedResolutionSelector.getResolutionStrategy()).isEqualTo(
                ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY);
        assertThat(mergedResolutionSelector.getResolutionFilter()).isEqualTo(resolutionFilter);
    }

}
