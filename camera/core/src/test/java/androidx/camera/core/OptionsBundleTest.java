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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class OptionsBundleTest {

    private static final Option<Object> OPTION_1 = Option.create("option.1", Object.class);
    private static final Option<Object> OPTION_1_A = Option.create("option.1.a", Object.class);
    private static final Option<Object> OPTION_2 = Option.create("option.2", Object.class);
    private static final Option<List<Integer>> OPTION_INTEGER_LIST = Option.create(
            "option.int_list", List.class);
    private static final Option<Object> OPTION_MISSING =
            Option.create("option.missing", Object.class);

    private static final Object VALUE_1 = new Object();
    private static final Object VALUE_1_A = new Object();
    private static final Object VALUE_2 = new Object();
    private static final List<Integer> VALUE_INTEGER_LIST = new ArrayList<>();
    private static final Object VALUE_MISSING = new Object();

    static {
        VALUE_INTEGER_LIST.add(1);
        VALUE_INTEGER_LIST.add(2);
        VALUE_INTEGER_LIST.add(3);
    }


    private OptionsBundle mAllOpts;

    @Before
    public void setUp() {
        MutableOptionsBundle mutOpts = MutableOptionsBundle.create();
        mutOpts.insertOption(OPTION_1, VALUE_1);
        mutOpts.insertOption(OPTION_1_A, VALUE_1_A);
        mutOpts.insertOption(OPTION_2, VALUE_2);
        mutOpts.insertOption(OPTION_INTEGER_LIST, VALUE_INTEGER_LIST);

        mAllOpts = OptionsBundle.from(mutOpts);
    }

    @Test
    public void canRetrieveValue() {
        assertThat(mAllOpts.retrieveOption(OPTION_1)).isSameInstanceAs(VALUE_1);
        assertThat(mAllOpts.retrieveOption(OPTION_1_A)).isSameInstanceAs(VALUE_1_A);
        assertThat(mAllOpts.retrieveOption(OPTION_2)).isSameInstanceAs(VALUE_2);
    }

    @Test
    public void canRetrieveListOfIntegers() {
        List<Integer> list = mAllOpts.retrieveOption(OPTION_INTEGER_LIST);
        assertThat(list).isSameInstanceAs(VALUE_INTEGER_LIST);
    }

    @Test
    public void willReturnDefault_ifOptionIsMissing() {
        Object value = mAllOpts.retrieveOption(OPTION_MISSING, VALUE_MISSING);
        assertThat(value).isSameInstanceAs(VALUE_MISSING);
    }

    @Test
    public void willReturnStoredValue_whenGivenDefault() {
        Object value = mAllOpts.retrieveOption(OPTION_1, VALUE_MISSING);
        assertThat(value).isSameInstanceAs(VALUE_1);
    }

    @Test
    public void canListOptions() {
        Set<Option<?>> list = mAllOpts.listOptions();
        for (Option<?> opt : list) {
            assertThat(opt).isAnyOf(OPTION_1, OPTION_1_A, OPTION_2, OPTION_INTEGER_LIST);
        }

        assertThat(list).hasSize(4);
    }

    @Test
    public void canCreateCopyOptionsBundle() {
        OptionsBundle copyBundle = OptionsBundle.from(mAllOpts);

        assertThat(copyBundle.containsOption(OPTION_1)).isTrue();
        assertThat(copyBundle.containsOption(OPTION_1_A)).isTrue();
        assertThat(copyBundle.containsOption(OPTION_2)).isTrue();
    }

    @Test
    public void canFindPartialIds() {
        mAllOpts.findOptions(
                "option.1",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        assertThat(option).isAnyOf(OPTION_1, OPTION_1_A);
                        return true;
                    }
                });
    }

    @Test
    public void canStopSearchingAfterFirstMatch() {
        final AtomicInteger count = new AtomicInteger();
        mAllOpts.findOptions(
                "option",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        count.getAndIncrement();
                        return false;
                    }
                });

        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    public void canGetZeroResults_fromFind() {
        final AtomicInteger count = new AtomicInteger();
        mAllOpts.findOptions(
                "invalid_find_string",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        count.getAndIncrement();
                        return false;
                    }
                });

        assertThat(count.get()).isEqualTo(0);
    }

    @Test
    public void canRetrieveValue_fromFindLambda() {
        final AtomicReference<Object> value = new AtomicReference<>(VALUE_MISSING);
        mAllOpts.findOptions(
                "option.2",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Option<?> option) {
                        value.set(mAllOpts.retrieveOption(option));
                        return true;
                    }
                });

        assertThat(value.get()).isSameInstanceAs(VALUE_2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveMissingOption_willThrow() {
        // Should throw IllegalArgumentException
        mAllOpts.retrieveOption(OPTION_MISSING);
    }
}
