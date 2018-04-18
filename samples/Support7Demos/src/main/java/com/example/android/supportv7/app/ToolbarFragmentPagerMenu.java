/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.android.supportv7.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how fragments can participate in the options menu from within a {@link ViewPager}.
 */
public class ToolbarFragmentPagerMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_fragment_pager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager vp = findViewById(R.id.viewpager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(),
                new MenuFragment(), new Menu2Fragment());
        vp.setAdapter(adapter);
    }

    private static class PagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments;

        public PagerAdapter(FragmentManager fm, Fragment... fragments) {
            super(fm);

            mFragments = new ArrayList<Fragment>();
            for (Fragment fragment : fragments) {
                mFragments.add(fragment);
            }
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
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
            menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(container.getContext());

            textView.setText(getClass().getSimpleName());
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return textView;
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
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add("Menu 2").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(container.getContext());

            textView.setText(getClass().getSimpleName());
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return textView;
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
}
