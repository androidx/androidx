/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setUpDemoAppActivity()

        findViewById<MenuListView>(R.id.top_level_list)
            .setItems(
                arrayOf(
                    MenuListView.MenuItem(
                        getResources().getString(R.string.safebrowsing_activity_title),
                        Intent(this, SafeBrowsingActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.proxy_override_activity_title),
                        Intent(this, ProxyOverrideActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.asset_loader_list_activity_title),
                        Intent(this, AssetLoaderListActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.tracing_controller_activity_title),
                        Intent(this, TracingControllerActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.renderer_termination_activity_title),
                        Intent(this, RendererTerminationActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.fullscreen_activity_title),
                        Intent(this, FullscreenActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.js_java_interaction_activity_title),
                        Intent(this, JsJavaInteractionActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.variations_header_activity_title),
                        Intent(this, GetVariationsHeaderActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.process_global_config_activity_title),
                        Intent(this, ProcessGlobalConfigActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.cookie_manager_activity_title),
                        Intent(this, CookieManagerActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.image_drag_drop_activity_title),
                        Intent(this, ImageDragActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.user_agent_metadata_activity_title),
                        Intent(this, UserAgentMetadataActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.multi_profile_activity_title),
                        Intent(this, MultiProfileTestActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.mute_audio_activity_title),
                        Intent(this, MuteAudioActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.restricted_content_activity_title),
                        Intent(this, RestrictedContentActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.async_startup_activity_title),
                        Intent(this, AsyncStartUpActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.default_trafficstats_tagging_activity),
                        Intent(this, DefaultTrafficStatsTaggingActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.web_storage_activity_title),
                        Intent(this, WebStorageCompatActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.payment_request_activity_title),
                        Intent(this, PaymentRequestActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.custom_header_activity_title),
                        Intent(this, CustomHeaderActivity::class.java),
                    ),
                )
            )
    }
}
