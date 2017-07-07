/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.arch.persistence.room.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.Arrays.asList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(JUnit4.class)
public class StringUtilTest {
    @Test
    public void testEmpty() {
        assertThat(StringUtil.splitToIntList(""), is(Collections.<Integer>emptyList()));
        assertThat(StringUtil.joinIntoString(Collections.<Integer>emptyList()), is(""));
    }

    @Test
    public void testNull() {
        assertThat(StringUtil.splitToIntList(null), nullValue());
        assertThat(StringUtil.joinIntoString(null), nullValue());
    }

    @Test
    public void testSingle() {
        assertThat(StringUtil.splitToIntList("4"), is(asList(4)));
        assertThat(StringUtil.joinIntoString(asList(4)), is("4"));
    }

    @Test
    public void testMultiple() {
        assertThat(StringUtil.splitToIntList("4,5"), is(asList(4, 5)));
        assertThat(StringUtil.joinIntoString(asList(4, 5)), is("4,5"));
    }

    @Test
    public void testNegative() {
        assertThat(StringUtil.splitToIntList("-4,-5,6,-7"), is(asList(-4, -5, 6, -7)));
        assertThat(StringUtil.joinIntoString(asList(-4, -5, 6, -7)), is("-4,-5,6,-7"));
    }

    @Test
    public void ignoreMalformed() {
        assertThat(StringUtil.splitToIntList("-4,a,5,7"), is(asList(-4, 5, 7)));
    }
}
