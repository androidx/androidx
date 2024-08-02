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

package com.example.androidx.webkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link Activity} for exercising various WebView functionality. This Activity is a {@link
 * ListView} which starts other Activities, each of which may similarly be a ListView, or may
 * actually exercise specific {@link android.webkit.WebView} features.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.top_level_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.safebrowsing_activity_title),
                        new Intent(activityContext, SafeBrowsingActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.proxy_override_activity_title),
                        new Intent(activityContext, ProxyOverrideActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.asset_loader_list_activity_title),
                        new Intent(activityContext, AssetLoaderListActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.force_dark_activity_title),
                        new Intent(activityContext, ForceDarkActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.force_dark_strategy_activity_title),
                        new Intent(activityContext, ForceDarkStrategyActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.multi_process_enabled_activity_title),
                        new Intent(activityContext, MultiProcessEnabledActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.tracing_controller_activity_title),
                        new Intent(activityContext, TracingControllerActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.renderer_termination_activity_title),
                        new Intent(activityContext, RendererTerminationActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.fullscreen_activity_title),
                        new Intent(activityContext, FullscreenActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.js_java_interaction_activity_title),
                        new Intent(activityContext, JsJavaInteractionActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.variations_header_activity_title),
                        new Intent(activityContext, GetVariationsHeaderActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.process_global_config_activity_title),
                        new Intent(activityContext, ProcessGlobalConfigActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.requested_with_activity_title),
                        new Intent(activityContext, RequestedWithHeaderActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.cookie_manager_activity_title),
                        new Intent(activityContext, CookieManagerActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.image_drag_drop_activity_title),
                        new Intent(activityContext, ImageDragActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.user_agent_metadata_activity_title),
                        new Intent(activityContext, UserAgentMetadataActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.multi_profile_activity_title),
                        new Intent(activityContext, MultiProfileTestActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.mute_audio_activity_title),
                        new Intent(activityContext, MuteAudioActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.restricted_content_activity_title),
                        new Intent(activityContext, RestrictedContentActivity.class)),
        };
        listView.setItems(menuItems);
    }
}
