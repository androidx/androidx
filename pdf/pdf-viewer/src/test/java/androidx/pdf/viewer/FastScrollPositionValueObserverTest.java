/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;

import androidx.pdf.widget.FastScrollView;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


@SmallTest
@RunWith(RobolectricTestRunner.class)
public class FastScrollPositionValueObserverTest {
    private final FastScrollView mMockFastScrollView = mock(FastScrollView.class);
    private final PageIndicator mMockPageIndicator = mock(PageIndicator.class);
    private final View mMockView = mock(View.class);

    @Test
    public void onChange() {
        when(mMockPageIndicator.getView()).thenReturn(mMockView);
        FastScrollPositionValueObserver fastScrollPositionValueObserver =
                new FastScrollPositionValueObserver(mMockFastScrollView, mMockPageIndicator);
        fastScrollPositionValueObserver.onChange(null, 100);
        verify(mMockPageIndicator).show();
        verify(mMockPageIndicator, times(2)).getView();
        verify(mMockFastScrollView).setVisible();
    }
}
