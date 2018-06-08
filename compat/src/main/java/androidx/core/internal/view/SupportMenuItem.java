/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.core.internal.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.core.view.ActionProvider;

/**
 * Interface for direct access to a previously created menu item.
 *
 * This version extends the one available in the framework to ensures that any necessary
 * elements added in later versions of the framework, are available for all platforms.
 *
 * @see android.view.MenuItem
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public interface SupportMenuItem extends android.view.MenuItem {
    /*
    * These should be kept in sync with attrs.xml enum constants for showAsAction
    */
    /**
     * Never show this item as a button in an Action Bar.
     */
    int SHOW_AS_ACTION_NEVER = 0;
    /**
     * Show this item as a button in an Action Bar if the system decides there is room for it.
     */
    int SHOW_AS_ACTION_IF_ROOM = 1;
    /**
     * Always show this item as a button in an Action Bar.
     * Use sparingly! If too many items are set to always show in the Action Bar it can
     * crowd the Action Bar and degrade the user experience on devices with smaller screens.
     * A good rule of thumb is to have no more than 2 items set to always show at a time.
     */
    int SHOW_AS_ACTION_ALWAYS = 2;

    /**
     * When this item is in the action bar, always show it with a text label even if it also has an
     * icon specified.
     */
    int SHOW_AS_ACTION_WITH_TEXT = 4;

    /**
     * This item's action view collapses to a normal menu item. When expanded, the action view
     * temporarily takes over a larger segment of its container.
     */
    int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

    /**
     * Sets how this item should display in the presence of an Action Bar. The parameter actionEnum
     * is a flag set. One of {@link #SHOW_AS_ACTION_ALWAYS}, {@link #SHOW_AS_ACTION_IF_ROOM}, or
     * {@link #SHOW_AS_ACTION_NEVER} should be used, and you may optionally OR the value with {@link
     * #SHOW_AS_ACTION_WITH_TEXT}. SHOW_AS_ACTION_WITH_TEXT requests that when the item is shown as
     * an action, it should be shown with a text label.
     *
     * @param actionEnum How the item should display. One of {@link #SHOW_AS_ACTION_ALWAYS}, {@link
     *                   #SHOW_AS_ACTION_IF_ROOM}, or {@link #SHOW_AS_ACTION_NEVER}.
     *                   SHOW_AS_ACTION_NEVER is the default.
     * @see android.app.ActionBar
     * @see #setActionView(View)
     */
    @Override
    void setShowAsAction(int actionEnum);

    /**
     * Sets how this item should display in the presence of an Action Bar.
     * The parameter actionEnum is a flag set. One of {@link #SHOW_AS_ACTION_ALWAYS},
     * {@link #SHOW_AS_ACTION_IF_ROOM}, or {@link #SHOW_AS_ACTION_NEVER} should
     * be used, and you may optionally OR the value with {@link #SHOW_AS_ACTION_WITH_TEXT}.
     * SHOW_AS_ACTION_WITH_TEXT requests that when the item is shown as an action,
     * it should be shown with a text label.
     *
     * <p>Note: This method differs from {@link #setShowAsAction(int)} only in that it
     * returns the current MenuItem instance for call chaining.
     *
     * @param actionEnum How the item should display. One of {@link #SHOW_AS_ACTION_ALWAYS}, {@link
     *                   #SHOW_AS_ACTION_IF_ROOM}, or {@link #SHOW_AS_ACTION_NEVER}.
     *                   SHOW_AS_ACTION_NEVER is the default.
     * @return This MenuItem instance for call chaining.
     * @see android.app.ActionBar
     * @see #setActionView(View)
     */
    @Override
    MenuItem setShowAsActionFlags(int actionEnum);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     *
     * <p><strong>Note:</strong> Setting an action view overrides the action provider
     * provider set via {@link #setSupportActionProvider(androidx.core.view.ActionProvider)}. </p>
     *
     * @param view View to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     * @see #setShowAsAction(int)
     */
    @Override
    MenuItem setActionView(View view);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     *
     * <p><strong>Note:</strong> Setting an action view overrides the action provider
     * provider set via {@link #setSupportActionProvider(androidx.core.view.ActionProvider)}. </p>
     *
     * @param resId Layout resource to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     * @see #setShowAsAction(int)
     */
    @Override
    MenuItem setActionView(int resId);

    /**
     * Returns the currently set action view for this menu item.
     *
     * @return This item's action view
     * @see #setActionView(View)
     * @see #setShowAsAction(int)
     */
    @Override
    View getActionView();

    /**
     * Sets the {@link androidx.core.view.ActionProvider} responsible for creating an action view if
     * the item is placed on the action bar. The provider also provides a default
     * action invoked if the item is placed in the overflow menu.
     *
     * <p><strong>Note:</strong> Setting an action provider overrides the action view
     * set via {@link #setActionView(int)} or {@link #setActionView(View)}.
     * </p>
     *
     * @param actionProvider The action provider.
     * @return This Item so additional setters can be called.
     * @see androidx.core.view.ActionProvider
     */
    SupportMenuItem setSupportActionProvider(ActionProvider actionProvider);

    /**
     * Gets the {@link ActionProvider}.
     *
     * @return The action provider.
     * @see ActionProvider
     * @see #setSupportActionProvider(ActionProvider)
     */
    ActionProvider getSupportActionProvider();

    /**
     * Expand the action view associated with this menu item. The menu item must have an action view
     * set, as well as the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a
     * listener has been set using
     * {@link #setSupportOnActionExpandListener(MenuItem.OnActionExpandListener)}
     * it will have its {@link MenuItem.OnActionExpandListener#onMenuItemActionExpand(MenuItem)}
     * method invoked. The listener may return false from this method to prevent expanding the
     * action view.
     *
     * @return true if the action view was expanded, false otherwise.
     */
    @Override
    boolean expandActionView();

    /**
     * Collapse the action view associated with this menu item. The menu item must have an action
     * view set, as well as the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a
     * listener has been set using
     * {@link #setSupportOnActionExpandListener(MenuItem.OnActionExpandListener)}
     * it will have its {@link MenuItem.OnActionExpandListener#onMenuItemActionCollapse(MenuItem)}
     * method invoked. The listener may return false from this method to prevent collapsing the
     * action view.
     *
     * @return true if the action view was collapsed, false otherwise.
     */
    @Override
    boolean collapseActionView();

    /**
     * Returns true if this menu item's action view has been expanded.
     *
     * @return true if the item's action view is expanded, false otherwise.
     * @see #expandActionView()
     * @see #collapseActionView()
     * @see #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
     * @see MenuItem.OnActionExpandListener
     */
    @Override
    boolean isActionViewExpanded();

    /**
     * Change the content description associated with this menu item.
     *
     * @param contentDescription The new content description.
     * @return This menu item instance for call chaining.
     */
    @Override
    SupportMenuItem setContentDescription(CharSequence contentDescription);

    /**
     * Retrieve the content description associated with this menu item.
     *
     * @return The content description.
     */
    @Override
    CharSequence getContentDescription();

    /**
     * Change the tooltip text associated with this menu item.
     *
     * @param tooltipText The new tooltip text.
     * @return This menu item instance for call chaining.
     */
    @Override
    SupportMenuItem setTooltipText(CharSequence tooltipText);

    /**
     * Retrieve the tooltip text associated with this menu item.
     *
     * @return The tooltip text.
     */
    @Override
    CharSequence getTooltipText();

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
     * @return This Item so additional setters can be called.
     */
    @Override
    MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers,
            int alphaModifiers);

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
     * @return This Item so additional setters can be called.
     */
    @Override
    MenuItem setNumericShortcut(char numericChar, int numericModifiers);

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
    @Override
    int getNumericModifiers();

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
     * @return This Item so additional setters can be called.
     */
    @Override
    MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers);

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
    @Override
    int getAlphabeticModifiers();

    /**
     * Applies a tint to this item's icon. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link MenuItem#setIcon(Drawable)} or {@link MenuItem#setIcon(int)} will
     * automatically mutate the icon and apply the specified tint and
     * tint mode.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getIconTintList()
     */
    @Override
    MenuItem setIconTintList(ColorStateList tint);

    /**
     * @return the tint applied to this item's icon
     * @see #setIconTintList(ColorStateList)
     */
    @Override
    ColorStateList getIconTintList();

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIconTintList(ColorStateList)} to this item's icon. The default mode is
     * {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #setIconTintList(ColorStateList)
     */
    @Override
    MenuItem setIconTintMode(PorterDuff.Mode tintMode);

    /**
     * Returns the blending mode used to apply the tint to this item's icon, if specified.
     *
     * @return the blending mode used to apply the tint to this item's icon
     * @see #setIconTintMode(PorterDuff.Mode)
     */
    @Override
    PorterDuff.Mode getIconTintMode();
}