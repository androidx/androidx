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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * This class is a mediator for accomplishing a given task, for example sharing a file. It is
 * responsible for creating a view that performs an action that accomplishes the task. This class
 * also implements other functions such a performing a default action.
 *
 * <p class="note"><strong>Note:</strong> This class is included in the <a
 * href="{@docRoot}tools/extras/support-library.html">support library</a> for compatibility
 * with API level 4 and higher. If you're developing your app for API level 14 and higher
 * <em>only</em>, you should instead use the framework {@link android.view.ActionProvider}
 * class.</p>
 *
 * <p>An ActionProvider can be
 * optionally specified for a {@link android.view.MenuItem} and in such a case it will be
 * responsible for
 * creating the action view that appears in the {@link android.app.ActionBar} as a substitute for
 * the menu item when the item is displayed as an action item. Also the provider is responsible for
 * performing a default action if a menu item placed on the overflow menu of the ActionBar is
 * selected and none of the menu item callbacks has handled the selection. For this case the
 * provider can also optionally provide a sub-menu for accomplishing the task at hand.
 *
 * <p>There are two ways for using an action provider for creating and handling of action views:
 *
 * <ul><li> Setting the action provider on a {@link android.view.MenuItem} directly by
 * calling {@link
 * android.support.v4.view.MenuItemCompat#setActionProvider(android.view.MenuItem, ActionProvider)}.
 * </li>
 *
 * <li>Declaring the action provider in the menu XML resource. For example:
 *
 * <pre><code>
 *   &lt;item android:id="@+id/my_menu_item"
 *     android:title="@string/my_menu_item_title"
 *     android:icon="@drawable/my_menu_item_icon"
 *     android:showAsAction="ifRoom"
 *     android:actionProviderClass="foo.bar.SomeActionProvider" /&gt;
 * </code></pre>
 * </li></ul></p>
 *
 * <h3>Creating a custom action provider</h3>
 *
 * <p>To create a custom action provider, extend ActionProvider and implement
 * its callback methods as necessary. In particular, implement the following
 * methods:</p>
 *
 * <dl>
 * <dt>{@link #ActionProvider ActionProvider()} constructor</dt>
 * <dd>This constructor is passed the application context. You should
 * save the context in a member field to use in the other callback methods.</dd>
 *
 * <dt>{@link #onCreateActionView onCreateActionView(MenuItem)}</dt>
 * <dd>The system calls this method when the action provider is created.
 * You define the action provider's layout through the implementation of this
 * method. Use the context acquired
 * from the constructor to instantiate a {@link android.view.LayoutInflater} and
 * inflate your action provider's layout from an XML resource, then hook up
 * event listeners for the view's components. For example:
 *
 *<pre>
 * public View onCreateActionView(MenuItem forItem) {
 *     // Inflate the action provider to be shown on the action bar.
 *     LayoutInflater layoutInflater = LayoutInflater.from(mContext);
 *     View providerView =
 *         layoutInflater.inflate(R.layout.my_action_provider, null);
 *     ImageButton button =
 *         (ImageButton) providerView.findViewById(R.id.button);
 *     button.setOnClickListener(new View.OnClickListener() {
 *         &#64;Override
 *         public void onClick(View v) {
 *             // Do something...
 *         }
 *     });
 *     return providerView;
 * }</pre>
 * </dd>
 *
 * <dt>{@link #onPerformDefaultAction onPerformDefaultAction()}</dt>
 * <dd><p>The system calls this method when the user selects a menu item from the action
 * overflow. The action provider should perform a default action for the
 * menu item. The system does not call this method if the menu item opens a submenu.</p>
 *
 * <p>If your action provider presents a submenu through the
 * {@link #onPrepareSubMenu onPrepareSubMenu()} callback, the submenu
 * appears even if the action provider is in the overflow menu.
 * Thus, the system never calls {@link #onPerformDefaultAction
 * onPerformDefaultAction()} if there is a submenu.</p>
 *
 * <p class="note"> <strong>Note:</strong> An activity or a fragment that
 * implements <code>onOptionsItemSelected()</code> can override the action
 * provider's default behavior (unless it uses a submenu) by handling the
 * item-selected event and returning <code>true</code>. In this case, the
 * system does not call
 * {@link #onPerformDefaultAction onPerformDefaultAction()}.</p></dd>
 * </dl>
 *
 *
 * @see android.support.v4.view.MenuItemCompat#setActionProvider(android.view.MenuItem, ActionProvider)
 * @see android.support.v4.view.MenuItemCompat#getActionProvider(android.view.MenuItem)
 */
public abstract class ActionProvider {
    private static final String TAG = "ActionProvider(support)";
    private final Context mContext;

    private SubUiVisibilityListener mSubUiVisibilityListener;
    private VisibilityListener mVisibilityListener;

    /**
     * Creates a new instance.
     *
     * @param context Context for accessing resources.
     */
    public ActionProvider(Context context) {
        mContext = context;
    }

    /**
     * Gets the context associated with this action provider.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Factory method for creating new action views.
     *
     * @return A new action view.
     */
    public abstract View onCreateActionView();

    /**
     * Factory method called by the Android framework to create new action views.
     * This method returns a new action view for the given MenuItem.
     *
     * <p>If your ActionProvider implementation overrides the deprecated no-argument overload
     * {@link #onCreateActionView()}, overriding this method for devices running API 16 or later
     * is recommended but optional. The default implementation calls {@link #onCreateActionView()}
     * for compatibility with applications written for older platform versions.</p>
     *
     * @param forItem MenuItem to create the action view for
     * @return the new action view
     */
    public View onCreateActionView(MenuItem forItem) {
        return onCreateActionView();
    }

    /**
     * The result of this method determines whether or not {@link #isVisible()} will be used
     * by the {@link MenuItem} this ActionProvider is bound to help determine its visibility.
     *
     * @return true if this ActionProvider overrides the visibility of the MenuItem
     *         it is bound to, false otherwise. The default implementation returns false.
     * @see #isVisible()
     */
    public boolean overridesItemVisibility() {
        return false;
    }

    /**
     * If {@link #overridesItemVisibility()} returns true, the return value of this method
     * will help determine the visibility of the {@link MenuItem} this ActionProvider is bound to.
     *
     * <p>If the MenuItem's visibility is explicitly set to false by the application,
     * the MenuItem will not be shown, even if this method returns true.</p>
     *
     * @return true if the MenuItem this ActionProvider is bound to is visible, false if
     *         it is invisible. The default implementation returns true.
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * If this ActionProvider is associated with an item in a menu,
     * refresh the visibility of the item based on {@link #overridesItemVisibility()} and
     * {@link #isVisible()}. If {@link #overridesItemVisibility()} returns false, this call
     * will have no effect.
     */
    public void refreshVisibility() {
        if (mVisibilityListener != null && overridesItemVisibility()) {
            mVisibilityListener.onActionProviderVisibilityChanged(isVisible());
        }
    }

    /**
     * Performs an optional default action.
     *
     * <p>For the case of an action provider placed in a menu
     * item not shown as an action this method is invoked if previous callbacks for processing menu
     * selection has handled the event.
     *
     * <p> A menu item selection is processed in the following order:
     *
     * <ul><li>Receiving a call to
     * {@link android.view.MenuItem.OnMenuItemClickListener#onMenuItemClick
     * MenuItem.OnMenuItemClickListener.onMenuItemClick}.</li>
     *
     * <li>Receiving a call to
     * {@link android.app.Activity#onOptionsItemSelected(android.view.MenuItem)}
     * FragmentActivity.onOptionsItemSelected(MenuItem)}
     * </li>
     *
     * <li>Receiving a call to
     * {@link android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)}
     * Fragment.onOptionsItemSelected(MenuItem)}</li>
     *
     * <li>Launching the {@link android.content.Intent} set via
     * {@link android.view.MenuItem#setIntent(android.content.Intent)
     * MenuItem.setIntent(android.content.Intent)}
     * </li>
     *
     * <li>Invoking this method.</li></ul>
     *
     * <p>The default implementation does not perform any action and returns false.
     */
    public boolean onPerformDefaultAction() {
        return false;
    }

    /**
     * Determines if this ActionProvider has a submenu associated with it.
     *
     * <p>Associated submenus will be shown when an action view is not. This provider instance will
     * receive a call to {@link #onPrepareSubMenu(SubMenu)} after the call to {@link
     * #onPerformDefaultAction()} and before a submenu is displayed to the user.
     *
     * @return true if the item backed by this provider should have an associated submenu
     */
    public boolean hasSubMenu() {
        return false;
    }

    /**
     * Called to prepare an associated submenu for the menu item backed by this ActionProvider.
     *
     * <p>if {@link #hasSubMenu()} returns true, this method will be called when the menu item is
     * selected to prepare the submenu for presentation to the user. Apps may use this to create or
     * alter submenu content right before display.
     *
     * @param subMenu Submenu that will be displayed
     */
    public void onPrepareSubMenu(SubMenu subMenu) {
    }

    /**
     * Notify the system that the visibility of an action view's sub-UI such as an anchored popup
     * has changed. This will affect how other system visibility notifications occur.
     *
     * @hide Pending future API approval
     */
    @RestrictTo(LIBRARY_GROUP)
    public void subUiVisibilityChanged(boolean isVisible) {
        if (mSubUiVisibilityListener != null) {
            mSubUiVisibilityListener.onSubUiVisibilityChanged(isVisible);
        }
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setSubUiVisibilityListener(SubUiVisibilityListener listener) {
        mSubUiVisibilityListener = listener;
    }

    /**
     * Set a listener to be notified when this ActionProvider's overridden visibility changes.
     * This should only be used by MenuItem implementations.
     *
     * @param listener listener to set
     */
    public void setVisibilityListener(VisibilityListener listener) {
        if (mVisibilityListener != null && listener != null) {
            Log.w(TAG, "setVisibilityListener: Setting a new ActionProvider.VisibilityListener " +
                    "when one is already set. Are you reusing this " + getClass().getSimpleName() +
                    " instance while it is still in use somewhere else?");
        }
        mVisibilityListener = listener;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void reset() {
        mVisibilityListener = null;
        mSubUiVisibilityListener = null;
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface SubUiVisibilityListener {

        public void onSubUiVisibilityChanged(boolean isVisible);
    }

    /**
     * Listens to changes in visibility as reported by {@link ActionProvider#refreshVisibility()}.
     *
     * @see ActionProvider#overridesItemVisibility()
     * @see ActionProvider#isVisible()
     */
    public interface VisibilityListener {
        public void onActionProviderVisibilityChanged(boolean isVisible);
    }
}
