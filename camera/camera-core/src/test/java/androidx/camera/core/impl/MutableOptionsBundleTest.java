/*
 * Copyright 2019 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.impl.Config.Option;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MutableOptionsBundleTest {
    private static final Option<Object> OPTION_1 = Option.create("option.1", Object.class);
    private static final Option<Object> OPTION_1_A = Option.create("option.1.a", Object.class);
    private static final Option<Object> OPTION_2 = Option.create("option.2", Object.class);

    private static final Object VALUE_1 = new Object();
    private static final Object VALUE_1_A = new Object();
    private static final Object VALUE_2 = new Object();
    private static final Object VALUE_MISSING = new Object();

    @Test
    public void canCreateEmptyBundle() {
        MutableOptionsBundle bundle = MutableOptionsBundle.create();
        assertThat(bundle).isNotNull();
    }

    @Test
    public void canAddValue() {
        MutableOptionsBundle bundle = MutableOptionsBundle.create();
        bundle.insertOption(OPTION_1, VALUE_1);

        assertThat(bundle.retrieveOption(OPTION_1, VALUE_MISSING)).isSameInstanceAs(VALUE_1);
    }

    @Test
    public void canRemoveValue() {
        MutableOptionsBundle bundle = MutableOptionsBundle.create();
        bundle.insertOption(OPTION_1, VALUE_1);
        bundle.removeOption(OPTION_1);

        assertThat(bundle.retrieveOption(OPTION_1, VALUE_MISSING)).isSameInstanceAs(VALUE_MISSING);
    }

    @Test
    public void canSetNullValue() {
        MutableOptionsBundle bundle = MutableOptionsBundle.create();
        bundle.insertOption(OPTION_1, null);

        assertThat(bundle.retrieveOption(OPTION_1)).isNull();
    }

    @Test
    public void canCreateFromConfiguration_andAddMore() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1_A);
        mutOpts.insertOption(OPTION_1_A, VALUE_1_A);

        Config config = OptionsBundle.from(mutOpts);

        MutableOptionsBundle mutOpts2 = MutableOptionsBundle.from(config);
        mutOpts2.insertOption(OPTION_2, REQUIRED, VALUE_2);
        mutOpts2.insertOption(OPTION_2, OPTIONAL, VALUE_1);

        Config config2 = OptionsBundle.from(mutOpts2);

        assertThat(config.listOptions()).containsExactly(OPTION_1, OPTION_1_A);
        assertThat(config2.listOptions()).containsExactly(OPTION_1, OPTION_1_A, OPTION_2);

        assertThat(config2.getPriorities(OPTION_1))
                .containsExactly(REQUIRED, ALWAYS_OVERRIDE, OPTIONAL);
        assertThat(config2.retrieveOptionWithPriority(OPTION_1, REQUIRED)).isEqualTo(VALUE_2);
        assertThat(config2.retrieveOptionWithPriority(OPTION_1, ALWAYS_OVERRIDE)).isEqualTo(
                VALUE_1);
        assertThat(config2.retrieveOptionWithPriority(OPTION_1, OPTIONAL)).isEqualTo(VALUE_1_A);
        assertThat(config2.getPriorities(OPTION_2))
                .containsExactly(REQUIRED, OPTIONAL);
        assertThat(config2.retrieveOptionWithPriority(OPTION_2, REQUIRED)).isEqualTo(VALUE_2);
        assertThat(config2.retrieveOptionWithPriority(OPTION_2, OPTIONAL)).isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_ALWAYSOVERRIDE_ALWAYSOVERRIDE() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_2);
    }

    @Test
    public void insertOption_valueIdentical_ALWAYSOVERRIDE_ALWAYSOVERRIDE() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_ALWAYSOVERRIDE_REQUIRED() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_ALWAYSOVERRIDE_OPTIONAL() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_REQUIRED_ALWAYSOVERRIDE() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_2);
    }

    @Test
    public void insertOption_valueIdentical_REQUIRED_REQUIRED() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(REQUIRED);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertOption_valueDifferent_REQUIRED_REQUIRED() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);
        // should throw an Error
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);
    }

    @Test
    public void insertOption_REQUIRED_OPTIONAL() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(REQUIRED);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_OPTIONAL_ALWAYSOVERRIDE() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_2);
    }

    @Test
    public void insertOption_OPTIONAL_REQUIRED() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1);
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(REQUIRED);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_2);
    }

    @Test
    public void insertOption_OPTIONAL_OPTIONAL() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_2);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(OPTIONAL);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_2);
    }

    @Test
    public void insertOption_valueIdentical_OPTIONAL_OPTIONAL() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1);
        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1);

        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_1);
        Config.OptionPriority highestPriority = Collections.min(mutOpts.getPriorities(OPTION_1));
        assertThat(highestPriority).isEqualTo(OPTIONAL);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, highestPriority))
                .isEqualTo(VALUE_1);
    }

    @Test
    public void insertOption_multiplePriorities() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();

        mutOpts.insertOption(OPTION_1, OPTIONAL, VALUE_1);
        mutOpts.insertOption(OPTION_1, ALWAYS_OVERRIDE, VALUE_2);
        mutOpts.insertOption(OPTION_1, REQUIRED, VALUE_1_A);

        mutOpts.insertOption(OPTION_2, OPTIONAL, VALUE_1);
        mutOpts.insertOption(OPTION_2, REQUIRED, VALUE_2);

        assertThat(mutOpts.getPriorities(OPTION_1))
                .containsExactly(OPTIONAL, ALWAYS_OVERRIDE, REQUIRED);
        assertThat(mutOpts.retrieveOption(OPTION_1)).isEqualTo(VALUE_2);
        assertThat(mutOpts.getOptionPriority(OPTION_1)).isEqualTo(ALWAYS_OVERRIDE);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, OPTIONAL)).isEqualTo(VALUE_1);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, REQUIRED)).isEqualTo(VALUE_1_A);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_1, ALWAYS_OVERRIDE))
                .isEqualTo(VALUE_2);

        assertThat(mutOpts.getPriorities(OPTION_2)).containsExactly(OPTIONAL, REQUIRED);
        assertThat(mutOpts.retrieveOption(OPTION_2)).isEqualTo(VALUE_2);
        assertThat(mutOpts.getOptionPriority(OPTION_2)).isEqualTo(REQUIRED);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_2, OPTIONAL)).isEqualTo(VALUE_1);
        assertThat(mutOpts.retrieveOptionWithPriority(OPTION_2, REQUIRED)).isEqualTo(VALUE_2);
    }
}
