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

package androidx.recyclerview.selection.testing;

import static org.junit.Assert.assertEquals;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.Resettable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import java.util.ArrayList;
import java.util.List;

public final class TestOnItemTouchListener implements OnItemTouchListener, Resettable {

    private final List<MotionEvent> mOnInterceptTouchEventCalls = new ArrayList<>();
    private final List<MotionEvent> mOnTouchEventCalls = new ArrayList<>();
    private boolean mConsumeEvents;

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        mOnInterceptTouchEventCalls.add(e);
        return mConsumeEvents;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        mOnTouchEventCalls.add(e);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public void consumeEvents(boolean enabled) {
        mConsumeEvents = enabled;
    }

    public void assertOnInterceptTouchEventCalled(int expectedTimesCalled) {
        assertEquals(expectedTimesCalled, mOnInterceptTouchEventCalls.size());
    }

    public void assertOnTouchEventCalled(int expectedTimesCalled) {
        assertEquals(expectedTimesCalled, mOnTouchEventCalls.size());
    }

    @Override
    public boolean isResetRequired() {
        return !mOnInterceptTouchEventCalls.isEmpty()
                || !mOnTouchEventCalls.isEmpty()
                || mConsumeEvents;
    }

    @Override
    public void reset() {
        mOnInterceptTouchEventCalls.clear();
        mOnTouchEventCalls.clear();
        mConsumeEvents = false;
    }
}
