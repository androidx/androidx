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

/**
 * An {@link Activity} for exercising various WebView functionality. This Activity is a {@link
 * ListView} which starts other Activities, each of which may similarly be a ListView, or may
 * actually exercise specific {@link android.webkit.WebView} features.
 */
class AssetLoaderListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_asset_loader_list)
        setTitle(R.string.asset_loader_list_activity_title)
        setUpDemoAppActivity()

        findViewById<MenuListView>(R.id.asset_loader_list)
            .setItems(
                arrayOf(
                    MenuListView.MenuItem(
                        getResources().getString(R.string.asset_loader_simple_activity_title),
                        Intent(this, AssetLoaderSimpleActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.asset_loader_ajax_activity_title),
                        Intent(this, AssetLoaderAjaxActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources()
                            .getString(R.string.asset_loader_internal_storage_activity_title),
                        Intent(this, AssetLoaderInternalStorageActivity::class.java),
                    ),
                )
            )
    }
}
