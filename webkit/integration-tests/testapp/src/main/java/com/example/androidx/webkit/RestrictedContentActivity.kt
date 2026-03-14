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

/** An {@link Activity} to exercise Restricted Content blocking functionality. */
class RestrictedContentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_restricted_content)
        setTitle(R.string.restricted_content_activity_title)
        setUpDemoAppActivity()

        findViewById<MenuListView>(R.id.restricted_content_list)
            .setItems(
                arrayOf(
                    MenuListView.MenuItem(
                        resources.getString(R.string.tiny_interstitial_activity_title),
                        Intent(this, TinyInterstitialActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.small_interstitial_activity_title),
                        Intent(this, SmallInterstitialActivity::class.java)
                            .putExtra(INTENT_EXTRA_CONTENT_TYPE, ContentType.RESTRICTED_CONTENT),
                    ),
                    MenuListView.MenuItem(
                        resources.getString(R.string.full_page_interstitial_activity_title),
                        Intent(this, FullPageInterstitialActivity::class.java)
                            .putExtra(INTENT_EXTRA_CONTENT_TYPE, ContentType.RESTRICTED_CONTENT),
                    ),
                )
            )
    }
}
