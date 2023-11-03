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

package androidx.camera.testing.mocks.helpers;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Build;

import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ArgumentCaptorTest {
    private static final Object DUMMY_ARGUMENT_1 = new Object();
    private static final Object DUMMY_ARGUMENT_2 = new Object();

    private static final List<Object> DUMMY_ARGUMENTS_1 = Arrays.asList(DUMMY_ARGUMENT_1,
            new Object(), DUMMY_ARGUMENT_2, new Object());
    private static final List<Object> DUMMY_ARGUMENTS_2 = Arrays.asList(new Object(), new Object());

    private ArgumentCaptor<Object> mArgumentCaptor;

    @Before
    public void setUp() {
        mArgumentCaptor = new ArgumentCaptor<>();
    }

    @Test
    public void noArgumentProvided_getValueReturnsNull() {
        assertNull(mArgumentCaptor.getValue());
    }

    @Test
    public void argumentsProvided_getValueReturnsLastValue() {
        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_1);

        assertEquals(DUMMY_ARGUMENTS_1.get(DUMMY_ARGUMENTS_1.size() - 1),
                mArgumentCaptor.getValue());
    }

    @Test
    public void argumentsProvidedTwice_getValueReturnsLastValue() {
        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_1);
        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_2);

        assertEquals(DUMMY_ARGUMENTS_2.get(DUMMY_ARGUMENTS_2.size() - 1),
                mArgumentCaptor.getValue());
    }


    @Test
    public void argumentsProvidedWithMatcher_getValueReturnsLastMatchedValue() {
        mArgumentCaptor = new ArgumentCaptor<>(argument -> argument.equals(DUMMY_ARGUMENT_1)
                        || argument.equals(DUMMY_ARGUMENT_2));

        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_1);

        assertEquals(DUMMY_ARGUMENT_2, mArgumentCaptor.getValue());
    }

    @Test
    public void argumentsProvidedWithMatcher_getValueReturnsNullForNoMatch() {
        mArgumentCaptor = new ArgumentCaptor<>(argument -> false);

        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_1);

        assertNull(mArgumentCaptor.getValue());
    }

    @Test
    public void allArguments_returnsCorrectValues() {
        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_1);
        mArgumentCaptor.setArguments(DUMMY_ARGUMENTS_2);

        List<Object> expectedResult = new ArrayList<>();
        expectedResult.addAll(DUMMY_ARGUMENTS_1);
        expectedResult.addAll(DUMMY_ARGUMENTS_2);

        assertThat(mArgumentCaptor.getAllValues()).isEqualTo(expectedResult);
    }
}
