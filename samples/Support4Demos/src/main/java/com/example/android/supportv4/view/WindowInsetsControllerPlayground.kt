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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationControlListenerCompat
import androidx.core.view.WindowInsetsAnimationControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import com.example.android.supportv4.R
import java.util.ArrayList
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@SuppressLint("InlinedApi")
@RequiresApi(21)
class WindowInsetsControllerPlayground : Activity() {

    private val TAG: String = "WindowInsets_Playground"

    val mTransitions = ArrayList<Transition>()
    var currentType: Int? = null

    private lateinit var mRoot: View
    private lateinit var editRow: ViewGroup
    private lateinit var visibility: TextView
    private lateinit var buttonsRow: ViewGroup
    private lateinit var fitSystemWindow: CheckBox
    private lateinit var isDecorView: CheckBox
    internal lateinit var info: TextView
    lateinit var graph: View

    val values = mutableListOf(0f)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insets_controller)
        setActionBar(findViewById(R.id.toolbar))

        mRoot = findViewById(R.id.root)
        editRow = findViewById(R.id.editRow)
        visibility = findViewById(R.id.visibility)
        buttonsRow = findViewById(R.id.buttonRow)
        info = findViewById(R.id.info)
        fitSystemWindow = findViewById(R.id.decorFitsSystemWindows)
        isDecorView = findViewById(R.id.isDecorView)
        addPlot()

        WindowCompat.setDecorFitsSystemWindows(window, fitSystemWindow.isChecked)
        ViewCompat.getWindowInsetsController(mRoot)!!.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        Log.e(
            TAG,
            "FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS: " + (
                window.attributes.flags and
                    FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS != 0
                )
        )

        fitSystemWindow.apply {
            isChecked = false
            setOnCheckedChangeListener { _, isChecked ->
                WindowCompat.setDecorFitsSystemWindows(window, isChecked)
            }
        }

        mTransitions.add(Transition(findViewById(R.id.scrollView)))
        mTransitions.add(Transition(editRow))

        setupTypeSpinner()
        setupHideShowButtons()
        setupAppearanceButtons()

        ViewCompat.setOnApplyWindowInsetsListener(mRoot) { _: View?, insets: WindowInsetsCompat ->
            val systemBarInsets = insets.getInsets(ime() or systemBars())
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

        setupIMEAnimation()
        setupActionButton()

        isDecorView.setOnCheckedChangeListener { _, _ ->
            setupIMEAnimation()
        }
    }

    private fun addPlot() {
        val stroke = 20
        val p2 = Paint()
        p2.color = Color.RED
        p2.strokeWidth = 1f
        p2.style = Paint.Style.FILL

        graph = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val mx = (values.maxOrNull() ?: 0f) + 1
                val mn = values.minOrNull() ?: 0f
                val ct = values.size.toFloat()

                val h = height - stroke * 2
                val w = width - stroke * 2
                values.forEachIndexed { i, f ->
                    val x = (i / ct) * w + stroke
                    val y = ((f - mn) / (mx - mn)) * h + stroke
                    canvas.drawCircle(x, y, stroke.toFloat(), p2)
                }
            }
        }
        graph.minimumWidth = 300
        graph.minimumHeight = 100
        graph.setBackgroundColor(Color.GRAY)
        val linearLayout = info.parent as LinearLayout
        linearLayout.addView(
            graph, linearLayout.indexOfChild(isDecorView),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
        )
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

    private fun setupActionButton() {
        findViewById<View>(R.id.floating_action_button).setOnClickListener { v: View? ->
            ViewCompat.getWindowInsetsController(v!!)!!.controlWindowInsetsAnimation(
                ime(), -1, LinearInterpolator(), null /* cancellationSignal */,
                object : WindowInsetsAnimationControlListenerCompat {
                    override fun onReady(
                        controller: WindowInsetsAnimationControllerCompat,
                        types: Int
                    ) {
                        val anim =
                            ValueAnimator.ofFloat(0f, 1f)
                        anim.duration = 1500
                        anim.addUpdateListener { animation: ValueAnimator ->
                            controller.setInsetsAndAlpha(
                                controller.shownStateInsets,
                                animation.animatedValue as Float,
                                anim.animatedFraction
                            )
                        }
                        anim.addListener(
                            object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    super.onAnimationEnd(animation)
                                    controller.finish(true)
                                }
                            })
                        anim.start()
                    }

                    override fun onCancelled(
                        controller: WindowInsetsAnimationControllerCompat?
                    ) {
                    }

                    override fun onFinished(
                        controller: WindowInsetsAnimationControllerCompat
                    ) {
                    }
                }
            )
        }
    }

    private fun setupIMEAnimation() {
        mRoot.setOnTouchListener(createOnTouchListener())
        if (isDecorView.isChecked) {
            ViewCompat.setWindowInsetsAnimationCallback(mRoot, null)
            ViewCompat.setWindowInsetsAnimationCallback(window.decorView, createAnimationCallback())
            // Why it doesn't work on the root view?
        } else {
            ViewCompat.setWindowInsetsAnimationCallback(window.decorView, null)
            ViewCompat.setWindowInsetsAnimationCallback(mRoot, createAnimationCallback())
        }
    }

    private fun createAnimationCallback(): WindowInsetsAnimationCompat.Callback {
        return object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                mTransitions.forEach { it.onPrepare(animation) }
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: List<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                mTransitions.forEach { it.onProgress() }
                return insets
            }

            override fun onStart(
                animation: WindowInsetsAnimationCompat,
                bounds: WindowInsetsAnimationCompat.BoundsCompat
            ): WindowInsetsAnimationCompat.BoundsCompat {
                mTransitions.forEach { obj -> obj.onStart() }
                return bounds
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                mTransitions.forEach { it.onFinish(animation) }
            }
        }
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

    private fun createOnTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private val mViewConfiguration =
                ViewConfiguration.get(this@WindowInsetsControllerPlayground)
            var mAnimationController: WindowInsetsAnimationControllerCompat? = null
            var mCurrentRequest: WindowInsetsAnimationControlListenerCompat? = null
            var mRequestedController = false
            var mDown = 0f
            var mCurrent = 0f
            var mDownInsets = Insets.NONE
            var mShownAtDown = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(
                v: View,
                event: MotionEvent
            ): Boolean {
                mCurrent = event.y
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mDown = event.y
                        val rootWindowInsets = ViewCompat.getRootWindowInsets(v)!!
                        mDownInsets = rootWindowInsets.getInsets(ime())
                        mShownAtDown = rootWindowInsets.isVisible(ime())
                        mRequestedController = false
                        mCurrentRequest = null
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (mAnimationController != null) {
                            updateInset()
                        } else if (abs(mDown - event.y) > mViewConfiguration.scaledTouchSlop &&
                            !mRequestedController
                        ) {
                            mRequestedController = true
                            val listener = object : WindowInsetsAnimationControlListenerCompat {
                                override fun onReady(
                                    controller: WindowInsetsAnimationControllerCompat,
                                    types: Int
                                ) {
                                    if (mCurrentRequest === this) {
                                        mAnimationController = controller
                                        updateInset()
                                    } else {
                                        controller.finish(mShownAtDown)
                                    }
                                }

                                override fun onFinished(
                                    controller: WindowInsetsAnimationControllerCompat
                                ) {
                                    mAnimationController = null
                                }

                                override fun onCancelled(
                                    controller: WindowInsetsAnimationControllerCompat?
                                ) {
                                    mAnimationController = null
                                }
                            }
                            mCurrentRequest = listener

                            ViewCompat.getWindowInsetsController(v)!!.controlWindowInsetsAnimation(
                                ime(),
                                1000,
                                LinearInterpolator(),
                                null /* cancellationSignal */,
                                listener
                            )
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (mAnimationController != null) {
                            val isCancel =
                                event.action == MotionEvent.ACTION_CANCEL
                            mAnimationController!!.finish(
                                if (isCancel) mShownAtDown else !mShownAtDown
                            )
                            mAnimationController = null
                        }
                        mRequestedController = false
                        mCurrentRequest = null
                    }
                }
                return true
            }

            fun updateInset() {
                var inset = (mDownInsets.bottom + (mDown - mCurrent)).toInt()
                val hidden = mAnimationController!!.hiddenStateInsets.bottom
                val shown = mAnimationController!!.shownStateInsets.bottom
                val start = if (mShownAtDown) shown else hidden
                val end = if (mShownAtDown) hidden else shown
                inset = max(inset, hidden)
                inset = min(inset, shown)
                mAnimationController!!.setInsetsAndAlpha(
                    Insets.of(0, 0, 0, inset),
                    1f, (inset - start) / (end - start).toFloat()
                )
            }
        }
    }

    private fun setupTypeSpinner() {
        val types = mapOf(
            "IME" to ime(),
            "Navigation" to WindowInsetsCompat.Type.navigationBars(),
            "System" to systemBars(),
            "Status" to WindowInsetsCompat.Type.statusBars()
        )
        findViewById<Spinner>(R.id.spn_insets_type).apply {
            adapter = ArrayAdapter(
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

    inner class Transition(private val view: View) {
        private var mEndBottom = 0
        private var mStartBottom = 0
        private var mInsetsAnimation: WindowInsetsAnimationCompat? = null
        private val debug = view.id == R.id.editRow

        @SuppressLint("SetTextI18n")
        fun onPrepare(animation: WindowInsetsAnimationCompat) {
            if (animation.typeMask and ime() != 0) {
                mInsetsAnimation = animation
            }
            mStartBottom = view.bottom
            if (debug) {
                values.clear()
                info.text = "Prepare: start=$mStartBottom, end=$mEndBottom"
            }
        }

        fun onProgress() {
            mInsetsAnimation?.let {
                view.y = (
                    mStartBottom +
                        (mEndBottom - mStartBottom) * it.interpolatedFraction - view.height
                    )
            }
            if (debug) {
                Log.d(TAG, view.y.toString())
                values.add(view.y)
                graph.invalidate()
            }
        }

        @SuppressLint("SetTextI18n")
        fun onStart() {
            mEndBottom = view.bottom
            if (debug) {
                info.text = "${info.text}\nStart: start=$mStartBottom, end=$mEndBottom"
            }
        }

        fun onFinish(animation: WindowInsetsAnimationCompat) {
            if (mInsetsAnimation == animation) {
                mInsetsAnimation = null
            }
        }
    }
}