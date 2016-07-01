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

import com.example.android.supportv4.R;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

/**
 * Demonstrates how fragments can participate in the options menu.
 */
public class FragmentMenuFragmentSupport extends Fragment {
    Fragment mFragment1;
    Fragment mFragment2;
    CheckBox mCheckBox1;
    CheckBox mCheckBox2;

    // Update fragment visibility when check boxes are changed.
    final OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            updateFragmentVisibility();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_menu, container, false);

        // Make sure the two menu fragments are created.
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFragment1 = fm.findFragmentByTag("f1");
        if (mFragment1 == null) {
            mFragment1 = new FragmentMenuSupport.MenuFragment();
            ft.add(mFragment1, "f1");
        }
        mFragment2 = fm.findFragmentByTag("f2");
        if (mFragment2 == null) {
            mFragment2 = new FragmentMenuSupport.Menu2Fragment();
            ft.add(mFragment2, "f2");
        }
        ft.commit();
        
        // Watch check box clicks.
        mCheckBox1 = (CheckBox)v.findViewById(R.id.menu1);
        mCheckBox1.setOnClickListener(mClickListener);
        mCheckBox2 = (CheckBox)v.findViewById(R.id.menu2);
        mCheckBox2.setOnClickListener(mClickListener);
        
        // Make sure fragments start out with correct visibility.
        updateFragmentVisibility();

        return v;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Make sure fragments are updated after check box view state is restored.
        updateFragmentVisibility();
    }

    // Update fragment visibility based on current check box state.
    void updateFragmentVisibility() {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (mCheckBox1.isChecked()) ft.show(mFragment1);
        else ft.hide(mFragment1);
        if (mCheckBox2.isChecked()) ft.show(mFragment2);
        else ft.hide(mFragment2);
        ft.commit();
    }
}
