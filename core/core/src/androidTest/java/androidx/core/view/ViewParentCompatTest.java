/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.core.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.view.View;
import android.view.ViewParent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class ViewParentCompatTest {

    private View mView;

    @Before
    public void setup() {
        mView = new View(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void  dispatchNestedScroll_viewIsNestedScrollingParent3_callsCorrectMethod() {
        final NestedScrollingParent3Impl nestedScrollingParent3Impl =
                mock(NestedScrollingParent3Impl.class);

        ViewParentCompat.onNestedScroll(
                nestedScrollingParent3Impl,
                mView,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{6, 7});

        verify(nestedScrollingParent3Impl).onNestedScroll(
                mView,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{6, 7});
        verify(nestedScrollingParent3Impl, never()).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt());
        verify(nestedScrollingParent3Impl, never()).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt());
    }

    @Test
    public void  dispatchNestedScroll_viewIsNestedScrollingParent2_callsCorrectMethod() {
        final NestedScrollingParent2Impl nestedScrollingParent2Impl =
                mock(NestedScrollingParent2Impl.class);

        ViewParentCompat.onNestedScroll(
                nestedScrollingParent2Impl,
                mView,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{6, 7});

        verify(nestedScrollingParent2Impl).onNestedScroll(
                mView,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH);
        verify(nestedScrollingParent2Impl, never()).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt());
    }

    @Test
    public void  dispatchNestedScroll_viewIsNscTouchTypeNotTouch_callsNothing() {
        final NestedScrollingParentImpl nestedScrollingParentImpl =
                mock(NestedScrollingParentImpl.class);

        ViewParentCompat.onNestedScroll(
                nestedScrollingParentImpl,
                mView,
                11,
                2,
                3,
                4,
                ViewCompat.TYPE_NON_TOUCH,
                new int[]{6, 7});

        verify(nestedScrollingParentImpl, never()).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt());
    }

    @Test
    public void  dispatchNestedScroll_viewIsNotASupportNestedScrollingParent_callsCorrectMethod() {
        final ViewParentImpl viewParentImpl = mock(ViewParentImpl.class);

        ViewParentCompat.onNestedScroll(
                viewParentImpl,
                mView,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{5, 6});

        verify(viewParentImpl).onNestedScroll(
                mView,
                1,
                2,
                3,
                4);
    }

    public abstract static class ViewParentImpl implements ViewParent {

    }

    public abstract static class NestedScrollingParentImpl extends ViewParentImpl
            implements NestedScrollingParent{

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed) {
        }
    }

    public abstract static class NestedScrollingParent2Impl extends NestedScrollingParentImpl
            implements NestedScrollingParent2 {

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }
    }

    public abstract static class NestedScrollingParent3Impl extends NestedScrollingParent2Impl
            implements NestedScrollingParent3 {

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, int @Nullable [] consumed) {
        }
    }
}
