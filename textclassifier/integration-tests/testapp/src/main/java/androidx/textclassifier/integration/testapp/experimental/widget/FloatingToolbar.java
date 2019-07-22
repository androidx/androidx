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

package androidx.textclassifier.integration.testapp.experimental.widget;

import android.graphics.Rect;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.textclassifier.widget.IFloatingToolbar;

/**
 * An experimental implementation of floating toolbar that supports slice.
 */
public final class FloatingToolbar implements IFloatingToolbar {

    public FloatingToolbar(@NonNull TextView textView) {

    }

    @Override
    public void setMenu(@NonNull SupportMenu menu) {

    }

    @Nullable
    @Override
    public SupportMenu getMenu() {
        return null;
    }

    @Override
    public void setContentRect(@NonNull Rect rect) {

    }

    @Override
    public void setSuggestedWidth(int suggestedWidth) {

    }

    @Override
    public void show() {

    }

    @Override
    public void updateLayout() {

    }

    @Override
    public void dismiss() {

    }

    @Override
    public void hide() {

    }

    @Override
    public boolean isShowing() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener onDismiss) {

    }

    @Override
    public void setDismissOnMenuItemClick(boolean dismiss) {

    }

    @Override
    public void setOnMenuItemClickListener(
            SupportMenuItem.OnMenuItemClickListener menuItemClickListener) {

    }
}
