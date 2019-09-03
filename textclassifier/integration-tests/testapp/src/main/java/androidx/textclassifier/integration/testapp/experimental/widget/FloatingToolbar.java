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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.util.Preconditions;
import androidx.textclassifier.integration.testapp.R;
import androidx.textclassifier.widget.IFloatingToolbar;

/**
 * An experimental implementation of floating toolbar that supports slice.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public final class FloatingToolbar implements IFloatingToolbar {

    static final Object FLOATING_TOOLBAR_TAG = "floating_toolbar";

    private final FloatingToolbarPopup mPopup;
    private final Rect mContentRect = new Rect();

    public FloatingToolbar(View view) {
        mPopup = new FloatingToolbarPopup(view.getRootView());
    }

    @Override
    public void setMenu(@NonNull SupportMenu menu) {}

    @Nullable
    @Override
    public SupportMenu getMenu() {
        return null;
    }

    @Override
    public void setContentRect(@NonNull Rect rect) {
        mContentRect.set(Preconditions.checkNotNull(rect));
    }

    @Override
    public void setSuggestedWidth(int suggestedWidth) {}

    @Override
    public void show() {
        mPopup.show(mContentRect);
    }

    @Override
    public void updateLayout() {}

    @Override
    public void dismiss() {}

    @Override
    public void hide() {}

    @Override
    public boolean isShowing() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener onDismiss) {}

    @Override
    public void setDismissOnMenuItemClick(boolean dismiss) {}

    @Override
    public void setOnMenuItemClickListener(
            SupportMenuItem.OnMenuItemClickListener menuItemClickListener) {}

    private static final class FloatingToolbarPopup {

        final View mHost;  // Host for the popup window.
        final Context mContext;
        final PopupWindow mPopupWindow;

        /* View components */
        final ViewGroup mContentContainer;  // holds all contents.
        final ViewGroup mMainPanel;  // holds menu items that are initially displayed.
        final ViewGroup mOverflowPanel;  // holds menu items hidden in the overflow.
        final ViewGroup mSlicePanel;  // holds the rich toolbar content.

        FloatingToolbarPopup(View host) {
            mHost = Preconditions.checkNotNull(host);
            mContext = host.getContext();
            mPopupWindow = createPopupWindow(mContext);
            mContentContainer = createContentContainer(
                    mContext, (ViewGroup) mPopupWindow.getContentView());
            mMainPanel = mContentContainer.findViewById(R.id.mainPanel);
            mOverflowPanel = mContentContainer.findViewById(R.id.overflowPanel);
            mSlicePanel = mContentContainer.findViewById(R.id.slicePanel);
        }

        void show(Rect contentRectOnScreen) {
            mPopupWindow.showAtLocation(mHost, Gravity.NO_GRAVITY, 0, 0);
        }

        static ViewGroup createContentContainer(Context context, ViewGroup parent) {
            ViewGroup contentContainer = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.floating_popup_container, parent);
            contentContainer.setTag(FLOATING_TOOLBAR_TAG);
            contentContainer.setClipToOutline(true);
            return contentContainer;
        }

        static PopupWindow createPopupWindow(Context context) {
            ViewGroup popupContentHolder = new LinearLayout(context);
            popupContentHolder.setSoundEffectsEnabled(false);
            PopupWindow popupWindow = new PopupWindow(popupContentHolder);
            popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            // TODO: Use .setLayoutInScreenEnabled(true) instead of .setClippingEnabled(false)
            // unless FLAG_LAYOUT_IN_SCREEN has any unintentional side-effects.
            popupWindow.setClippingEnabled(false);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
            popupWindow.setAnimationStyle(0);
            int color = Color.TRANSPARENT;
            // Uncomment the next line for a translucent popup. Comment for transparent popup.
            color = Color.argb(50, 50, 0, 0);
            popupWindow.setBackgroundDrawable(new ColorDrawable(color));
            return popupWindow;
        }
    }
}
