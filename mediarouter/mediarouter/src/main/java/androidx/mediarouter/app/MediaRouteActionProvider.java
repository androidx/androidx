/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.mediarouter.app;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ActionProvider;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouterParams;

/**
 * The media route action provider displays a {@link MediaRouteButton media route button}
 * in the application's {@link ActionBar} to allow the user to select routes and
 * to control the currently selected route.
 * <p>
 * The application must specify the kinds of routes that the user should be allowed
 * to select by specifying a {@link MediaRouteSelector selector} with the
 * {@link #setRouteSelector} method.
 * </p><p>
 * Refer to {@link MediaRouteButton} for a description of the button that will
 * appear in the action bar menu.
 * </p>
 *
 * <h3>Prerequisites</h3>
 * <p>
 * To use the media route action provider, the activity must be a subclass of
 * {@link AppCompatActivity} from the <code>android.support.v7.appcompat</code>
 * support library.  Refer to support library documentation for details.
 * </p>
 *
 * <h3>Example</h3>
 * <p>
 * </p><p>
 * The application should define a menu resource to include the provider in the
 * action bar options menu.  Note that the support library action bar uses attributes
 * that are defined in the application's resource namespace rather than the framework's
 * resource namespace to configure each item.
 * </p><pre>
 * &lt;menu xmlns:android="http://schemas.android.com/apk/res/android"
 *         xmlns:app="http://schemas.android.com/apk/res-auto">
 *     &lt;item android:id="@+id/media_route_menu_item"
 *         android:title="@string/media_route_menu_title"
 *         app:showAsAction="always"
 *         app:actionProviderClass="androidx.mediarouter.app.MediaRouteActionProvider"/>
 * &lt;/menu>
 * </pre><p>
 * Then configure the menu and set the route selector for the chooser.
 * </p><pre>
 * public class MyActivity extends AppCompatActivity {
 *     private MediaRouter mRouter;
 *     private MediaRouter.Callback mCallback;
 *     private MediaRouteSelector mSelector;
 *
 *     protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *
 *         mRouter = Mediarouter.getInstance(this);
 *         mSelector = new MediaRouteSelector.Builder()
 *                 .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
 *                 .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
 *                 .build();
 *         mCallback = new MyCallback();
 *     }
 *
 *     // Add the callback on start to tell the media router what kinds of routes
 *     // the application is interested in so that it can try to discover suitable ones.
 *     public void onStart() {
 *         super.onStart();
 *
 *         mediaRouter.addCallback(mSelector, mCallback,
 *                 MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
 *
 *         MediaRouter.RouteInfo route = mediaRouter.updateSelectedRoute(mSelector);
 *         // do something with the route...
 *     }
 *
 *     // Remove the callback flag CALLBACK_FLAG_REQUEST_DISCOVERY on stop by calling
 *     // addCallback() again in order to tell the media router that it no longer
 *     // needs to invest effort trying to discover routes of these kinds for now.
 *     public void onStop() {
 *         mRouter.addCallback(mSelector, mCallback, &#47;* flags= *&#47; 0);
 *
 *         super.onStop();
 *     }
 *
 *     // Remove the callback when the activity is destroyed.
 *     public void onDestroy() {
 *         mRouter.removeCallback(mCallback);
 *
 *         super.onDestroy();
 *     }
 *
 *     public boolean onCreateOptionsMenu(Menu menu) {
 *         super.onCreateOptionsMenu(menu);
 *
 *         getMenuInflater().inflate(R.menu.sample_media_router_menu, menu);
 *
 *         MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
 *         MediaRouteActionProvider mediaRouteActionProvider =
 *                 (MediaRouteActionProvider)MenuItemCompat.getActionProvider(mediaRouteMenuItem);
 *         mediaRouteActionProvider.setRouteSelector(mSelector);
 *         return true;
 *     }
 *
 *     private final class MyCallback extends MediaRouter.Callback {
 *         // Implement callback methods as needed.
 *     }
 * }
 * </pre>
 *
 * @see #setRouteSelector
 */
public class MediaRouteActionProvider extends ActionProvider {
    private static final String TAG = "MRActionProvider";

