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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Counts the number of onCreateView, onHiddenChanged (onHide, onShow), onAttach, and onDetach
 * calls.
 */
public class CountCallsFragment extends StrictViewFragment {
    public int onCreateViewCount = 0;
    public int onDestroyViewCount = 0;
    public int onHideCount = 0;
    public int onShowCount = 0;
    public int onAttachCount = 0;
    public int onDetachCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        onCreateViewCount++;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            onHideCount++;
        } else {
            onShowCount++;
        }
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onAttach(Context context) {
        onAttachCount++;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        onDetachCount++;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        onDestroyViewCount++;
        super.onDestroyView();
    }
}
