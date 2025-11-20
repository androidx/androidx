/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation.testapp

import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.ui.setupWithNavController
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.navigation.NavigationView

/**
 * Simple 'Help' activity that shows the data URI passed to it. In a real world app, it would load
 * the chosen help article, etc.
 */
class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        val fade = Fade()
        fade.excludeTarget(android.R.id.statusBarBackground, true)
        fade.excludeTarget(android.R.id.navigationBarBackground, true)
        window.exitTransition = fade
        window.enterTransition = fade

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.data).text = intent.data?.toString()

        val bottomToolbar = findViewById<Toolbar>(R.id.bottom_toolbar)
        bottomToolbar.navigationIcon = DrawerArrowDrawable(this)
        bottomToolbar.setNavigationOnClickListener {
            BottomSheetNavigationView().show(supportFragmentManager, "bottom")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
        return true
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in ComponentActivity")
    override fun onBackPressed() {
        super.onBackPressed()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}

class BottomSheetNavigationView : BottomSheetDialogFragment() {
    @Suppress("DEPRECATION")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val navigationView =
            requireActivity().layoutInflater.inflate(R.layout.bottom_bar_menu, container, false)
                as NavigationView

        // Add a fake Navigation Graph just to test out the behavior but not
        // actually navigate anywhere
        navigationView.setupWithNavController(
            NavController(requireContext()).apply {
                navigatorProvider.addNavigator(TestNavigator())
                graph =
                    createGraph(startDestination = R.id.launcher_home) {
                        test(R.id.launcher_home)
                        test(R.id.android)
                    }
            }
        )
        return navigationView
    }
}
