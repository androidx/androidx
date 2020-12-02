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
import androidx.car.app.WrappedRuntimeException;
import androidx.car.app.model.Toggle.OnCheckedChangeListener;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Tests for {@link Toggle}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ToggleTest {
    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Mock
    OnCheckedChangeListener mMockOnCheckedChangeListener;

    @Test
    public void build_withValues_notCheckedByDefault() {
        Toggle toggle = Toggle.builder(mMockOnCheckedChangeListener).build();
        assertThat(toggle.isChecked()).isFalse();
    }

    @Test
    @UiThreadTest
    public void build_checkedChange_sendsCheckedChangeCall() {
        Toggle toggle = Toggle.builder(mMockOnCheckedChangeListener).setChecked(true).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);

        toggle.getOnCheckedChangeListener().onCheckedChange(false, onDoneCallback);
        verify(mMockOnCheckedChangeListener).onCheckedChange(false);
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    @UiThreadTest
    public void build_checkedChange_sendsCheckedChangeCallWithFailure() {
        String testExceptionMessage = "Test exception";
        doThrow(new RuntimeException(testExceptionMessage)).when(
                mMockOnCheckedChangeListener).onCheckedChange(false);

        Toggle toggle = Toggle.builder(mMockOnCheckedChangeListener).setChecked(true).build();
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);

        try {
            toggle.getOnCheckedChangeListener().onCheckedChange(false, onDoneCallback);
        } catch (WrappedRuntimeException e) {
            assertThat(e.getMessage()).contains(testExceptionMessage);
        }
        verify(mMockOnCheckedChangeListener).onCheckedChange(false);
        verify(onDoneCallback).onFailure(any());
    }

    @Test
    public void equals() {
        Toggle toggle = Toggle.builder(mMockOnCheckedChangeListener).setChecked(true).build();
        assertThat(toggle)
                .isEqualTo(Toggle.builder(mMockOnCheckedChangeListener).setChecked(true).build());
    }

    @Test
    public void notEquals() {
        Toggle toggle = Toggle.builder(mMockOnCheckedChangeListener).setChecked(true).build();
        assertThat(toggle)
                .isNotEqualTo(Toggle.builder(mMockOnCheckedChangeListener).setChecked(
                        false).build());
    }
}
