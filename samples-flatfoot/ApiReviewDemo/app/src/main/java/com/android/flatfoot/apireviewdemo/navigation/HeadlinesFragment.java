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

package com.android.flatfoot.apireviewdemo.navigation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.navigation.app.nav.Navigation;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.flatfoot.apireviewdemo.R;

/**
 * Fragment which shows a list of headlines and navigates to the chosen article
 */
public class HeadlinesFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.headlines_fragment, container, false);
        // TODO Switch to RecyclerView
        ListView listView = (ListView) view.findViewById(R.id.listView);
        // TODO Use Room for persistence
        final ArrayAdapter<Article> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                new Article[] { new Article(0, "Article 0"), new Article(1, "Article 1"),
                        new Article(2, "Article 2"), new Article(3, "Article 3"),
                        new Article(4, "Article 4"), new Article(5, "Article 5"),
                        new Article(6, "Article 6"), new Article(7, "Article 7"),
                        new Article(8, "Article 8"), new Article(9, "Article 9") });
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                args.putInt("articleId", adapter.getItem(position).mId);
                Navigation.findController(view).navigate(R.id.view_article, args);
            }
        });
        return view;
    }

    private class Article {
        private final int mId;
        private final String mTitle;

        Article(int id, String title) {
            mId = id;
            mTitle = title;
        }

        @Override
        public String toString() {
            return mTitle;
        }
    }
}
