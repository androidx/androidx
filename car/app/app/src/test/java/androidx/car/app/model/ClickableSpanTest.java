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

package androidx.car.app.model;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
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

/** Tests for {@link ClickableSpan}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ClickableSpanTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private OnClickListener mOnClickListener;

    @Mock private OnDoneCallback mOnDoneCallback;

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void create_nullListener_throws() {
        ClickableSpan.create(null);
    }

    @Test
    public void create_withListener_returnsDelegate() {
        ClickableSpan span = ClickableSpan.create(mOnClickListener);

        span.getOnClickDelegate().sendClick(mOnDoneCallback);

        verify(mOnClickListener, times(1)).onClick();
    }

    @Test
    public void equals() {
        ClickableSpan span1 = ClickableSpan.create(mOnClickListener);
        ClickableSpan span2 = ClickableSpan.create(mOnClickListener);

        assertThat(span1).isEqualTo(span2);
    }

    @Test
    public void notEquals() {
        ClickableSpan span1 = ClickableSpan.create(mOnClickListener);
        ForegroundCarColorSpan span2 = ForegroundCarColorSpan.create(CarColor.BLUE);

        assertThat(span1).isNotEqualTo(span2);
    }
}
