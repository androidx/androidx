/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.os.Build;

import androidx.camera.core.Config.Option;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
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

        assertThat(bundle.retrieveOption(OPTION_1, VALUE_MISSING)).isSameAs(VALUE_1);
    }

    @Test
    public void canRemoveValue() {
        MutableOptionsBundle bundle = MutableOptionsBundle.create();
        bundle.insertOption(OPTION_1, VALUE_1);
        bundle.removeOption(OPTION_1);

        assertThat(bundle.retrieveOption(OPTION_1, VALUE_MISSING)).isSameAs(VALUE_MISSING);
    }

    @Test
    public void canCreateFromConfiguration_andAddMore() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, VALUE_1);
        mutOpts.insertOption(OPTION_1_A, VALUE_1_A);

        Config config = OptionsBundle.from(mutOpts);

        MutableOptionsBundle mutOpts2 = MutableOptionsBundle.from(config);
        mutOpts2.insertOption(OPTION_2, VALUE_2);

        Config config2 = OptionsBundle.from(mutOpts2);

        assertThat(config.listOptions()).containsExactly(OPTION_1, OPTION_1_A);
        assertThat(config2.listOptions()).containsExactly(OPTION_1, OPTION_1_A, OPTION_2);
    }
}
