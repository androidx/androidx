/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.internal.view.SupportMenuItem;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Helper for accessing features in {@link android.view.MenuItem}.
 * <p class="note"><strong>Note:</strong> You cannot get an instance of this class. Instead,
 * it provides <em>static</em> methods that correspond to the methods in {@link
 * android.view.MenuItem}, but take a {@link android.view.MenuItem} object as an additional
 * argument.</p>
 */
public final class MenuItemCompat {
    private static final String TAG = "MenuItemCompat";

    /**
     * Never show this item as a button in an Action Bar.
     *
     * @deprecated Use {@link MenuItem#SHOW_AS_ACTION_NEVER} directly.
     */
    @Deprecated
    public static final int SHOW_AS_ACTION_NEVER = 0;

    /**
     * Show this item as a button in an Action Bar if the system
     * decides there is room for it.
     *
     * @deprecated Use {@link MenuItem#SHOW_AS_ACTION_IF_ROOM} directly.
     */
    @Deprecated
    public static final int SHOW_AS_ACTION_IF_ROOM = 1;

    /**
     * Always show this item as a button in an Action Bar. Use sparingly!
     * If too many items are set to always show in the Action Bar it can
     * crowd the Action Bar and degrade the user experience on devices with
     * smaller screens. A good rule of thumb is to have no more than 2
     * items set to always show at a time.
     *
     * @deprecated Use {@link MenuItem#SHOW_AS_ACTION_ALWAYS} directly.
     */
    @Deprecated
    public static final int SHOW_AS_ACTION_ALWAYS = 2;

    /**
     * When this item is in the action bar, always show it with a
     * text label even if it also has an icon specified.
     *
     * @deprecated Use {@link MenuItem#SHOW_AS_ACTION_WITH_TEXT} directly.
     */
    @Deprecated
    public static final int SHOW_AS_ACTION_WITH_TEXT = 4;

    /**
     * This item's action view collapses to a normal menu item.
     * When expanded, the action view temporarily takes over
     * a larger segment of its container.
     *
     * @deprecated Use {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW} directly.
     */
    @Deprecated
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

    /**
     * Interface for the full API.
     */
    interface MenuVersionImpl {
        void setContentDescription(MenuItem item, CharSequence contentDescription);
        CharSequence getContentDescription(MenuItem item);
        void setTooltipText(MenuItem item, CharSequence tooltipText);
        CharSequence getTooltipText(MenuItem item);
        void setShortcut(MenuItem item, char numericChar, char alphaChar, int numericModifiers,
                int alphaModifiers);
        void setAlphabeticShortcut(MenuItem item, char alphaChar, int alphaModifiers);
        int getAlphabeticModifiers(MenuItem item);
        void setNumericShortcut(MenuItem item, char numericChar, int numericModifiers);
        int getNumericModifiers(MenuItem item);
        void setIconTintList(MenuItem item, ColorStateList tint);
        ColorStateList getIconTintList(MenuItem item);
        void setIconTintMode(MenuItem item, PorterDuff.Mode tintMode);
        PorterDuff.Mode getIconTintMode(MenuItem item);
    }

    /**
     * Interface definition for a callback to be invoked when a menu item marked with {@link
     * #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW} is expanded or collapsed.
     *
     * @see #expandActionView(android.view.MenuItem)
     * @see #collapseActionView(android.view.MenuItem)
     * @see #setShowAsAction(android.view.MenuItem, int)
     *
     * @deprecated Use {@link MenuItem.OnActionExpandListener} directly.
     */
    @Deprecated
    public interface OnActionExpandListener {

        /**
         * Called when a menu item with {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is expanded.
         *
         * @param item Item that was expanded
         * @return true if the item should expand, false if expansion should be suppressed.
         */
        boolean onMenuItemActionExpand(MenuItem item);

        /**
         * Called when a menu item with {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is collapsed.
         *
         * @param item Item that was collapsed
         * @return true if the item should collapse, false if collapsing should be suppressed.
         */
        boolean onMenuItemActionCollapse(MenuItem item);
    }

    static class MenuItemCompatBaseImpl implements MenuVersionImpl {
        @Override
        public void setContentDescription(MenuItem item, CharSequence contentDescription) {
        }

