/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.InputQueue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.Window
import org.mockito.Mockito.mock

/** Stub implementation of [Window] for use in tests.  */
internal open class TestWindow @JvmOverloads constructor(
    context: Context?,
    private val decorView: View = mock(View::class.java)
) : Window(context) {
    override fun onActive() {}
    override fun setChildDrawable(i: Int, drawable: Drawable) {}
    override fun setChildInt(i: Int, i1: Int) {}
    override fun isShortcutKey(i: Int, keyEvent: KeyEvent): Boolean {
        return false
    }

    override fun setVolumeControlStream(i: Int) {}
    override fun getVolumeControlStream(): Int {
        return 0
    }

    override fun getStatusBarColor(): Int {
        return 0
    }

    override fun setStatusBarColor(i: Int) {}
    override fun getNavigationBarColor(): Int {
        return 0
    }

    override fun setNavigationBarColor(i: Int) {}
    override fun setDecorCaptionShade(i: Int) {}
    override fun setResizingCaptionDrawable(drawable: Drawable) {}
    override fun superDispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        return false
    }

    override fun superDispatchKeyShortcutEvent(keyEvent: KeyEvent): Boolean {
        return false
    }

    override fun superDispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun superDispatchTrackballEvent(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun superDispatchGenericMotionEvent(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun getDecorView(): View {
        return decorView
    }

    override fun peekDecorView(): View {
        TODO("NOT IMPLEMENTED")
    }

    override fun saveHierarchyState(): Bundle {
        TODO("NOT IMPLEMENTED")
    }

    override fun restoreHierarchyState(bundle: Bundle) {}
    override fun takeSurface(callback2: SurfaceHolder.Callback2) {}
    override fun takeInputQueue(callback: InputQueue.Callback) {}
    override fun isFloating(): Boolean {
        return false
    }

    override fun setContentView(i: Int) {}
    override fun setContentView(view: View) {}
    override fun setContentView(view: View, layoutParams: ViewGroup.LayoutParams) {}
    override fun addContentView(view: View, layoutParams: ViewGroup.LayoutParams) {}
    override fun getCurrentFocus(): View? {
        return null
    }

    override fun getLayoutInflater(): LayoutInflater {
        TODO("NOT IMPLEMENTED")
    }

    override fun setTitle(charSequence: CharSequence) {}
    override fun setTitleColor(i: Int) {}
    override fun openPanel(i: Int, keyEvent: KeyEvent) {}
    override fun closePanel(i: Int) {}
    override fun togglePanel(i: Int, keyEvent: KeyEvent) {}
    override fun invalidatePanelMenu(i: Int) {}
    override fun performPanelShortcut(i: Int, i1: Int, keyEvent: KeyEvent, i2: Int): Boolean {
        return false
    }

    override fun performPanelIdentifierAction(i: Int, i1: Int, i2: Int): Boolean {
        return false
    }

    override fun closeAllPanels() {}
    override fun performContextMenuIdentifierAction(i: Int, i1: Int): Boolean {
        return false
    }

    override fun onConfigurationChanged(configuration: Configuration) {}
    override fun setBackgroundDrawable(drawable: Drawable) {}
    override fun setFeatureDrawableResource(i: Int, i1: Int) {}
    override fun setFeatureDrawable(i: Int, drawable: Drawable) {}
    override fun setFeatureDrawableUri(i: Int, uri: Uri) {}
    override fun setFeatureDrawableAlpha(i: Int, i1: Int) {}
    override fun setFeatureInt(i: Int, i1: Int) {}
    override fun takeKeyEvents(b: Boolean) {}
}
