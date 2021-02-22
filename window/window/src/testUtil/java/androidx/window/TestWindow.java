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

package androidx.window;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stub implementation of {@link Window} for use in tests. */
public class TestWindow extends Window {

    private final View mDecorView;

    public TestWindow(Context context) {
        this(context, mock(View.class));
    }

    public TestWindow(Context context, View decorView) {
        super(context);
        mDecorView = decorView;
    }

    @Override
    protected void onActive() {}

    @Override
    public void setChildDrawable(int i, Drawable drawable) {}

    @Override
    public void setChildInt(int i, int i1) {}

    @Override
    public boolean isShortcutKey(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void setVolumeControlStream(int i) {}

    @Override
    public int getVolumeControlStream() {
        return 0;
    }

    @Override
    public int getStatusBarColor() {
        return 0;
    }

    @Override
    public void setStatusBarColor(int i) {}

    @Override
    public int getNavigationBarColor() {
        return 0;
    }

    @Override
    public void setNavigationBarColor(int i) {}

    @Override
    public void setDecorCaptionShade(int i) {}

    @Override
    public void setResizingCaptionDrawable(Drawable drawable) {}

    @Override
    public boolean superDispatchKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean superDispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean superDispatchTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean superDispatchTrackballEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean superDispatchGenericMotionEvent(MotionEvent motionEvent) {
        return false;
    }

    @NonNull
    @Override
    public View getDecorView() {
        return mDecorView;
    }

    @Override
    public View peekDecorView() {
        return null;
    }

    @Override
    public Bundle saveHierarchyState() {
        return null;
    }

    @Override
    public void restoreHierarchyState(Bundle bundle) {}

    @Override
    public void takeSurface(SurfaceHolder.Callback2 callback2) {}

    @Override
    public void takeInputQueue(InputQueue.Callback callback) {}

    @Override
    public boolean isFloating() {
        return false;
    }

    @Override
    public void setContentView(int i) {}

    @Override
    public void setContentView(View view) {}

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {}

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {}

    @Nullable
    @Override
    public View getCurrentFocus() {
        return null;
    }

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return null;
    }

    @Override
    public void setTitle(CharSequence charSequence) {}

    @Override
    public void setTitleColor(int i) {}

    @Override
    public void openPanel(int i, KeyEvent keyEvent) {}

    @Override
    public void closePanel(int i) {}

    @Override
    public void togglePanel(int i, KeyEvent keyEvent) {}

    @Override
    public void invalidatePanelMenu(int i) {}

    @Override
    public boolean performPanelShortcut(int i, int i1, KeyEvent keyEvent, int i2) {
        return false;
    }

    @Override
    public boolean performPanelIdentifierAction(int i, int i1, int i2) {
        return false;
    }

    @Override
    public void closeAllPanels() {}

    @Override
    public boolean performContextMenuIdentifierAction(int i, int i1) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {}

    @Override
    public void setBackgroundDrawable(Drawable drawable) {}

    @Override
    public void setFeatureDrawableResource(int i, int i1) {}

    @Override
    public void setFeatureDrawable(int i, Drawable drawable) {}

    @Override
    public void setFeatureDrawableUri(int i, Uri uri) {}

    @Override
    public void setFeatureDrawableAlpha(int i, int i1) {}

    @Override
    public void setFeatureInt(int i, int i1) {}

    @Override
    public void takeKeyEvents(boolean b) {}
}
