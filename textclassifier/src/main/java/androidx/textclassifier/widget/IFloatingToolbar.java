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

package androidx.textclassifier.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.textclassifier.R;

/**
 * A floating toolbar for showing contextual menu items.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface IFloatingToolbar {
    int MENU_ID_SMART_ACTION = R.id.smartAction;

    /**
     * Sets the menu to be shown in this floating toolbar.
     * NOTE: Call {@link #updateLayout()} or {@link #show()} to effect visual changes to the
     * toolbar.
     */
    void setMenu(@NonNull SupportMenu menu);

    /**
     * Returns the currently set menu.
     */
    @Nullable
    SupportMenu getMenu();

    /**
     * Sets the custom listener for invocation of menu items in this floating toolbar.
     */
    void setOnMenuItemClickListener(
            @Nullable SupportMenuItem.OnMenuItemClickListener menuItemClickListener);

    /**
     * Sets the content rectangle. This is the area of the interesting content that this toolbar
     * should avoid obstructing.
     * NOTE: Call {@link #updateLayout()} or {@link #show()} to effect visual changes to the
     * toolbar.
     */
    void setContentRect(@NonNull Rect rect);

    /**
     * Sets the suggested width of this floating toolbar.
     * The actual width will be about this size but there are no guarantees that it will be exactly
     * the suggested width.
     * NOTE: Call {@link #updateLayout()} or {@link #show()} to effect visual changes to the
     * toolbar.
     */
    void setSuggestedWidth(int suggestedWidth);

    /**
     * Shows this floating toolbar.
     */
    void show();

    /**
     * Updates this floating toolbar to reflect recent position and view updates.
     * NOTE: This method is a no-op if the toolbar isn't showing.
     */
    void updateLayout();

    /**
     * Dismisses this floating toolbar.
     */
    void dismiss();

    /**
     * Hides this floating toolbar. This is a no-op if the toolbar is not showing.
     * Use {@link #isHidden()} to distinguish between a hidden and a dismissed toolbar.
     */
    void hide();

    /**
     * Returns {@code true} if this toolbar is currently showing. {@code false} otherwise.
     */
    boolean isShowing();

    /**
     * Returns {@code true} if this toolbar is currently hidden. {@code false} otherwise.
     */
    boolean isHidden();

    /**
     * Sets the floating toolbar's onDismissListener.
     */
    void setOnDismissListener(@Nullable PopupWindow.OnDismissListener onDismiss);

    /**
     * Sets whether or not to dismiss the floating toolbar after a menu item click has been handled.
     */
    void setDismissOnMenuItemClick(boolean dismiss);
}
