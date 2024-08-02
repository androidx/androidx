/*
 * Copyright 2024 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link Activity} to exercise Restricted Content blocking functionality.
 */
public class RestrictedContentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout();
    }

    private void setupLayout() {
        setContentView(R.layout.activity_restricted_content);
        setTitle(R.string.restricted_content_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.restricted_content_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.tiny_interstitial_activity_title),
                        new Intent(activityContext, TinyInterstitialActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.small_interstitial_activity_title),
                        new Intent(activityContext, SmallInterstitialActivity.class)
                                .putExtra(SmallInterstitialActivity.CONTENT_TYPE,
                                        ContentType.RESTRICTED_CONTENT)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.full_page_interstitial_activity_title),
                        new Intent(activityContext, FullPageInterstitialActivity.class)
                                .putExtra(FullPageInterstitialActivity.CONTENT_TYPE,
                                        ContentType.RESTRICTED_CONTENT)),
        };
        listView.setItems(menuItems);
    }
}
