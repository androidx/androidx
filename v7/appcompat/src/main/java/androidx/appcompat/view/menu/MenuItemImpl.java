/*
 * Copyright (C) 2006 The Android Open Source Project
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

package androidx.appcompat.view.menu;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.internal.view.SupportMenuItem;
import androidx.core.view.ActionProvider;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class MenuItemImpl implements SupportMenuItem {

    private static final String TAG = "MenuItemImpl";

    private static final int SHOW_AS_ACTION_MASK = SHOW_AS_ACTION_NEVER |
            SHOW_AS_ACTION_IF_ROOM |
            SHOW_AS_ACTION_ALWAYS;

    private final int mId;
    private final int mGroup;
    private final int mCategoryOrder;
    private final int mOrdering;
    private CharSequence mTitle;
    private CharSequence mTitleCondensed;
    private Intent mIntent;
    private char mShortcutNumericChar;
    private int mShortcutNumericModifiers = KeyEvent.META_CTRL_ON;
    private char mShortcutAlphabeticChar;
    private int mShortcutAlphabeticModifiers = KeyEvent.META_CTRL_ON;

    /** The icon's drawable which is only created as needed */
    private Drawable mIconDrawable;

    /**
     * The icon's resource ID which is used to get the Drawable when it is
     * needed (if the Drawable isn't already obtained--only one of the two is
     * needed).
     */
    private int mIconResId = NO_ICON;

    /** The menu to which this item belongs */
    MenuBuilder mMenu;
    /** If this item should launch a sub menu, this is the sub menu to launch */
    private SubMenuBuilder mSubMenu;

    private Runnable mItemCallback;
    private SupportMenuItem.OnMenuItemClickListener mClickListener;

    private CharSequence mContentDescription;
    private CharSequence mTooltipText;

    private ColorStateList mIconTintList = null;
    private PorterDuff.Mode mIconTintMode = null;
    private boolean mHasIconTint = false;
    private boolean mHasIconTintMode = false;
    private boolean mNeedToApplyIconTint = false;

    private int mFlags = ENABLED;
    private static final int CHECKABLE = 0x00000001;
    private static final int CHECKED = 0x00000002;
    private static final int EXCLUSIVE = 0x00000004;
    private static final int HIDDEN = 0x00000008;
    private static final int ENABLED = 0x00000010;
    private static final int IS_ACTION = 0x00000020;

    private int mShowAsAction = SHOW_AS_ACTION_NEVER;

    private View mActionView;
    private ActionProvider mActionProvider;
    private MenuItem.OnActionExpandListener mOnActionExpandListener;
    private boolean mIsActionViewExpanded = false;

    /** Used for the icon resource ID if this item does not have an icon */
    static final int NO_ICON = 0;

    /**
     * Current use case is for context menu: Extra information linked to the
     * View that added this item to the context menu.
     */
    private ContextMenuInfo mMenuInfo;


    /**
     * Instantiates this menu item.
     *
     * @param menu
     * @param group Item ordering grouping control. The item will be added after
     *            all other items whose order is <= this number, and before any
     *            that are larger than it. This can also be used to define
     *            groups of items for batch state changes. Normally use 0.
     * @param id Unique item ID. Use 0 if you do not need a unique ID.
     * @param categoryOrder The ordering for this item.
     * @param title The text to display for the item.
     */
    MenuItemImpl(MenuBuilder menu, int group, int id, int categoryOrder, int ordering,
            CharSequence title, int showAsAction) {

        mMenu = menu;
        mId = id;
        mGroup = group;
        mCategoryOrder = categoryOrder;
        mOrdering = ordering;
        mTitle = title;
        mShowAsAction = showAsAction;
    }

    /**
     * Invokes the item by calling various listeners or callbacks.
     *
     * @return true if the invocation was handled, false otherwise
     */
    public boolean invoke() {
        if (mClickListener != null && mClickListener.onMenuItemClick(this)) {
            return true;
        }

        if (mMenu.dispatchMenuItemSelected(mMenu, this)) {
            return true;
        }

        if (mItemCallback != null) {
            mItemCallback.run();
            return true;
        }

        if (mIntent != null) {
            try {
                mMenu.getContext().startActivity(mIntent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Can't find activity to handle intent; ignoring", e);
            }
        }

        if (mActionProvider != null && mActionProvider.onPerformDefaultAction()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isEnabled() {
        return (mFlags & ENABLED) != 0;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        if (enabled) {
            mFlags |= ENABLED;
        } else {
            mFlags &= ~ENABLED;
        }

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public int getGroupId() {
        return mGroup;
    }

    @Override
    @ViewDebug.CapturedViewProperty
    public int getItemId() {
        return mId;
    }

    @Override
    public int getOrder() {
        return mCategoryOrder;
    }

    public int getOrdering() {
        return mOrdering;
    }

    @Override
    public Intent getIntent() {
        return mIntent;
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        mIntent = intent;
        return this;
    }

    Runnable getCallback() {
        return mItemCallback;
    }

    public MenuItem setCallback(Runnable callback) {
        mItemCallback = callback;
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return mShortcutAlphabeticChar;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        if (mShortcutAlphabeticChar == alphaChar) {
            return this;
        }

        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers) {
        if (mShortcutAlphabeticChar == alphaChar
                && mShortcutAlphabeticModifiers == alphaModifiers) {
            return this;
        }

        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        mShortcutAlphabeticModifiers = KeyEvent.normalizeMetaState(alphaModifiers);

        mMenu.onItemsChanged(false);
        return this;
    }

    @Override
    public int getAlphabeticModifiers() {
        return mShortcutAlphabeticModifiers;
    }

    @Override
    public char getNumericShortcut() {
        return mShortcutNumericChar;
    }

    @Override
    public int getNumericModifiers() {
        return mShortcutNumericModifiers;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        if (mShortcutNumericChar == numericChar) {
            return this;
        }

        mShortcutNumericChar = numericChar;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers) {
        if (mShortcutNumericChar == numericChar && mShortcutNumericModifiers == numericModifiers) {
            return this;
        }

        mShortcutNumericChar = numericChar;
        mShortcutNumericModifiers = KeyEvent.normalizeMetaState(numericModifiers);

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        mShortcutNumericChar = numericChar;
        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers,
            int alphaModifiers) {
        mShortcutNumericChar = numericChar;
        mShortcutNumericModifiers = KeyEvent.normalizeMetaState(numericModifiers);
        mShortcutAlphabeticChar = Character.toLowerCase(alphaChar);
        mShortcutAlphabeticModifiers = KeyEvent.normalizeMetaState(alphaModifiers);

        mMenu.onItemsChanged(false);

        return this;
    }

    /**
     * @return The active shortcut (based on QWERTY-mode of the menu).
     */
    char getShortcut() {
        return (mMenu.isQwertyMode() ? mShortcutAlphabeticChar : mShortcutNumericChar);
    }

    /**
     * @return The label to show for the shortcut. This includes the chording key (for example
     *         'Menu+a'). Also, any non-human readable characters should be human readable (for
     *         example 'Menu+enter').
     */
    String getShortcutLabel() {

        char shortcut = getShortcut();
        if (shortcut == 0) {
            return "";
        }

        Resources res = mMenu.getContext().getResources();

        StringBuilder sb = new StringBuilder();
        if (ViewConfiguration.get(mMenu.getContext()).hasPermanentMenuKey()) {
            sb.append(res.getString(R.string.abc_prepend_shortcut_label));
        }

        final int modifiers =
                mMenu.isQwertyMode() ? mShortcutAlphabeticModifiers : mShortcutNumericModifiers;
        appendModifier(sb, modifiers, KeyEvent.META_META_ON,
                res.getString(R.string.abc_menu_meta_shortcut_label));
        appendModifier(sb, modifiers, KeyEvent.META_CTRL_ON,
                res.getString(R.string.abc_menu_ctrl_shortcut_label));
        appendModifier(sb, modifiers, KeyEvent.META_ALT_ON,
                res.getString(R.string.abc_menu_alt_shortcut_label));
        appendModifier(sb, modifiers, KeyEvent.META_SHIFT_ON,
                res.getString(R.string.abc_menu_shift_shortcut_label));
        appendModifier(sb, modifiers, KeyEvent.META_SYM_ON,
                res.getString(R.string.abc_menu_sym_shortcut_label));
        appendModifier(sb, modifiers, KeyEvent.META_FUNCTION_ON,
                res.getString(R.string.abc_menu_function_shortcut_label));

        switch (shortcut) {

            case '\n':
                sb.append(res.getString(R.string.abc_menu_enter_shortcut_label));
                break;

            case '\b':
                sb.append(res.getString(R.string.abc_menu_delete_shortcut_label));
                break;

            case ' ':
                sb.append(res.getString(R.string.abc_menu_space_shortcut_label));
                break;

            default:
                sb.append(shortcut);
                break;
        }

        return sb.toString();
    }

    private static void appendModifier(StringBuilder sb, int modifiers, int flag, String label) {
        if ((modifiers & flag) == flag) {
            sb.append(label);
        }
    }

    /**
     * @return Whether this menu item should be showing shortcuts (depends on
     *         whether the menu should show shortcuts and whether this item has
     *         a shortcut defined)
     */
    boolean shouldShowShortcut() {
        // Show shortcuts if the menu is supposed to show shortcuts AND this item has a shortcut
        return mMenu.isShortcutsVisible() && (getShortcut() != 0);
    }

    @Override
    public SubMenu getSubMenu() {
        return mSubMenu;
    }

    @Override
    public boolean hasSubMenu() {
        return mSubMenu != null;
    }

    public void setSubMenu(SubMenuBuilder subMenu) {
        mSubMenu = subMenu;

        subMenu.setHeaderTitle(getTitle());
    }

    @Override
    @ViewDebug.CapturedViewProperty
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Gets the title for a particular {@link MenuView.ItemView}
     *
     * @param itemView The ItemView that is receiving the title
     * @return Either the title or condensed title based on what the ItemView prefers
     */
    CharSequence getTitleForItemView(MenuView.ItemView itemView) {
        return ((itemView != null) && itemView.prefersCondensedTitle())
                ? getTitleCondensed()
                : getTitle();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        mTitle = title;

        mMenu.onItemsChanged(false);

        if (mSubMenu != null) {
            mSubMenu.setHeaderTitle(title);
        }

        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        return setTitle(mMenu.getContext().getString(title));
    }

    @Override
    public CharSequence getTitleCondensed() {
        final CharSequence ctitle = mTitleCondensed != null ? mTitleCondensed : mTitle;

        if (Build.VERSION.SDK_INT < 18 && ctitle != null && !(ctitle instanceof String)) {
            // For devices pre-JB-MR2, where we have a non-String CharSequence, we need to
            // convert this to a String so that EventLog.writeEvent() does not throw an exception
            // in Activity.onMenuItemSelected()
            return ctitle.toString();
        } else {
            // Else, we just return the condensed title
            return ctitle;
        }
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mTitleCondensed = title;

        // Could use getTitle() in the loop below, but just cache what it would do here
        if (title == null) {
            title = mTitle;
        }

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public Drawable getIcon() {
        if (mIconDrawable != null) {
            return applyIconTintIfNecessary(mIconDrawable);
        }

        if (mIconResId != NO_ICON) {
            Drawable icon = AppCompatResources.getDrawable(mMenu.getContext(), mIconResId);
            mIconResId = NO_ICON;
            mIconDrawable = icon;
            return applyIconTintIfNecessary(icon);
        }

        return null;
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        mIconResId = NO_ICON;
        mIconDrawable = icon;
        mNeedToApplyIconTint = true;
        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public MenuItem setIcon(int iconResId) {
        mIconDrawable = null;
        mIconResId = iconResId;
        mNeedToApplyIconTint = true;

        // If we have a view, we need to push the Drawable to them
        mMenu.onItemsChanged(false);

        return this;
    }


    @Override
    public MenuItem setIconTintList(@Nullable ColorStateList iconTintList) {
        mIconTintList = iconTintList;
        mHasIconTint = true;
        mNeedToApplyIconTint = true;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public ColorStateList getIconTintList() {
        return mIconTintList;
    }

    @Override
    public MenuItem setIconTintMode(PorterDuff.Mode iconTintMode) {
        mIconTintMode = iconTintMode;
        mHasIconTintMode = true;
        mNeedToApplyIconTint = true;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public PorterDuff.Mode getIconTintMode() {
        return mIconTintMode;
    }

    private Drawable applyIconTintIfNecessary(Drawable icon) {
        if (icon != null && mNeedToApplyIconTint && (mHasIconTint || mHasIconTintMode)) {
            icon = DrawableCompat.wrap(icon);
            icon = icon.mutate();

            if (mHasIconTint) {
                DrawableCompat.setTintList(icon, mIconTintList);
            }

            if (mHasIconTintMode) {
                DrawableCompat.setTintMode(icon, mIconTintMode);
            }

            mNeedToApplyIconTint = false;
        }

        return icon;
    }

    @Override
    public boolean isCheckable() {
        return (mFlags & CHECKABLE) == CHECKABLE;
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~CHECKABLE) | (checkable ? CHECKABLE : 0);
        if (oldFlags != mFlags) {
            mMenu.onItemsChanged(false);
        }

        return this;
    }

    public void setExclusiveCheckable(boolean exclusive) {
        mFlags = (mFlags & ~EXCLUSIVE) | (exclusive ? EXCLUSIVE : 0);
    }

    public boolean isExclusiveCheckable() {
        return (mFlags & EXCLUSIVE) != 0;
    }

    @Override
    public boolean isChecked() {
        return (mFlags & CHECKED) == CHECKED;
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        if ((mFlags & EXCLUSIVE) != 0) {
            // Call the method on the Menu since it knows about the others in this
            // exclusive checkable group
            mMenu.setExclusiveItemChecked(this);
        } else {
            setCheckedInt(checked);
        }

        return this;
    }

    void setCheckedInt(boolean checked) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~CHECKED) | (checked ? CHECKED : 0);
        if (oldFlags != mFlags) {
            mMenu.onItemsChanged(false);
        }
    }

    @Override
    public boolean isVisible() {
        if (mActionProvider != null && mActionProvider.overridesItemVisibility()) {
            return (mFlags & HIDDEN) == 0 && mActionProvider.isVisible();
        }
        return (mFlags & HIDDEN) == 0;
    }

    /**
     * Changes the visibility of the item. This method DOES NOT notify the parent menu of a change
     * in this item, so this should only be called from methods that will eventually trigger this
     * change.  If unsure, use {@link #setVisible(boolean)} instead.
     *
     * @param shown Whether to show (true) or hide (false).
     * @return Whether the item's shown state was changed
     */
    boolean setVisibleInt(boolean shown) {
        final int oldFlags = mFlags;
        mFlags = (mFlags & ~HIDDEN) | (shown ? 0 : HIDDEN);
        return oldFlags != mFlags;
    }

    @Override
    public MenuItem setVisible(boolean shown) {
        // Try to set the shown state to the given state. If the shown state was changed
        // (i.e. the previous state isn't the same as given state), notify the parent menu that
        // the shown state has changed for this item
        if (setVisibleInt(shown)) mMenu.onItemVisibleChanged(this);

        return this;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener clickListener) {
        mClickListener = clickListener;
        return this;
    }

    @Override
    public String toString() {
        return mTitle != null ? mTitle.toString() : null;
    }

    void setMenuInfo(ContextMenuInfo menuInfo) {
        mMenuInfo = menuInfo;
    }

    @Override
    public ContextMenuInfo getMenuInfo() {
        return mMenuInfo;
    }

    public void actionFormatChanged() {
        mMenu.onItemActionRequestChanged(this);
    }

    /**
     * @return Whether the menu should show icons for menu items.
     */
    public boolean shouldShowIcon() {
        return mMenu.getOptionalIconsVisible();
    }

    public boolean isActionButton() {
        return (mFlags & IS_ACTION) == IS_ACTION;
    }

    public boolean requestsActionButton() {
        return (mShowAsAction & SHOW_AS_ACTION_IF_ROOM) == SHOW_AS_ACTION_IF_ROOM;
    }

    public boolean requiresActionButton() {
        return (mShowAsAction & SHOW_AS_ACTION_ALWAYS) == SHOW_AS_ACTION_ALWAYS;
    }

    public void setIsActionButton(boolean isActionButton) {
        if (isActionButton) {
            mFlags |= IS_ACTION;
        } else {
            mFlags &= ~IS_ACTION;
        }
    }

    public boolean showsTextAsAction() {
        return (mShowAsAction & SHOW_AS_ACTION_WITH_TEXT) == SHOW_AS_ACTION_WITH_TEXT;
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        switch (actionEnum & SHOW_AS_ACTION_MASK) {
            case SHOW_AS_ACTION_ALWAYS:
            case SHOW_AS_ACTION_IF_ROOM:
            case SHOW_AS_ACTION_NEVER:
                // Looks good!
                break;

            default:
                // Mutually exclusive options selected!
                throw new IllegalArgumentException("SHOW_AS_ACTION_ALWAYS, SHOW_AS_ACTION_IF_ROOM,"
                        + " and SHOW_AS_ACTION_NEVER are mutually exclusive.");
        }
        mShowAsAction = actionEnum;
        mMenu.onItemActionRequestChanged(this);
    }

    @Override
    public SupportMenuItem setActionView(View view) {
        mActionView = view;
        mActionProvider = null;
        if (view != null && view.getId() == View.NO_ID && mId > 0) {
            view.setId(mId);
        }
        mMenu.onItemActionRequestChanged(this);
        return this;
    }

    @Override
    public SupportMenuItem setActionView(int resId) {
        final Context context = mMenu.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        setActionView(inflater.inflate(resId, new LinearLayout(context), false));
        return this;
    }

    @Override
    public View getActionView() {
        if (mActionView != null) {
            return mActionView;
        } else if (mActionProvider != null) {
            mActionView = mActionProvider.onCreateActionView(this);
            return mActionView;
        } else {
            return null;
        }
    }

    @Override
    public MenuItem setActionProvider(android.view.ActionProvider actionProvider) {
        throw new UnsupportedOperationException(
                "This is not supported, use MenuItemCompat.setActionProvider()");
    }

    @Override
    public android.view.ActionProvider getActionProvider() {
        throw new UnsupportedOperationException(
                "This is not supported, use MenuItemCompat.getActionProvider()");
    }

    @Override
    public ActionProvider getSupportActionProvider() {
        return mActionProvider;
    }

    @Override
    public SupportMenuItem setSupportActionProvider(ActionProvider actionProvider) {
        if (mActionProvider != null) {
            mActionProvider.reset();
        }
        mActionView = null;
        mActionProvider = actionProvider;
        mMenu.onItemsChanged(true); // Measurement can be changed
        if (mActionProvider != null) {
            mActionProvider.setVisibilityListener(new ActionProvider.VisibilityListener() {
                @Override
                public void onActionProviderVisibilityChanged(boolean isVisible) {
                    mMenu.onItemVisibleChanged(MenuItemImpl.this);
                }
            });
        }
        return this;
    }

    @Override
    public SupportMenuItem setShowAsActionFlags(int actionEnum) {
        setShowAsAction(actionEnum);
        return this;
    }

    @Override
    public boolean expandActionView() {
        if (!hasCollapsibleActionView()) {
            return false;
        }

        if (mOnActionExpandListener == null ||
                mOnActionExpandListener.onMenuItemActionExpand(this)) {
            return mMenu.expandItemActionView(this);
        }

        return false;
    }

    @Override
    public boolean collapseActionView() {
        if ((mShowAsAction & SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW) == 0) {
            return false;
        }
        if (mActionView == null) {
            // We're already collapsed if we have no action view.
            return true;
        }

        if (mOnActionExpandListener == null ||
                mOnActionExpandListener.onMenuItemActionCollapse(this)) {
            return mMenu.collapseItemActionView(this);
        }

        return false;
    }

    public boolean hasCollapsibleActionView() {
        if ((mShowAsAction & SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW) != 0) {
            if (mActionView == null && mActionProvider != null) {
                mActionView = mActionProvider.onCreateActionView(this);
            }
            return mActionView != null;
        }
        return false;
    }

    public void setActionViewExpanded(boolean isExpanded) {
        mIsActionViewExpanded = isExpanded;
        mMenu.onItemsChanged(false);
    }

    @Override
    public boolean isActionViewExpanded() {
        return mIsActionViewExpanded;
    }

    @Override
    public MenuItem setOnActionExpandListener(MenuItem.OnActionExpandListener listener) {
        mOnActionExpandListener = listener;
        return this;
    }

    @Override
    public SupportMenuItem setContentDescription(CharSequence contentDescription) {
        mContentDescription = contentDescription;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    @Override
    public SupportMenuItem setTooltipText(CharSequence tooltipText) {
        mTooltipText = tooltipText;

        mMenu.onItemsChanged(false);

        return this;
    }

    @Override
    public CharSequence getTooltipText() {
        return mTooltipText;
    }
}
