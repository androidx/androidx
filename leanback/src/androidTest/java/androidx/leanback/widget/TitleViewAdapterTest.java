/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TitleViewAdapterTest {


    public static class CustomTitle extends LinearLayout implements TitleViewAdapter.Provider {

        final View mSearchOrbView;

        public CustomTitle(Context context, AttributeSet set) {
            this(context, set, 0);
        }

        public CustomTitle(Context context, AttributeSet set, int s) {
            super(context, set, s);
            mSearchOrbView = new View(context);
            addView(mSearchOrbView, 10, 10);
        }

        TitleViewAdapter mTitleViewAdapter = new TitleViewAdapter() {
            @Override
            public View getSearchAffordanceView() {
                return mSearchOrbView;
            }
        };

        @Override
        public TitleViewAdapter getTitleViewAdapter() {
            return mTitleViewAdapter;
        }
    }

    @Test
    public void customTitle() {
        CustomTitle t = new CustomTitle(InstrumentationRegistry.getTargetContext(), null);
        TitleViewAdapter adapter = t.getTitleViewAdapter();
        adapter.setTitle("title");
        adapter.setBadgeDrawable(new GradientDrawable());
        View.OnClickListener listener = Mockito.mock(View.OnClickListener.class);
        adapter.setOnSearchClickedListener(listener);
        adapter.getSearchAffordanceView().performClick();
        Mockito.verify(listener).onClick(Mockito.any(View.class));
    }
}
