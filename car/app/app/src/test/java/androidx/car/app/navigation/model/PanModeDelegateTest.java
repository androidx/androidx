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

package androidx.car.app.navigation.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link androidx.car.app.model.PanModeDelegateImpl}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PanModeDelegateTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    PanModeListener mMockPanModeListener;

    @Test
    public void create() {
        PanModeDelegate delegate = PanModeDelegateImpl.create(mMockPanModeListener);

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        delegate.sendPanModeChanged(true, onDoneCallback);
        verify(mMockPanModeListener).onPanModeChanged(true);
        verify(onDoneCallback).onSuccess(null);
    }
}
