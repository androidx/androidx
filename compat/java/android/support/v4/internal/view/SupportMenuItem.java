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

package android.support.v4.internal.view;

import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;
import android.view.View;

/**
 * Interface for direct access to a previously created menu item.
 *
 * This version extends the one available in the framework to ensures that any necessary
 * elements added in later versions of the framework, are available for all platforms.
 *
 * @see android.view.MenuItem
 * @hide
 */
public interface SupportMenuItem extends android.view.MenuItem {
    /*
    * These should be kept in sync with attrs.xml enum constants for showAsAction
    */
    /**
     * Never show this item as a button in an Action Bar.
     */
    public static final int SHOW_AS_ACTION_NEVER = 0;
    /**
     * Show this item as a button in an Action Bar if the system decides there is room for it.
     */
    public static final int SHOW_AS_ACTION_IF_ROOM = 1;
    /**
     * Always show this item as a button in an Action Bar.
     * Use sparingly! If too many items are set to always show in the Action Bar it can
     * crowd the Action Bar and degrade the user experience on devices with smaller screens.
     * A good rule of thumb is to have no more than 2 items set to always show at a time.
     */
    public static final int SHOW_AS_ACTION_ALWAYS = 2;

    /**
     * When this item is in the action bar, always show it with a text label even if it also has an
     * icon specified.
     */
    public static final int SHOW_AS_ACTION_WITH_TEXT = 4;

    /**
     * This item's action view collapses to a normal menu item. When expanded, the action view
     * temporarily takes over a larger segment of its container.
     */
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

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
    public void setShowAsAction(int actionEnum);

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
    public MenuItem setShowAsActionFlags(int actionEnum);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     *
     * <p><strong>Note:</strong> Setting an action view overrides the action provider
     * provider set via {@link #setSupportActionProvider(android.support.v4.view.ActionProvider)}. </p>
     *
     * @param view View to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     * @see #setShowAsAction(int)
     */
    public MenuItem setActionView(View view);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     *
     * <p><strong>Note:</strong> Setting an action view overrides the action provider
     * provider set via {@link #setSupportActionProvider(android.support.v4.view.ActionProvider)}. </p>
     *
     * @param resId Layout resource to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     * @see #setShowAsAction(int)
     */
    public MenuItem setActionView(int resId);

    /**
     * Returns the currently set action view for this menu item.
     *
     * @return This item's action view
     * @see #setActionView(View)
     * @see #setShowAsAction(int)
     */
    public View getActionView();

    /**
     * Sets the {@link android.support.v4.view.ActionProvider} responsible for creating an action view if
     * the item is placed on the action bar. The provider also provides a default
     * action invoked if the item is placed in the overflow menu.
     *
     * <p><strong>Note:</strong> Setting an action provider overrides the action view
     * set via {@link #setActionView(int)} or {@link #setActionView(View)}.
     * </p>
     *
     * @param actionProvider The action provider.
     * @return This Item so additional setters can be called.
     * @see android.support.v4.view.ActionProvider
     */
    public SupportMenuItem setSupportActionProvider(ActionProvider actionProvider);

    /**
     * Gets the {@link ActionProvider}.
     *
     * @return The action provider.
     * @see ActionProvider
     * @see #setSupportActionProvider(ActionProvider)
     */
    public ActionProvider getSupportActionProvider();

    /**
     * Expand the action view associated with this menu item. The menu item must have an action view
     * set, as well as the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a
     * listener has been set using {@link #setSupportOnActionExpandListener(android.support.v4.view.MenuItemCompat.OnActionExpandListener)}
     * it will have its {@link android.support.v4.view.MenuItemCompat.OnActionExpandListener#onMenuItemActionExpand(MenuItem)} method
     * invoked. The listener may return false from this method to prevent expanding the action view.
     *
     * @return true if the action view was expanded, false otherwise.
     */
    public boolean expandActionView();

    /**
     * Collapse the action view associated with this menu item. The menu item must have an action
     * view set, as well as the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a
     * listener has been set using {@link #setSupportOnActionExpandListener(android.support.v4.view.MenuItemCompat.OnActionExpandListener)}
     * it will have its {@link android.support.v4.view.MenuItemCompat.OnActionExpandListener#onMenuItemActionCollapse(MenuItem)} method
     * invoked. The listener may return false from this method to prevent collapsing the action
     * view.
     *
     * @return true if the action view was collapsed, false otherwise.
     */
    public boolean collapseActionView();

    /**
     * Returns true if this menu item's action view has been expanded.
     *
     * @return true if the item's action view is expanded, false otherwise.
     * @see #expandActionView()
     * @see #collapseActionView()
     * @see #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
     * @see android.support.v4.view.MenuItemCompat.OnActionExpandListener
     */
    public boolean isActionViewExpanded();

    /**
     * Set an {@link android.support.v4.view.MenuItemCompat.OnActionExpandListener} on this menu item to be notified when the associated
     * action view is expanded or collapsed. The menu item must be configured to expand or collapse
     * its action view using the flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     *
     * @param listener Listener that will respond to expand/collapse events
     * @return This menu item instance for call chaining
     */
    public SupportMenuItem setSupportOnActionExpandListener(MenuItemCompat.OnActionExpandListener listener);
}