        @Override
        public CharSequence getContentDescription(MenuItem item) {
            return null;
        }

        @Override
        public void setTooltipText(MenuItem item, CharSequence tooltipText) {
        }

        @Override
        public CharSequence getTooltipText(MenuItem item) {
            return null;
        }

        @Override
        public void setShortcut(MenuItem item, char numericChar, char alphaChar,
                int numericModifiers, int alphaModifiers) {
        }

        @Override
        public void setAlphabeticShortcut(MenuItem item, char alphaChar, int alphaModifiers) {
        }

        @Override
        public int getAlphabeticModifiers(MenuItem item) {
            return 0;
        }

        @Override
        public void setNumericShortcut(MenuItem item, char numericChar, int numericModifiers) {
        }

        @Override
        public int getNumericModifiers(MenuItem item) {
            return 0;
        }

        @Override
        public void setIconTintList(MenuItem item, ColorStateList tint) {
        }

        @Override
        public ColorStateList getIconTintList(MenuItem item) {
            return null;
        }

        @Override
        public void setIconTintMode(MenuItem item, PorterDuff.Mode tintMode) {
        }

        @Override
        public PorterDuff.Mode getIconTintMode(MenuItem item) {
            return null;
        }
    }

    @RequiresApi(26)
    static class MenuItemCompatApi26Impl extends MenuItemCompatBaseImpl {
        @Override
        public void setContentDescription(MenuItem item, CharSequence contentDescription) {
            item.setContentDescription(contentDescription);
        }

        @Override
        public CharSequence getContentDescription(MenuItem item) {
            return item.getContentDescription();
        }

        @Override
        public void setTooltipText(MenuItem item, CharSequence tooltipText) {
            item.setTooltipText(tooltipText);
        }

        @Override
        public CharSequence getTooltipText(MenuItem item) {
            return item.getTooltipText();
        }

        @Override
        public void setShortcut(MenuItem item, char numericChar, char alphaChar,
                int numericModifiers, int alphaModifiers) {
            item.setShortcut(numericChar, alphaChar, numericModifiers, alphaModifiers);
        }

        @Override
        public void setAlphabeticShortcut(MenuItem item, char alphaChar, int alphaModifiers) {
            item.setAlphabeticShortcut(alphaChar, alphaModifiers);
        }

        @Override
        public int getAlphabeticModifiers(MenuItem item) {
            return item.getAlphabeticModifiers();
        }

        @Override
        public void setNumericShortcut(MenuItem item, char numericChar, int numericModifiers) {
            item.setNumericShortcut(numericChar, numericModifiers);
        }

        @Override
        public int getNumericModifiers(MenuItem item) {
            return item.getNumericModifiers();
        }

        @Override
        public void setIconTintList(MenuItem item, ColorStateList tint) {
            item.setIconTintList(tint);
        }

        @Override
        public ColorStateList getIconTintList(MenuItem item) {
            return item.getIconTintList();
        }

        @Override
        public void setIconTintMode(MenuItem item, PorterDuff.Mode tintMode) {
            item.setIconTintMode(tintMode);
        }

        @Override
        public PorterDuff.Mode getIconTintMode(MenuItem item) {
            return item.getIconTintMode();
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final MenuVersionImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 26) {
            IMPL = new MenuItemCompatApi26Impl();
        } else {
            IMPL = new MenuItemCompatBaseImpl();
        }
    }

    // -------------------------------------------------------------------

