/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.supportv7.widget

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.android.supportv7.R

class NestedScrollViewCoordinatorLayoutAppBarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nsv_cl_appbar_activity)

        val sb = StringBuilder()
        for (i in 1..100) {
            sb.append("jfdklsajfdklsajfdkslafdsafdsafdsafdsafdsafdsafdsafdsafdsafds\n\n\n\n\n")
        }
        findViewById<TextView>(R.id.appbarlayout_tv).text = sb.toString()
    }
}