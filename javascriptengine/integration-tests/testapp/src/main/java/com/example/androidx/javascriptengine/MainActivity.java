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

package com.example.androidx.javascriptengine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An {@link Activity} for exercising various WebView functionality. This Activity is a {@link
 * ListView} which starts other Activities, each of which may similarly be a ListView, or may
 * actually exercise specific {@link android.webkit.WebView} features.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This process does not have a reason to load WebView. Moreover, we do this as a
        // check to ensure that `WebViewEvaluationActivity` is actually running on a different
        // process. If it was not the case, running `WebViewEvaluationActivity` creates a user
        // visible crash.
        WebView.disableWebView();

        setContentView(R.layout.activity_main);
        appendWebViewVersionToTitle(this);

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.top_level_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.webview_evaluation_activity_title),
                        new Intent(activityContext, WebViewEvaluationActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(
                                R.string.javascriptengine_evaluation_activity_title),
                        new Intent(activityContext, JavaScriptEngineEvaluationActivity.class)),
        };
        listView.setItems(menuItems);
    }

    /**
     * Inserts the {@link android.webkit.WebView} version in the current Activity title. This
     * assumes the title has already been set to something interesting, and we want to append the
     * WebView version to the end of the title.
     */
    private static void appendWebViewVersionToTitle(@NonNull Activity activity) {
        PackageInfo webViewPackage = WebViewCompat.getCurrentWebViewPackage(activity);

        final String webViewVersion = webViewPackage != null
                ? webViewPackage.versionName
                : activity.getResources().getString(R.string.not_updateable_webview);

        final String oldTitle = activity.getTitle().toString();
        final String newTitle = oldTitle + " (" + webViewVersion + ")";
        activity.setTitle(newTitle);
    }
}