    /**
     * Sets how this item should display in the presence of a compatible Action Bar. If the given
     * item is compatible, this will call the item's supported implementation of
     * {@link MenuItem#setShowAsAction(int)}.
     *
     * @param item - the item to change
     * @param actionEnum - How the item should display.
     *
     * @deprecated Use {@link MenuItem#setShowAsAction(int)} directly.
     */
    @Deprecated
    public static void setShowAsAction(MenuItem item, int actionEnum) {
        item.setShowAsAction(actionEnum);
    }

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     *
     * @param item the item to change
     * @param view View to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     *
     * @see #setShowAsAction(MenuItem, int)
     *
     * @deprecated Use {@link MenuItem#setActionView(View)} directly.
     */
    @Deprecated
    public static MenuItem setActionView(MenuItem item, View view) {
        return item.setActionView(view);
    }

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     * <p>
     *   <strong>Note:</strong> Setting an action view overrides the action provider
     *           set via {@link #setActionProvider(MenuItem, ActionProvider)}.
     * </p>
     *
     * @param item the item to change
     * @param resId Layout resource to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     *
     * @see #setShowAsAction(MenuItem, int)
     *
     * @deprecated Use {@link MenuItem#setActionView(int)} directly.
     */
    @Deprecated
    public static MenuItem setActionView(MenuItem item, int resId) {
        return item.setActionView(resId);
    }

    /**
     * Returns the currently set action view for this menu item.
     *
     * @param item the item to query
     * @return This item's action view
     *
     * @deprecated Use {@link MenuItem#getActionView()} directly.
     */
    @Deprecated
    public static View getActionView(MenuItem item) {
        return item.getActionView();
    }

    /**
     * Sets the {@link ActionProvider} responsible for creating an action view if
     * the item is placed on the action bar. The provider also provides a default
     * action invoked if the item is placed in the overflow menu.
     * <p>
     *   <strong>Note:</strong> Setting an action provider overrides the action view
     *           set via {@link #setActionView(MenuItem, View)}.
     * </p>
     *
     * @param item item to change
     * @param provider The action provider.
     * @return This Item so additional setters can be called.
     *
     * @see ActionProvider
     */
    public static MenuItem setActionProvider(MenuItem item, ActionProvider provider) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).setSupportActionProvider(provider);
        }
        // TODO Wrap the support ActionProvider and assign it
        Log.w(TAG, "setActionProvider: item does not implement SupportMenuItem; ignoring");
        return item;
    }

    /**
     * Gets the {@link ActionProvider}.
     *
     * @return The action provider.
     *
     * @see ActionProvider
     * @see #setActionProvider(MenuItem, ActionProvider)
     */
    public static ActionProvider getActionProvider(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getSupportActionProvider();
        }

