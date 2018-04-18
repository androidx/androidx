/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv7.app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.android.supportv7.R;

/**
 * Demonstrates how fragments can participate in the options menu.
 */
public class ActionBarFragmentMenu extends AppCompatActivity {
    MenuFragment mFragment1;
    Menu2Fragment mFragment2;
    CheckBox mCheckBox1;
    CheckBox mCheckBox2;
    CheckBox mCheckBox3;
    CheckBox mHasOptionsMenu;
    CheckBox mMenuVisibility;

    // Update fragment visibility when check boxes are changed.
    final OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            updateFragmentVisibility();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_bar_fragment_menu);

        // Make sure the two menu fragments are created.
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFragment1 = (MenuFragment)fm.findFragmentByTag("f1");
        if (mFragment1 == null) {
            mFragment1 = new MenuFragment();
            ft.add(mFragment1, "f1");
        }
        mFragment2 = (Menu2Fragment)fm.findFragmentByTag("f2");
        if (mFragment2 == null) {
            mFragment2 = new Menu2Fragment();
            ft.add(mFragment2, "f2");
        }
        ft.commit();

        // Watch check box clicks.
        mCheckBox1 = (CheckBox)findViewById(R.id.menu1);
        mCheckBox1.setOnClickListener(mClickListener);
        mCheckBox2 = (CheckBox)findViewById(R.id.menu2);
        mCheckBox2.setOnClickListener(mClickListener);
        mCheckBox3 = (CheckBox)findViewById(R.id.menu3);
        mCheckBox3.setOnClickListener(mClickListener);
        mHasOptionsMenu = (CheckBox)findViewById(R.id.has_options_menu);
        mHasOptionsMenu.setOnClickListener(mClickListener);
        mMenuVisibility = (CheckBox)findViewById(R.id.menu_visibility);
        mMenuVisibility.setOnClickListener(mClickListener);

        // Make sure fragments start out with correct visibility.
        updateFragmentVisibility();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Make sure fragments are updated after check box view state is restored.
        updateFragmentVisibility();
    }

    // Update fragment visibility based on current check box state.
    void updateFragmentVisibility() {
        // Update top level fragments.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mCheckBox1.isChecked()) ft.show(mFragment1);
        else ft.hide(mFragment1);
        if (mCheckBox2.isChecked()) ft.show(mFragment2);
        else ft.hide(mFragment2);
        ft.commit();

        mFragment1.setHasOptionsMenu(mHasOptionsMenu.isChecked());
        mFragment1.setMenuVisibility(mMenuVisibility.isChecked());
        mFragment2.setHasOptionsMenu(mHasOptionsMenu.isChecked());
        mFragment2.setMenuVisibility(mMenuVisibility.isChecked());

        // Update the nested fragment.
        if (mFragment2.mFragment3 != null) {
            ft = mFragment2.getFragmentManager().beginTransaction();
            if (mCheckBox3.isChecked()) ft.show(mFragment2.mFragment3);
            else ft.hide(mFragment2.mFragment3);
            ft.commit();

            mFragment2.mFragment3.setHasOptionsMenu(mHasOptionsMenu.isChecked());
            mFragment2.mFragment3.setMenuVisibility(mMenuVisibility.isChecked());
        }
    }

    /**
     * A fragment that displays a menu.  This fragment happens to not
     * have a UI (it does not implement onCreateView), but it could also
     * have one if it wanted.
     */
    public static class MenuFragment extends Fragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add("Menu 1a").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getTitle().equals("Menu 1a")) {
                Toast.makeText(getActivity(), "Selected Menu 1a.", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (item.getTitle().equals("Menu 1b")) {
                Toast.makeText(getActivity(), "Selected Menu 1b.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Second fragment with a menu.
     */
    public static class Menu2Fragment extends Fragment {
        Menu3Fragment mFragment3;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            FragmentManager fm = getChildFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mFragment3 = (Menu3Fragment)fm.findFragmentByTag("f3");
            if (mFragment3 == null) {
                mFragment3 = new Menu3Fragment();
                ft.add(mFragment3, "f3");
            }
            ft.commit();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add("Menu 2").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getTitle().equals("Menu 2")) {
                Toast.makeText(getActivity(), "Selected Menu 2.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }
    }

    /**
     * Third fragment with a menu.
     * This one is nested within the second.
     */
    public static class Menu3Fragment extends Fragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            Toast.makeText(getActivity(), "Created nested fragment's menu.",
                    Toast.LENGTH_SHORT).show();
            inflater.inflate(R.menu.display_options_actions, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public void onDestroyOptionsMenu() {
            Toast.makeText(getActivity(), "Destroyed nested fragment's menu.",
                    Toast.LENGTH_SHORT).show();
            super.onDestroyOptionsMenu();
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            Toast.makeText(getActivity(), "Prepared nested fragment's menu.",
                    Toast.LENGTH_SHORT).show();
            super.onPrepareOptionsMenu(menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.simple_item) {
                Toast.makeText(getActivity(), "Selected nested fragment's menu item.",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
