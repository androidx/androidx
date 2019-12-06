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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.internal.view.SupportMenu;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.util.Preconditions;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.textclassifier.integration.testapp.R;
import androidx.textclassifier.widget.IFloatingToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * An experimental implementation of floating toolbar that supports slice.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public final class FloatingToolbar implements IFloatingToolbar {

    private static final SupportMenuItem.OnMenuItemClickListener NO_OP_MENUITEM_CLICK_LISTENER =
            new SupportMenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    return false;
                }
            };

    @VisibleForTesting
    static final Object FLOATING_TOOLBAR_TAG = "floating_toolbar";
    @VisibleForTesting
    static final Object MAIN_PANEL_TAG = "main_panel";

    private final FloatingToolbarPopup mPopup;
    private final Rect mContentRect = new Rect();

    private SupportMenu mMenu;
    private List<SupportMenuItem> mShowingMenuItems = new ArrayList<>();
    private SupportMenuItem.OnMenuItemClickListener mMenuItemClickListener =
            NO_OP_MENUITEM_CLICK_LISTENER;

    /* Item click listeners */
    private SupportMenuItem.OnMenuItemClickListener mOnMenuItemClickListener;

    private final Comparator<SupportMenuItem> mMenuItemComparator =
            new Comparator<SupportMenuItem>() {
                @Override
                public int compare(SupportMenuItem menuItem1, SupportMenuItem menuItem2) {
                    int customOrder1 = getCustomOrder(menuItem1);
                    int customOrder2 = getCustomOrder(menuItem2);
                    if (customOrder1 == customOrder2) {
                        return compareOrder(menuItem1, menuItem2);
                    }
                    return Integer.compare(customOrder1, customOrder2);
                }

                private int getCustomOrder(SupportMenuItem menuItem) {
                    if (menuItem.getItemId() == MENU_ID_SMART_ACTION) {
                        return 0;
                    }
                    if (requiresActionButton(menuItem)) {
                        return 1;
                    }
                    if (requiresOverflow(menuItem)) {
                        return 3;
                    }
                    return 2;
                }

                private int compareOrder(SupportMenuItem menuItem1, SupportMenuItem menuItem2) {
                    return menuItem1.getOrder() - menuItem2.getOrder();
                }
            };

    public FloatingToolbar(View view) {
        mPopup = new FloatingToolbarPopup(view.getRootView());
    }

    @Override
    public void setMenu(@NonNull SupportMenu menu) {
        mMenu = Preconditions.checkNotNull(menu);
    }

    @Nullable
    @Override
    public SupportMenu getMenu() {
        return mMenu;
    }

    @Override
    public void setSuggestedWidth(int suggestedWidth) {}

    @Override
    public void show() {
        doShow();
    }

    @Override
    public void setContentRect(@NonNull Rect rect) {

    }

    private void doShow() {
        List<SupportMenuItem> menuItems = getVisibleAndEnabledMenuItems(mMenu);
        Collections.sort(menuItems, mMenuItemComparator);
        if (!isCurrentlyShowing(menuItems)) {
            mPopup.layoutMenuItems(menuItems, mOnMenuItemClickListener);
        }
        mPopup.show(mContentRect);
    }

    /**
     * Returns true if this floating toolbar is currently showing the specified menu items.
     */
    private boolean isCurrentlyShowing(List<SupportMenuItem> menuItems) {
        if (mShowingMenuItems == null || menuItems.size() != mShowingMenuItems.size()) {
            return false;
        }

        final int size = menuItems.size();
        for (int i = 0; i < size; i++) {
            final SupportMenuItem menuItem = menuItems.get(i);
            final SupportMenuItem showingItem = mShowingMenuItems.get(i);
            if (menuItem.getItemId() != showingItem.getItemId()
                    || !TextUtils.equals(menuItem.getTitle(), showingItem.getTitle())
                    || !Objects.equals(menuItem.getIcon(), showingItem.getIcon())
                    || menuItem.getGroupId() != showingItem.getGroupId()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void updateLayout() {}

    @Override
    public void dismiss() {
        mPopup.dismiss();
    }

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
            @Nullable SupportMenuItem.OnMenuItemClickListener menuItemClickListener) {
        mMenuItemClickListener = mMenuItemClickListener == null
                ? NO_OP_MENUITEM_CLICK_LISTENER : menuItemClickListener;
    }

    /**
     * Returns the visible and enabled menu items in the specified menu.
     * This method is recursive.
     */
    private static List<SupportMenuItem> getVisibleAndEnabledMenuItems(SupportMenu menu) {
        List<SupportMenuItem> menuItems = new ArrayList<>();
        for (int i = 0; (menu != null) && (i < menu.size()); i++) {
            SupportMenuItem menuItem = (SupportMenuItem) menu.getItem(i);
            if (menuItem.isVisible() && menuItem.isEnabled()) {
                SupportMenu subMenu = (SupportMenu) menuItem.getSubMenu();
                if (subMenu != null) {
                    menuItems.addAll(getVisibleAndEnabledMenuItems(subMenu));
                } else {
                    menuItems.add(menuItem);
                }
            }
        }
        return menuItems;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean requiresOverflow(SupportMenuItem menuItem) {
        if (menuItem instanceof MenuItemImpl) {
            final MenuItemImpl impl = (MenuItemImpl) menuItem;
            return !impl.requiresActionButton() && !impl.requestsActionButton();
        }
        return false;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean requiresActionButton(SupportMenuItem menuItem) {
        return menuItem instanceof MenuItemImpl
                && menuItem.requiresActionButton();
    }

    private static final class FloatingToolbarPopup {

        final View mHost;  // Host for the popup window.
        final Context mContext;
        final PopupWindow mPopupWindow;

        /* View components */
        private final ViewGroup mContentContainer;  // holds all contents.
        private final MainPanel mMainPanel;  // holds menu items that are initially displayed.

        /* Item click listeners */
        private SupportMenuItem.OnMenuItemClickListener mOnMenuItemClickListener;
        private final View.OnClickListener mMenuItemButtonOnClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v.getTag() instanceof SupportMenuItem) {
                            if (mOnMenuItemClickListener != null) {
                                mOnMenuItemClickListener.onMenuItemClick(
                                        (SupportMenuItem) v.getTag());
                            }
                        }
                    }
                };

        FloatingToolbarPopup(View host) {
            mHost = Preconditions.checkNotNull(host);
            mContext = host.getContext();
            mPopupWindow = createPopupWindow(mContext);
            mContentContainer = createContentContainer(
                    mContext, (ViewGroup) mPopupWindow.getContentView());
            mMainPanel = new MainPanel(
                    mContentContainer.findViewById(R.id.mainPanel),
                    mContext,
                    mMenuItemButtonOnClickListener);
        }

        /**
         * Shows this popup at the specified coordinates.
         * The specified coordinates may be adjusted to make sure the popup is entirely on-screen.
         */
        public void show(Rect contentRectOnScreen) {
            Preconditions.checkNotNull(contentRectOnScreen);
            mPopupWindow.showAtLocation(mHost, Gravity.NO_GRAVITY, 0, 0);
        }

        /**
         * Lays out buttons for the specified menu items.
         * Requires a subsequent call to {@link #show()} to show the items.
         */
        public void layoutMenuItems(
                List<SupportMenuItem> menuItems,
                SupportMenuItem.OnMenuItemClickListener menuItemClickListener) {
            mOnMenuItemClickListener = menuItemClickListener;
            menuItems = mMainPanel.layoutMenuItems(menuItems);
            if (!menuItems.isEmpty()) {
                // Add remaining items to the overflow.
            }
        }

        public void dismiss() {

        }

        /**
         * Clears out the panels and their container. Resets their calculated sizes.
         */
        @SuppressWarnings("unchecked")
        void clearPanels() {
            mMainPanel.clear();
            mContentContainer.removeAllViews();
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
            popupWindow.setClippingEnabled(false);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
            popupWindow.setAnimationStyle(0);
            // Set the next line to Color.TRANSPARENT for a translucent popup.
            int color = Color.argb(50, 50, 0, 0);
            popupWindow.setBackgroundDrawable(new ColorDrawable(color));
            return popupWindow;
        }

        /**
         * This class is responsible for layout of the main panel.
         */
        private static final class MainPanel {

            private final ViewGroup mWidget;
            private final Context mContext;
            private final int mIconTextSpacing;
            private final int mOverflowButtonWidth;
            private final int mToolbarWidth;

            private int mAvailableWidth;

            /* Item click listeners */
            private final View.OnClickListener mMenuItemButtonOnClickListener;

            MainPanel(ViewGroup widget,
                    Context context,
                    View.OnClickListener menuItemButtonOnClickListener) {
                mWidget = Preconditions.checkNotNull(widget);
                mWidget.setTag(MAIN_PANEL_TAG);
                mContext = Preconditions.checkNotNull(context);
                mMenuItemButtonOnClickListener =
                        Preconditions.checkNotNull(menuItemButtonOnClickListener);
                mIconTextSpacing = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.floating_toolbar_icon_text_spacing);
                mOverflowButtonWidth = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.floating_toolbar_overflow_button_width);
                mToolbarWidth = mContext.getResources()
                        .getDimensionPixelSize(R.dimen.floating_toolbar_preferred_width);
                mAvailableWidth = mToolbarWidth;
            }

            /**
             * Fits as many menu items in the main panel and returns a list of the menu items that
             * were not fit in.
             *
             * @return The menu items that are not included in this main panel.
             */
            List<SupportMenuItem> layoutMenuItems(List<SupportMenuItem> menuItems) {
                Preconditions.checkNotNull(menuItems);
                final List<SupportMenuItem> mainPanelMenuItems = new ArrayList<>();
                // add the overflow menu items to the end of the items list.
                final List<SupportMenuItem> overflowMenuItems = new ArrayList<>();
                for (SupportMenuItem menuItem : menuItems) {
                    if (menuItem.getItemId() != MENU_ID_SMART_ACTION
                            && requiresOverflow(menuItem)) {
                        overflowMenuItems.add(menuItem);
                    } else {
                        mainPanelMenuItems.add(menuItem);
                    }
                }
                mainPanelMenuItems.addAll(overflowMenuItems);

                clear();
                mWidget.setPaddingRelative(0, 0, 0, 0);
                int index;
                boolean isFirst = true;
                boolean isLast = menuItems.size() == 1;
                int itemCount = mainPanelMenuItems.size();
                for (index = 0; index < itemCount; index++) {
                    isLast = index == itemCount - 1;
                    boolean added = addItem(
                            mainPanelMenuItems.get(index),
                            isFirst,
                            isLast);
                    if (!added) {
                        break;
                    }
                    isFirst = false;
                }
                if (!isLast) {
                    // Reserve space for overflowButton.
                    mWidget.setPaddingRelative(0, 0, mOverflowButtonWidth, 0);
                }
                return mainPanelMenuItems.subList(index, itemCount);
            }

            void clear() {
                mWidget.removeAllViews();
                mAvailableWidth = mToolbarWidth;
            }

            /**
             * Returns true if the given menu item is successfully added to the main panel ,
             * otherwise, returns false.
             */
            private boolean addItem(final SupportMenuItem item, boolean isFirst, boolean isLast) {
                // if this is the first item, regardless of requiresOverflow(), it should be
                // displayed on the main panel. Otherwise all items including this one will be
                // overflow items, and should be displayed in overflow panel.
                if (!isFirst && requiresOverflow(item)) {
                    return false;
                }

                final boolean showIcon = isFirst
                        && item.getItemId() == MENU_ID_SMART_ACTION;
                final View menuItemButton = PanelUtils.createMenuItemButton(
                        mContext, item, mIconTextSpacing, showIcon);
                if (!showIcon) {
                    ((LinearLayout) menuItemButton).setGravity(Gravity.CENTER);
                }

                // Adding additional start padding for the first button to even out button
                // spacing.
                if (isFirst) {
                    menuItemButton.setPaddingRelative(
                            (int) (1.5 * menuItemButton.getPaddingStart()),
                            menuItemButton.getPaddingTop(),
                            menuItemButton.getPaddingEnd(),
                            menuItemButton.getPaddingBottom());
                }

                // Adding additional end padding for the last button to even out button spacing.
                if (isLast) {
                    menuItemButton.setPaddingRelative(
                            menuItemButton.getPaddingStart(),
                            menuItemButton.getPaddingTop(),
                            (int) (1.5 * menuItemButton.getPaddingEnd()),
                            menuItemButton.getPaddingBottom());
                }

                menuItemButton.measure(
                        View.MeasureSpec.UNSPECIFIED,
                        View.MeasureSpec.UNSPECIFIED);
                final int menuItemButtonWidth = Math.min(
                        menuItemButton.getMeasuredWidth(), mToolbarWidth);

                // Check if we can fit an item while reserving space for the overflowButton.
                final boolean canFitWithOverflow =
                        menuItemButtonWidth <= mAvailableWidth - mOverflowButtonWidth;
                final boolean canFitNoOverflow =
                        isLast && menuItemButtonWidth <= mAvailableWidth;
                if (canFitWithOverflow || canFitNoOverflow) {
                    PanelUtils.setButtonTagAndClickListener(
                            menuItemButton, item, mMenuItemButtonOnClickListener);
                    // Set tooltips for main panel items, but not overflow items (b/35726766).
                    CharSequence tooltip = item.getTooltipText() == null
                            ? item.getTitle()
                            : item.getTooltipText();
                    ViewCompat.setTooltipText(menuItemButton, tooltip);
                    mWidget.addView(menuItemButton);
                    final ViewGroup.LayoutParams params = menuItemButton.getLayoutParams();
                    params.width = menuItemButtonWidth;
                    menuItemButton.setLayoutParams(params);
                    mAvailableWidth -= menuItemButtonWidth;
                    return true;
                }
                return false;
            }
        }

        /**
         * A helper class that contains the helper methods which are shared by different panels.
         */
        private static final class PanelUtils{

            /**
             * Creates and returns a menu button for the specified menu item.
             */
            static View createMenuItemButton(
                    Context context,
                    SupportMenuItem menuItem,
                    int iconTextSpacing,
                    boolean showIcon) {
                final View menuItemButton = LayoutInflater.from(context)
                        .inflate(R.layout.floating_popup_menu_button, null);
                if (menuItem != null) {
                    updateMenuItemButton(menuItemButton, menuItem, iconTextSpacing, showIcon);
                }
                return menuItemButton;
            }

            /**
             * Updates the specified menu item button with the specified menu item data.
             */
            static void updateMenuItemButton(
                    View menuItemButton,
                    SupportMenuItem menuItem,
                    int iconTextSpacing,
                    boolean showIcon) {
                final TextView buttonText = menuItemButton.findViewById(
                        androidx.textclassifier.R.id.floating_toolbar_menu_item_text);
                buttonText.setEllipsize(null);
                if (TextUtils.isEmpty(menuItem.getTitle())) {
                    buttonText.setVisibility(View.GONE);
                } else {
                    buttonText.setVisibility(View.VISIBLE);
                    buttonText.setText(menuItem.getTitle());
                }
                final ImageView buttonIcon = menuItemButton.findViewById(
                        androidx.textclassifier.R.id.floating_toolbar_menu_item_image);
                if (menuItem.getIcon() == null || !showIcon) {
                    buttonIcon.setVisibility(View.GONE);
                    buttonText.setPaddingRelative(0, 0, 0, 0);
                } else {
                    buttonIcon.setVisibility(View.VISIBLE);
                    buttonIcon.setImageDrawable(menuItem.getIcon());
                    buttonText.setPaddingRelative(iconTextSpacing, 0, 0, 0);
                }
                final CharSequence contentDescription =
                        MenuItemCompat.getContentDescription(menuItem);
                if (TextUtils.isEmpty(contentDescription)) {
                    menuItemButton.setContentDescription(menuItem.getTitle());
                } else {
                    menuItemButton.setContentDescription(contentDescription);
                }
            }

            static void setButtonTagAndClickListener(
                    View menuItemButton,
                    SupportMenuItem menuItem,
                    View.OnClickListener menuItemButtonOnClickListener) {
                menuItemButton.setTag(menuItem);
                menuItemButton.setOnClickListener(menuItemButtonOnClickListener);
            }
        }
    }
}
