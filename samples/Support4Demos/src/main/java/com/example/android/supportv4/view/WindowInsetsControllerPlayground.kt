/*
 * Copyright 2020 The Android Open Source Project
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
package com.example.android.supportv4.view

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsControllerCompat
import com.example.android.supportv4.R
import kotlin.concurrent.thread

@SuppressLint("InlinedApi")
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class WindowInsetsControllerPlayground : Activity() {

    private val TAG = WindowInsetsControllerPlayground::class.java.name

    var currentType: Int? = null

    private lateinit var mRoot: View
    private lateinit var editText: EditText
    private lateinit var visibility: TextView
    private lateinit var checkbox: CheckBox
    private lateinit var buttonsRow: ViewGroup

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insets_controller)
        setActionBar(findViewById(R.id.toolbar))

        mRoot = findViewById(R.id.root)
        editText = findViewById(R.id.editText)
        visibility = findViewById(R.id.visibility)
        checkbox = findViewById(R.id.decorFitsSystemWindows)
        buttonsRow = findViewById(R.id.buttonRow)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.getWindowInsetsController(mRoot)!!.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        Log.e(
            TAG,
            "FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS: " + (
                window.attributes.flags and
                    FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS != 0
                )
        )

        checkbox.apply {
            isChecked = false
            setOnCheckedChangeListener { _, isChecked ->
                WindowCompat.setDecorFitsSystemWindows(window, isChecked)
            }
        }

        setupTypeSpinner()
        setupHideShowButtons()
        setupAppearanceButtons()

        ViewCompat.setOnApplyWindowInsetsListener(mRoot) { _: View?, insets: WindowInsetsCompat ->
            val systemBarInsets = insets.getInsets(
                ime() or
                    WindowInsetsCompat.Type.systemBars()
            )
            mRoot.setPadding(
                systemBarInsets.left,
                systemBarInsets.top,
                systemBarInsets.right,
                systemBarInsets.bottom
            )
            visibility.text =
                "Inset visibility: " + currentType?.let { insets.isVisible(it) }?.toString()

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupAppearanceButtons() {
        mapOf<String, (Boolean) -> Unit>(
            "LIGHT_NAV" to { isLight ->
                ViewCompat.getWindowInsetsController(mRoot)!!.isAppearanceLightNavigationBars =
                    isLight
            },
            "LIGHT_STAT" to { isLight ->
                ViewCompat.getWindowInsetsController(mRoot)!!.isAppearanceLightStatusBars = isLight
            },
        ).forEach { (name, callback) ->
            buttonsRow.addView(
                ToggleButton(this).apply {
                    text = name
                    textOn = text
                    textOff = text
                    setOnClickListener {
                        isChecked = true
                        callback(true)

                        it.postDelayed(
                            {
                                isChecked = false
                                callback(false)
                            },
                            2000
                        )
                    }
                }
            )
        }
    }

    private var visibilityThreadRunning = true

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        thread {
            visibilityThreadRunning = true
            while (visibilityThreadRunning) {
                visibility.post {
                    visibility.text = currentType?.let {
                        ViewCompat.getRootWindowInsets(mRoot)?.isVisible(it).toString()
                    } + " " + window.attributes.flags + " " + SystemClock.elapsedRealtime()
                }
                Thread.sleep(500)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        visibilityThreadRunning = false
    }

    private fun setupHideShowButtons() {
        findViewById<Button>(R.id.btn_show).apply {
            setOnClickListener { view ->
                currentType?.let { type ->
                    ViewCompat.getWindowInsetsController(view)?.show(type)
                }
            }
        }
        findViewById<Button>(R.id.btn_hide).apply {
            setOnClickListener {
                currentType?.let { type ->
                    ViewCompat.getWindowInsetsController(it)?.hide(type)
                }
            }
        }
    }

    private fun setupTypeSpinner() {
        val types = mapOf(
            "IME" to ime(),
            "Navigation" to WindowInsetsCompat.Type.navigationBars(),
            "System" to WindowInsetsCompat.Type.systemBars(),
            "Status" to WindowInsetsCompat.Type.statusBars()
        )
        findViewById<Spinner>(R.id.spn_insets_type).apply {
            adapter = ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_dropdown_item,
                types.keys.toTypedArray()
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (parent != null) {
                        currentType = types[parent.selectedItem]
                    }
                }
            }
        }
    }
}