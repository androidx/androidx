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


package androidx.fragment.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.test.R;

public class StrictViewFragment extends StrictFragment {
    boolean mOnCreateViewCalled, mOnViewCreatedCalled, mOnDestroyViewCalled;
    int mLayoutId = R.layout.strict_view_fragment;

    public void setLayoutId(int layoutId) {
        mLayoutId = layoutId;
    }

    public static StrictViewFragment create(int layoutId) {
        StrictViewFragment fragment = new StrictViewFragment();
        fragment.mLayoutId = layoutId;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        checkGetActivity();
        checkState("onCreateView", CREATED);
        final View result = inflater.inflate(mLayoutId, container, false);
        mOnCreateViewCalled = true;
        return result;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (view == null) {
            throw new IllegalArgumentException("onViewCreated view argument should not be null");
        }
        checkGetActivity();
        checkState("onViewCreated", CREATED);
        mOnViewCreatedCalled = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getView() == null) {
            throw new IllegalStateException("getView returned null in onDestroyView");
        }
        checkGetActivity();
        checkState("onDestroyView", CREATED);
        mOnDestroyViewCalled = true;
    }
}
