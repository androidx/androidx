/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.test.R;

public class PageFragment extends Fragment {
    private static final String ARG_VALUE = "value";

    private int mValue;
    EventListener mOnAttachListener = EventListener.NO_OP;
    EventListener mOnDestroyListener = EventListener.NO_OP;

    static PageFragment create(int value) {
        PageFragment result = new PageFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_VALUE, value);
        result.setArguments(args);
        return result;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.item_test_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle data = savedInstanceState != null ? savedInstanceState : getArguments();
        updateValue(data.getInt(ARG_VALUE));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mOnAttachListener.onEvent(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(ARG_VALUE, mValue);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOnDestroyListener.onEvent(this);
    }

    void updateValue(int newValue) {
        mValue = newValue;
        ((TextView) getView()).setText(String.valueOf(newValue));
    }

    public interface EventListener {
        void onEvent(PageFragment fragment);

        EventListener NO_OP = new EventListener() {
            @Override
            public void onEvent(PageFragment fragment) {
                // do nothing
            }
        };
    }
}
