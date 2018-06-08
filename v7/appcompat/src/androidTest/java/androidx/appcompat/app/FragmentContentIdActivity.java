package androidx.appcompat.app;
/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.fragment.app.Fragment;


public class FragmentContentIdActivity extends BaseTestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new FragmentA())
                .commit();
    }

    @Override
    protected int getContentViewLayoutResId() {
        // We don't want to set a layout
        return 0;
    }

    public void replaceWithFragmentB() {
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new FragmentB())
                .commit();
    }

    public static class FragmentA extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View view = new View(getContext());
            view.setId(R.id.fragment_a);
            return view;
        }
    }

    public static class FragmentB extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            View view = new View(getContext());
            view.setId(R.id.fragment_b);
            return view;
        }
    }

}



