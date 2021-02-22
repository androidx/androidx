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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.model.Toggle.OnCheckedChangeListener;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link Toggle}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ToggleTest {
    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    OnCheckedChangeListener mMockOnCheckedChangeListener;

    @Test
    public void build_withValues_notCheckedByDefault() {
        Toggle toggle = new Toggle.Builder(mMockOnCheckedChangeListener).build();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    public void build_checkedChange_sendsCheckedChangeCall() {
        Toggle toggle = new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(true).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);

        toggle.getOnCheckedChangeDelegate().sendCheckedChange(false, onDoneCallback);
        verify(mMockOnCheckedChangeListener).onCheckedChange(false);
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void build_checkedChange_sendsCheckedChangeCallWithFailure() {
        String testExceptionMessage = "Test exception";
        doThrow(new RuntimeException(testExceptionMessage)).when(
                mMockOnCheckedChangeListener).onCheckedChange(false);

        Toggle toggle = new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(true).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);

        try {
            toggle.getOnCheckedChangeDelegate().sendCheckedChange(false, onDoneCallback);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains(testExceptionMessage);
        }
        verify(mMockOnCheckedChangeListener).onCheckedChange(false);
        verify(onDoneCallback).onFailure(any());
    }

    @Test
    public void equals() {
        Toggle toggle = new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(true).build();
        assertThat(toggle)
                .isEqualTo(new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(
                        true).build());
    }

    @Test
    public void notEquals() {
        Toggle toggle = new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(true).build();
        assertThat(toggle)
                .isNotEqualTo(new Toggle.Builder(mMockOnCheckedChangeListener).setChecked(
                        false).build());
    }
}