    private final MediaRouter mRouter;
    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private MediaRouteDialogFactory mDialogFactory = MediaRouteDialogFactory.getDefault();
    private MediaRouteButton mButton;

    /**
     * Creates the action provider.
     *
     * @param context The context.
     */
    public MediaRouteActionProvider(@NonNull Context context) {
        super(context);

        mRouter = MediaRouter.getInstance(context);
    }

    /**
     * Gets the media route selector for filtering the routes that the user can
     * select using the media route chooser dialog.
     *
     * @return The selector, never null.
     */
    @NonNull
    public MediaRouteSelector getRouteSelector() {
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can
     * select using the media route chooser dialog.
     *
     * @param selector The selector, must not be null.
     */
    public void setRouteSelector(@NonNull MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        if (!mSelector.equals(selector)) {
            mSelector = selector;

            if (mButton != null) {
                mButton.setRouteSelector(selector);
            }
        }
    }

    /**
     * Enables dynamic group feature.
     * With this enabled, a different set of {@link MediaRouteChooserDialog} and
     * {@link MediaRouteControllerDialog} is shown when the button is clicked.
     * If a {@link androidx.mediarouter.media.MediaRouteProvider media route provider}
     * supports dynamic group, the users can use that feature with the dialogs.
     *
     * @see MediaRouteButton#enableDynamicGroup()
     * @see androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController
     *
     * @deprecated Use {@link
     * androidx.mediarouter.media.MediaRouterParams.Builder#setDialogType(int)} with
     * {@link androidx.mediarouter.media.MediaRouterParams#DIALOG_TYPE_DYNAMIC_GROUP} instead.
     */
    @Deprecated
    public void enableDynamicGroup() {
        MediaRouterParams oldParams = mRouter.getRouterParams();
        MediaRouterParams.Builder newParamsBuilder = oldParams == null
                ? new MediaRouterParams.Builder() : new MediaRouterParams.Builder(oldParams);
        newParamsBuilder.setDialogType(MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP);
        mRouter.setRouterParams(newParamsBuilder.build());
    }

    /**
     * Does nothing. You should not call this method.
     *
     * @deprecated The visibility of the button is no longer controlled from {@link ActionProvider}.
     */
    @Deprecated
    public void setAlwaysVisible(boolean alwaysVisible) {
        // No-op.
    }

    /**
     * Gets the media route dialog factory to use when showing the route chooser
     * or controller dialog.
     *
     * @return The dialog factory, never null.
     */
    @NonNull
    public MediaRouteDialogFactory getDialogFactory() {
        return mDialogFactory;
    }

    /**
     * Sets the media route dialog factory to use when showing the route chooser
     * or controller dialog.
     *
     * @param factory The dialog factory, must not be null.
     */
    public void setDialogFactory(@NonNull MediaRouteDialogFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }

        if (mDialogFactory != factory) {
            mDialogFactory = factory;

            if (mButton != null) {
                mButton.setDialogFactory(factory);
            }
        }
    }

    /**
     * Gets the associated media route button, or null if it has not yet been created.
     */
    @Nullable
    public MediaRouteButton getMediaRouteButton() {
        return mButton;
    }

    /**
     * Called when the media route button is being created.
     * <p>
     * Subclasses may override this method to customize the button.
     * </p>
     */
    @NonNull
    public MediaRouteButton onCreateMediaRouteButton() {
        return new MediaRouteButton(getContext());
    }

    @Override
    @NonNull
    public View onCreateActionView() {
        if (mButton != null) {
            Log.e(TAG, "onCreateActionView: this ActionProvider is already associated " +
                    "with a menu item. Don't reuse MediaRouteActionProvider instances! " +
                    "Abandoning the old menu item...");
        }

        mButton = onCreateMediaRouteButton();
        mButton.setCheatSheetEnabled(true);
        mButton.setRouteSelector(mSelector);
        mButton.setDialogFactory(mDialogFactory);
        mButton.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return mButton;
    }

    @Override
    public boolean onPerformDefaultAction() {
        if (mButton != null) {
            return mButton.showDialog();
        }
        return false;
    }
}
