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

package androidx.fragment.app.test

import android.content.Intent
import android.os.Bundle

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import java.util.concurrent.CountDownLatch

class NewIntentActivity : FragmentActivity() {
    val newIntent = CountDownLatch(1)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(FooFragment(), "derp")
                .commitNow()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Test a child fragment transaction -
        supportFragmentManager
            .findFragmentByTag("derp")!!
            .childFragmentManager
            .beginTransaction()
            .add(FooFragment(), "derp4")
            .commitNow()
        newIntent.countDown()
    }

    class FooFragment : Fragment()
}
