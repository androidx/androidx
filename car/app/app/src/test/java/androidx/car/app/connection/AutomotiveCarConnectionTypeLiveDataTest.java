/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.connection;

import static org.mockito.Mockito.verify;

import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link AutomotiveCarConnectionTypeLiveData}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AutomotiveCarConnectionTypeLiveDataTest {
    @Mock private Observer<Integer> mMockObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void observe_returnsNative() {
        new AutomotiveCarConnectionTypeLiveData().observeForever(mMockObserver);

        verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_NATIVE);
    }
}