        // TODO Wrap the framework ActionProvider and return it
        Log.w(TAG, "getActionProvider: item does not implement SupportMenuItem; returning null");
        return null;
    }

    /**
     * Expand the action view associated with this menu item.
     * The menu item must have an action view set, as well as
     * the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     * If a listener has been set using
     * {@link #setOnActionExpandListener(MenuItem, OnActionExpandListener)}
     * it will have its {@link OnActionExpandListener#onMenuItemActionExpand(MenuItem)}
     * method invoked. The listener may return false from this method to prevent expanding
     * the action view.
     *
     * @return true if the action view was expanded, false otherwise.
     *
     * @deprecated Use {@link MenuItem#expandActionView()} directly.
     */
    @Deprecated
    public static boolean expandActionView(MenuItem item) {
        return item.expandActionView();
    }

    /**
     * Collapse the action view associated with this menu item. The menu item must have an action
     * view set, as well as the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a
     * listener has been set using {@link #setOnActionExpandListener(MenuItem,
     * android.support.v4.view.MenuItemCompat.OnActionExpandListener)}
     * it will have its {@link
     * android.support.v4.view.MenuItemCompat.OnActionExpandListener#onMenuItemActionCollapse(MenuItem)}
     * method invoked. The listener may return false from this method to prevent collapsing
     * the action view.
     *
     * @return true if the action view was collapsed, false otherwise.
     *
     * @deprecated Use {@link MenuItem#collapseActionView()} directly.
     */
    @Deprecated
    public static boolean collapseActionView(MenuItem item) {
        return item.collapseActionView();
    }

    /**
     * Returns true if this menu item's action view has been expanded.
     *
     * @return true if the item's action view is expanded, false otherwise.
     * @see #expandActionView(MenuItem)
     * @see #collapseActionView(MenuItem)
     * @see #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
     * @see android.support.v4.view.MenuItemCompat.OnActionExpandListener
     *
     * @deprecated Use {@link MenuItem#isActionViewExpanded()} directly.
     */
    @Deprecated
    public static boolean isActionViewExpanded(MenuItem item) {
        return item.isActionViewExpanded();
    }

    /**
     * Set an {@link OnActionExpandListener} on this menu
     * item to be notified when the associated action view is expanded or collapsed.
     * The menu item must be configured to expand or collapse its action view using the flag
     * {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     *
     * @param listener Listener that will respond to expand/collapse events
     * @return This menu item instance for call chaining
     *
     * @deprecated Use {@link MenuItem#setOnActionExpandListener(MenuItem.OnActionExpandListener)}
     * directly.
     */
    @Deprecated
    public static MenuItem setOnActionExpandListener(MenuItem item,
            final OnActionExpandListener listener) {
        return item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return listener.onMenuItemActionExpand(item);
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return listener.onMenuItemActionCollapse(item);
            }
        });
    }

    /**
     * Change the content description associated with this menu item.
     *
     * @param item item to change.
     * @param contentDescription The new content description.
     */
    public static void setContentDescription(MenuItem item, CharSequence contentDescription) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setContentDescription(contentDescription);
        } else {
            IMPL.setContentDescription(item, contentDescription);
        }
    }

    /**
     * Retrieve the content description associated with this menu item.
     *
     * @return The content description.
     */
    public static CharSequence getContentDescription(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getContentDescription();
        }
        return IMPL.getContentDescription(item);
    }

    /**
     * Change the tooltip text associated with this menu item.
     *
     * @param item item to change.
     * @param tooltipText The new tooltip text
     */
    public static void setTooltipText(MenuItem item, CharSequence tooltipText) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setTooltipText(tooltipText);
        } else {
            IMPL.setTooltipText(item, tooltipText);
        }
    }

    /**
     * Retrieve the tooltip text associated with this menu item.
     *
     * @return The tooltip text.
     */
    public static CharSequence getTooltipText(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getTooltipText();
        }
        return IMPL.getTooltipText(item);
    }

    /**
     * Change both the numeric and alphabetic shortcut associated with this
     * item. Note that the shortcut will be triggered when the key that
     * generates the given character is pressed along with the corresponding
     * modifier key. Also note that case is not significant and that alphabetic
     * shortcut characters will be handled in lower case.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param numericChar The numeric shortcut key. This is the shortcut when
     *        using a numeric (e.g., 12-key) keyboard.
     * @param numericModifiers The numeric modifier associated with the shortcut. It should
     *        be a combination of {@link KeyEvent#META_META_ON}, {@link KeyEvent#META_CTRL_ON},
     *        {@link KeyEvent#META_ALT_ON}, {@link KeyEvent#META_SHIFT_ON},
     *        {@link KeyEvent#META_SYM_ON}, {@link KeyEvent#META_FUNCTION_ON}.
     * @param alphaChar The alphabetic shortcut key. This is the shortcut when
     *        using a keyboard with alphabetic keys.
     * @param alphaModifiers The alphabetic modifier associated with the shortcut. It should
     *        be a combination of {@link KeyEvent#META_META_ON}, {@link KeyEvent#META_CTRL_ON},
     *        {@link KeyEvent#META_ALT_ON}, {@link KeyEvent#META_SHIFT_ON},
     *        {@link KeyEvent#META_SYM_ON}, {@link KeyEvent#META_FUNCTION_ON}.
     */
    public static void setShortcut(MenuItem item, char numericChar, char alphaChar,
            int numericModifiers, int alphaModifiers) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setShortcut(numericChar, alphaChar, numericModifiers,
                    alphaModifiers);
        } else {
            IMPL.setShortcut(item, numericChar, alphaChar, numericModifiers, alphaModifiers);
        }
    }

    /**
     * Change the numeric shortcut and modifiers associated with this item.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param numericChar The numeric shortcut key.  This is the shortcut when
     *                 using a 12-key (numeric) keyboard.
     * @param numericModifiers The modifier associated with the shortcut. It should
     *        be a combination of {@link KeyEvent#META_META_ON}, {@link KeyEvent#META_CTRL_ON},
     *        {@link KeyEvent#META_ALT_ON}, {@link KeyEvent#META_SHIFT_ON},
     *        {@link KeyEvent#META_SYM_ON}, {@link KeyEvent#META_FUNCTION_ON}.
     */
    public static void setNumericShortcut(MenuItem item, char numericChar, int numericModifiers) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setNumericShortcut(numericChar, numericModifiers);
        } else {
            IMPL.setNumericShortcut(item, numericChar, numericModifiers);
        }
    }

    /**
     * Return the modifiers for this menu item's numeric (12-key) shortcut.
     * The modifier is a combination of {@link KeyEvent#META_META_ON},
     * {@link KeyEvent#META_CTRL_ON}, {@link KeyEvent#META_ALT_ON},
     * {@link KeyEvent#META_SHIFT_ON}, {@link KeyEvent#META_SYM_ON},
     * {@link KeyEvent#META_FUNCTION_ON}.
     * For example, {@link KeyEvent#META_FUNCTION_ON}|{@link KeyEvent#META_CTRL_ON}
     *
     * @return Modifier associated with the numeric shortcut.
     */
    public static int getNumericModifiers(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getNumericModifiers();
        }
        return IMPL.getNumericModifiers(item);
    }

    /**
     * Change the alphabetic shortcut associated with this item. The shortcut
     * will be triggered when the key that generates the given character is
     * pressed along with the modifier keys. Case is not significant and shortcut
     * characters will be displayed in lower case. Note that menu items with
     * the characters '\b' or '\n' as shortcuts will get triggered by the
     * Delete key or Carriage Return key, respectively.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param alphaChar The alphabetic shortcut key. This is the shortcut when
     *        using a keyboard with alphabetic keys.
     * @param alphaModifiers The modifier associated with the shortcut. It should
     *        be a combination of {@link KeyEvent#META_META_ON}, {@link KeyEvent#META_CTRL_ON},
     *        {@link KeyEvent#META_ALT_ON}, {@link KeyEvent#META_SHIFT_ON},
     *        {@link KeyEvent#META_SYM_ON}, {@link KeyEvent#META_FUNCTION_ON}.
     */
    public static void setAlphabeticShortcut(MenuItem item, char alphaChar, int alphaModifiers) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setAlphabeticShortcut(alphaChar, alphaModifiers);
        } else {
            IMPL.setAlphabeticShortcut(item, alphaChar, alphaModifiers);
        }
    }

    /**
     * Return the modifier for this menu item's alphabetic shortcut.
     * The modifier is a combination of {@link KeyEvent#META_META_ON},
     * {@link KeyEvent#META_CTRL_ON}, {@link KeyEvent#META_ALT_ON},
     * {@link KeyEvent#META_SHIFT_ON}, {@link KeyEvent#META_SYM_ON},
     * {@link KeyEvent#META_FUNCTION_ON}.
     * For example, {@link KeyEvent#META_FUNCTION_ON}|{@link KeyEvent#META_CTRL_ON}
     *
     * @return Modifier associated with the keyboard shortcut.
     */
    public static int getAlphabeticModifiers(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getAlphabeticModifiers();
        }
        return IMPL.getAlphabeticModifiers(item);
    }

    /**
     * Applies a tint to the item's icon. Does not modify the
     * current tint mode of that item, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link MenuItem#setIcon(Drawable)} or {@link MenuItem#setIcon(int)} will
     * automatically mutate the icon and apply the specified tint and
     * tint mode.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getIconTintList(MenuItem)
     */
    public static void setIconTintList(MenuItem item, ColorStateList tint) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setIconTintList(tint);
        } else {
            IMPL.setIconTintList(item, tint);
        }
    }

    /**
     * @return the tint applied to the item's icon
     * @see #setIconTintList(MenuItem, ColorStateList)
     */
    public static ColorStateList getIconTintList(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getIconTintList();
        }
        return IMPL.getIconTintList(item);
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIconTintList(MenuItem, ColorStateList)} to the item's icon. The default mode is
     * {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #setIconTintList(MenuItem, ColorStateList)
     */
    public static void setIconTintMode(MenuItem item, PorterDuff.Mode tintMode) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setIconTintMode(tintMode);
        } else {
            IMPL.setIconTintMode(item, tintMode);
        }
    }

    /**
     * Returns the blending mode used to apply the tint to the item's icon, if specified.
     *
     * @return the blending mode used to apply the tint to the item's icon
     * @see #setIconTintMode(MenuItem, PorterDuff.Mode)
     */
    public static PorterDuff.Mode getIconTintMode(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getIconTintMode();
        }
        return IMPL.getIconTintMode(item);
    }

    private MenuItemCompat() {}
}
