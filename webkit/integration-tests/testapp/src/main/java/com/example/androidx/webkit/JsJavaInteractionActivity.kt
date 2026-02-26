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

class JsJavaInteractionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_js_java_interaction)
        setTitle(R.string.js_java_interaction_activity_title)
        setUpDemoAppActivity()

        findViewById<MenuListView>(R.id.js_java_interaction_list)
            .setItems(
                arrayOf(
                    MenuListView.MenuItem(
                        getResources().getString(R.string.web_message_listener_activity_title),
                        Intent(this, WebMessageListenerActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources()
                            .getString(
                                R.string.web_message_listener_malicious_website_activity_title
                            ),
                        Intent(this, WebMessageListenerMaliciousWebsiteActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.document_start_javascript_activity_title),
                        Intent(this, DocumentStartJavaScriptActivity::class.java),
                    ),
                    MenuListView.MenuItem(
                        getResources().getString(R.string.web_message_compat_activity_title),
                        Intent(this, WebMessageCompatActivity::class.java),
                    ),
                )
            )
    }
}
