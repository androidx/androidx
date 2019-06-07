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

package androidx.navigation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmbeddedXmlTest {

    @get:Rule
    var activityRule = ActivityTestRule(EmbeddedXmlActivity::class.java, false, false)

    @Test
    @Throws(Throwable::class)
    fun testRecreate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.context,
                EmbeddedXmlActivity::class.java)

        val activity = activityRule.launchActivity(intent)
        instrumentation.waitForIdleSync()
        activityRule.runOnUiThread { activity.recreate() }
    }
}

/**
 * Test Navigation Activity that dynamically adds the [NavHostFragment].
 *
 *
 * You must call [NavController.setGraph]
 * to set the appropriate graph for your test.
 */
class EmbeddedXmlActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.embedded_xml_activity)

        if (savedInstanceState == null) {
            val embeddedFragment = EmbeddedXmlFragment()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, embeddedFragment)
                    .setPrimaryNavigationFragment(embeddedFragment)
                    .commit()
        }
    }
}

class EmbeddedXmlFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.navigation_activity, container, false)
    }
}
