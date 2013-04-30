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

import android.support.v4.internal.view.SupportMenuItem;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

/**
 * Helper for accessing features in {@link android.view.MenuItem}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class MenuItemCompat {
    private static final String TAG = "MenuItemCompat";

    /**
     * Never show this item as a button in an Action Bar.
     */
    public static final int SHOW_AS_ACTION_NEVER = 0;

    /**
     * Show this item as a button in an Action Bar if the system
     * decides there is room for it.
     */
    public static final int SHOW_AS_ACTION_IF_ROOM = 1;

    /**
     * Always show this item as a button in an Action Bar. Use sparingly!
     * If too many items are set to always show in the Action Bar it can
     * crowd the Action Bar and degrade the user experience on devices with
     * smaller screens. A good rule of thumb is to have no more than 2
     * items set to always show at a time.
     */
    public static final int SHOW_AS_ACTION_ALWAYS = 2;

    /**
     * When this item is in the action bar, always show it with a
     * text label even if it also has an icon specified.
     */
    public static final int SHOW_AS_ACTION_WITH_TEXT = 4;

    /**
     * This item's action view collapses to a normal menu item.
     * When expanded, the action view temporarily takes over
     * a larger segment of its container.
     */
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

    /**
     * Interface for the full API.
     */
    interface MenuVersionImpl {
        boolean setShowAsAction(MenuItem item, int actionEnum);
        MenuItem setActionView(MenuItem item, View view);
        MenuItem setActionView(MenuItem item, int resId);
        View getActionView(MenuItem item);
        boolean expandActionView(MenuItem item);
        boolean collapseActionView(MenuItem item);
        boolean isActionViewExpanded(MenuItem item);
        MenuItem setOnActionExpandListener(MenuItem item, OnActionExpandListener listener);
    }

    /**
     * Interface definition for a callback to be invoked when a menu item marked with {@link
     * #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW} is expanded or collapsed.
     *
     * @see #expandActionView(android.view.MenuItem)
     * @see #collapseActionView(android.view.MenuItem)
     * @see #setShowAsAction(android.view.MenuItem, int)
     */
    public interface OnActionExpandListener {

        /**
         * Called when a menu item with {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is expanded.
         *
         * @param item Item that was expanded
         * @return true if the item should expand, false if expansion should be suppressed.
         */
        public boolean onMenuItemActionExpand(MenuItem item);

        /**
         * Called when a menu item with {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is collapsed.
         *
         * @param item Item that was collapsed
         * @return true if the item should collapse, false if collapsing should be suppressed.
         */
        public boolean onMenuItemActionCollapse(MenuItem item);
    }

    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class BaseMenuVersionImpl implements MenuVersionImpl {
        @Override
        public boolean setShowAsAction(MenuItem item, int actionEnum) {
            return false;
        }

        @Override
        public MenuItem setActionView(MenuItem item, View view) {
            return item;
        }

        @Override
        public MenuItem setActionView(MenuItem item, int resId) {
            return item;
        }

        @Override
        public View getActionView(MenuItem item) {
            return null;
        }

        @Override
        public boolean expandActionView(MenuItem item) {
            return false;
        }

        @Override
        public boolean collapseActionView(MenuItem item) {
            return false;
        }

        @Override
        public boolean isActionViewExpanded(MenuItem item) {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(MenuItem item, OnActionExpandListener listener) {
            return item;
        }
    }

    /**
     * Interface implementation for devices with at least v11 APIs.
     */
    static class HoneycombMenuVersionImpl implements MenuVersionImpl {
        @Override
        public boolean setShowAsAction(MenuItem item, int actionEnum) {
            MenuItemCompatHoneycomb.setShowAsAction(item, actionEnum);
            return true;
        }
        @Override
        public MenuItem setActionView(MenuItem item, View view) {
            return MenuItemCompatHoneycomb.setActionView(item, view);
        }

        @Override
        public MenuItem setActionView(MenuItem item, int resId) {
            return MenuItemCompatHoneycomb.setActionView(item, resId);
        }

        @Override
        public View getActionView(MenuItem item) {
            return MenuItemCompatHoneycomb.getActionView(item);
        }

        @Override
        public boolean expandActionView(MenuItem item) {
            return false;
        }

        @Override
        public boolean collapseActionView(MenuItem item) {
            return false;
        }

        @Override
        public boolean isActionViewExpanded(MenuItem item) {
            return false;
        }

        @Override
        public MenuItem setOnActionExpandListener(MenuItem item, OnActionExpandListener listener) {
            return item;
        }
    }

    static class IcsMenuVersionImpl extends HoneycombMenuVersionImpl {
        @Override
        public boolean expandActionView(MenuItem item) {
            return MenuItemCompatIcs.expandActionView(item);
        }

        @Override
        public boolean collapseActionView(MenuItem item) {
            return MenuItemCompatIcs.collapseActionView(item);
        }

        @Override
        public boolean isActionViewExpanded(MenuItem item) {
            return MenuItemCompatIcs.isActionViewExpanded(item);
        }

        @Override
        public MenuItem setOnActionExpandListener(MenuItem item,
                final OnActionExpandListener listener) {
            if (listener == null) {
                return MenuItemCompatIcs.setOnActionExpandListener(item, null);
            }
            /*
             * MenuItemCompatIcs is a dependency of this segment of the support lib
             * but not the other way around, so we need to take an extra step here to proxy
             * to the right types.
             */
            return MenuItemCompatIcs.setOnActionExpandListener(item,
                    new MenuItemCompatIcs.SupportActionExpandProxy() {
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
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final MenuVersionImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 14) {
            IMPL = new IcsMenuVersionImpl();
        } else if (version >= 11) {
            IMPL = new HoneycombMenuVersionImpl();
        } else {
            IMPL = new BaseMenuVersionImpl();
        }
    }

    // -------------------------------------------------------------------

    /**
     * Call {@link MenuItem#setShowAsAction(int) MenuItem.setShowAsAction()}.
     * If running on a pre-{@link android.os.Build.VERSION_CODES#HONEYCOMB} device
     * and <code>item</code> does not implement {@link android.support.v4.internal.view.SupportMenuItem},
     * this method does nothing and returns false.  Otherwise returns true.
     */
    public static boolean setShowAsAction(MenuItem item, int actionEnum) {
        if (item instanceof SupportMenuItem) {
            ((SupportMenuItem) item).setShowAsAction(actionEnum);
            return true;
        }
        return IMPL.setShowAsAction(item, actionEnum);
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
     */
    public static MenuItem setActionView(MenuItem item, View view) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).setActionView(view);
        }
        return IMPL.setActionView(item, view);
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
     */
    public static MenuItem setActionView(MenuItem item, int resId) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).setActionView(resId);
        }
        return IMPL.setActionView(item, resId);
    }

    /**
     * Returns the currently set action view for this menu item.
     *
     * @param item the item to query
     * @return This item's action view
     */
    public static View getActionView(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).getActionView();
        }
        return IMPL.getActionView(item);
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
     */
    public static boolean expandActionView(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).expandActionView();
        }
        return IMPL.expandActionView(item);
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
     */
    public static boolean collapseActionView(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).collapseActionView();
        }
        return IMPL.collapseActionView(item);
    }

    /**
     * Returns true if this menu item's action view has been expanded.
     *
     * @return true if the item's action view is expanded, false otherwise.
     * @see #expandActionView(MenuItem)
     * @see #collapseActionView(MenuItem)
     * @see #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
     * @see android.support.v4.view.MenuItemCompat.OnActionExpandListener
     */
    public static boolean isActionViewExpanded(MenuItem item) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).isActionViewExpanded();
        }
        return IMPL.isActionViewExpanded(item);
    }

    /**
     * Set an {@link android.support.v4.view.MenuItemCompat.OnActionExpandListener} on this menu
     * item to be notified when the associated action view is expanded or collapsed.
     * The menu item must be configured to expand or collapse its action view using the flag
     * {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     *
     * @param listener Listener that will respond to expand/collapse events
     * @return This menu item instance for call chaining
     */
    public static MenuItem setOnActionExpandListener(MenuItem item,
            OnActionExpandListener listener) {
        if (item instanceof SupportMenuItem) {
            return ((SupportMenuItem) item).setSupportOnActionExpandListener(listener);
        }
        return IMPL.setOnActionExpandListener(item, listener);
    }
}
