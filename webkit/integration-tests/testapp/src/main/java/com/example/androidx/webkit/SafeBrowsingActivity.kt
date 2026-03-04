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

/** An {@link Activity} to exercise Safe Browsing functionality. */
class SafeBrowsingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_safe_browsing)
        setTitle(R.string.safebrowsing_activity_title)
        setUpDemoAppActivity()

        findViewById<MenuListView>(R.id.safe_browsing_list)
            .setItems(
                arrayOf(
                    MenuListView.MenuItem(
                        resources.getString(R.string.small_interstitial_activity_title),
                        Intent(this, SmallInterstitialActivity::class.java)
                            .putExtra(
                                SmallInterstitialActivity.CONTENT_TYPE,
                                ContentType.MALICIOUS_CONTENT,
                            ),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.medium_wide_interstitial_activity_title),
                        Intent(this, MediumInterstitialActivity::class.java)
                            .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, false),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.medium_tall_interstitial_activity_title),
                        Intent(this, MediumInterstitialActivity::class.java)
                            .putExtra(MediumInterstitialActivity.LAYOUT_HORIZONTAL, false),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.loud_interstitial_activity_title),
                        Intent(this, FullPageInterstitialActivity::class.java)
                            .putExtra(
                                FullPageInterstitialActivity.CONTENT_TYPE,
                                ContentType.MALICIOUS_CONTENT,
                            ),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.giant_interstitial_activity_title),
                        Intent(this, GiantInterstitialActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.per_web_view_enable_activity_title),
                        Intent(this, PerWebViewEnableActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.invisible_activity_title),
                        Intent(this, InvisibleActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.unattached_activity_title),
                        Intent(this, UnattachedActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.custom_interstitial_activity_title),
                        Intent(this, CustomInterstitialActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.allowlist_activity_title),
                        Intent(this, AllowlistActivity::class.java),
                    ),
                )
            )
    }
}
