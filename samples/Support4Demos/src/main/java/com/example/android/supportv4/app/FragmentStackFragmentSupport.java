/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.supportv4.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.android.supportv4.R;

public class FragmentStackFragmentSupport extends Fragment {
    int mStackLevel = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Do first time initialization -- add initial fragment.
            Fragment newFragment = FragmentStackSupport.CountingFragment.newInstance(mStackLevel);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.add(R.id.simple_fragment, newFragment).commit();
        } else {
            mStackLevel = savedInstanceState.getInt("level");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stack, container, false);

        // Watch for button clicks.
        Button button = (Button)v.findViewById(R.id.new_fragment);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addFragmentToStack();
            }
        });
        button = (Button)v.findViewById(R.id.delete_fragment);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getChildFragmentManager().popBackStack();
            }
        });
        button = (Button)v.findViewById(R.id.home);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // If there is a back stack, pop it all.
                FragmentManager fm = getChildFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(fm.getBackStackEntryAt(0).getId(),
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("level", mStackLevel);
    }

    void addFragmentToStack() {
        mStackLevel++;

        // Instantiate a new fragment.
        Fragment newFragment = FragmentStackSupport.CountingFragment.newInstance(mStackLevel);

        // Add the fragment to the activity, pushing this transaction
        // on to the back stack.
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.simple_fragment, newFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
    }
}
