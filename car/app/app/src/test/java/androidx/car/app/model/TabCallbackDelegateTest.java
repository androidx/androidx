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

package androidx.car.app.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

/** Tests for {@link TabCallbackDelegateImpl}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TabCallbackDelegateTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    TabTemplate.TabCallback mMockTabCallback;

    @Test
    public void sendTabSelected() {
        TabCallbackDelegate delegate = TabCallbackDelegateImpl.create(mMockTabCallback);

        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        delegate.sendTabSelected("MOCK_TAB_ID", onDoneCallback);
        verify(mMockTabCallback).onTabSelected("MOCK_TAB_ID");
        verify(onDoneCallback).onSuccess(null);
        verify(onDoneCallback, never()).onFailure(any());
    }
}
