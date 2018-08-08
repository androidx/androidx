// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package androidx.ui.engine.platform.io.plugin.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager

import androidx.ui.engine.platform.io.platform.PlatformViewRegistry
import androidx.ui.engine.platform.io.view.FlutterNativeView
import androidx.ui.engine.platform.io.view.FlutterView

/**
 * Registry used by plugins to set up interaction with Android APIs.
 *
 *
 * Flutter applications by default include an auto-generated and auto-updated
 * plugin registrant class (GeneratedPluginRegistrant) that makes use of a
 * [PluginRegistry] to register contributions from each plugin mentioned
 * in the application's pubspec file. The generated registrant class is, again
 * by default, called from the application's main [Activity], which
 * defaults to an instance of [io.flutter.app.FlutterActivity], itself a
 * [PluginRegistry].
 */
interface PluginRegistry {
    /**
     * Returns a [Registrar] for receiving the registrations pertaining
     * to the specified plugin.
     *
     * @param pluginKey a unique String identifying the plugin; typically the
     * fully qualified name of the plugin's main class.
     */
    fun registrarFor(pluginKey: String): Registrar

    /**
     * Returns whether the specified plugin is known to this registry.
     *
     * @param pluginKey a unique String identifying the plugin; typically the
     * fully qualified name of the plugin's main class.
     * @return true if this registry has handed out a registrar for the
     * specified plugin.
     */
    fun hasPlugin(pluginKey: String): Boolean

    /**
     * Returns the value published by the specified plugin, if any.
     *
     *
     * Plugins may publish a single value, such as an instance of the
     * plugin's main class, for situations where external control or
     * interaction is needed. Clients are expected to know the value's
     * type.
     *
     * @param pluginKey a unique String identifying the plugin; typically the
     * fully qualified name of the plugin's main class.
     * @return the published value, possibly null.
     */
    fun <T> valuePublishedByPlugin(pluginKey: String): T

    /**
     * Receiver of registrations from a single plugin.
     */
    interface Registrar {
        /**
         * Returns the [Activity] that forms the plugin's operating context.
         *
         *
         * Plugin authors should not assume the type returned by this method
         * is any specific subclass of `Activity` (such as
         * [io.flutter.app.FlutterActivity] or
         * [io.flutter.app.FlutterFragmentActivity]), as applications
         * are free to use any activity subclass.
         *
         *
         * When there is no foreground activity in the application, this
         * will return null. If a [Context] is needed, use context() to
         * get the application's context.
         */
        fun activity(): Activity

        /**
         * Returns the [android.app.Application]'s [Context].
         */
        fun context(): Context

        /**
         * Returns the active [Context].
         *
         * @return the current [Activity][.activity], if not null, otherwise the [Application][.context].
         */
        fun activeContext(): Context

        /**
         * Returns a [TextureRegistry] which the plugin can use for
         * managing backend textures.
         */
        // fun textures(): TextureRegistry

        /**
         * Returns the application's [PlatformViewRegistry].
         *
         * Plugins can use the platform registry to register their view factories.
         */
        fun platformViewRegistry(): PlatformViewRegistry

        /**
         * Returns the [FlutterView] that's instantiated by this plugin's
         * [activity][.activity].
         */
        fun view(): FlutterView

        /**
         * Returns the file name for the given asset.
         * The returned file name can be used to access the asset in the APK
         * through the [AssetManager] API.
         *
         * @param asset the name of the asset. The name can be hierarchical
         * @return the filename to be used with [AssetManager]
         */
        fun lookupKeyForAsset(asset: String): String

        /**
         * Returns the file name for the given asset which originates from the
         * specified packageName. The returned file name can be used to access
         * the asset in the APK through the [AssetManager] API.
         *
         * @param asset the name of the asset. The name can be hierarchical
         * @param packageName the name of the package from which the asset originates
         * @return the file name to be used with [AssetManager]
         */
        fun lookupKeyForAsset(asset: String, packageName: String): String

        /**
         * Publishes a value associated with the plugin being registered.
         *
         *
         * The published value is available to interested clients via
         * [PluginRegistry.valuePublishedByPlugin].
         *
         *
         * Publication should be done only when client code needs to interact
         * with the plugin in a way that cannot be accomplished by the plugin
         * registering callbacks with client APIs.
         *
         *
         * Overwrites any previously published value.
         *
         * @param value the value, possibly null.
         * @return this [Registrar].
         */
        fun publish(value: Any): Registrar

        /**
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to `Activity#onRequestPermissionsResult(int, String[], int[])`
         * or `android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])`.
         *
         * @param listener a [RequestPermissionsResultListener] callback.
         * @return this [Registrar].
         */
        fun addRequestPermissionsResultListener(
            listener: RequestPermissionsResultListener
        ): Registrar

        /*
         * Method addRequestPermissionResultListener(RequestPermissionResultListener listener)
         * was made unavailable on 2018-02-28, leaving this comment as a temporary
         * tombstone for reference. This comment will be removed on 2018-03-28
         * (or at least four weeks after the unavailability is released).
         *
         * https://github.com/flutter/flutter/wiki/Changelog#typo-fixed-in-flutter-engine-android-api
         *
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to {@code Activity#onRequestPermissionsResult(int, String[], int[])}
         * or {@code android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])}.
         *
         * @param listener a {@link RequestPermissionResultListener} callback.
         * @return this {@link Registrar}.
         * @deprecated on 2018-01-02 because of misspelling. This method will be made unavailable
         * on 2018-02-06 (or at least four weeks after the deprecation is released). Use
         * {@link #addRequestPermissionsResultListener(RequestPermissionsResultListener)} instead.
         */

        /**
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to [Activity.onActivityResult].
         *
         * @param listener an [ActivityResultListener] callback.
         * @return this [Registrar].
         */
        fun addActivityResultListener(listener: ActivityResultListener): Registrar

        /**
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to [Activity.onNewIntent].
         *
         * @param listener a [NewIntentListener] callback.
         * @return this [Registrar].
         */
        fun addNewIntentListener(listener: NewIntentListener): Registrar

        /**
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to [Activity.onUserLeaveHint].
         *
         * @param listener a [UserLeaveHintListener] callback.
         * @return this [Registrar].
         */
        fun addUserLeaveHintListener(listener: UserLeaveHintListener): Registrar

        /**
         * Adds a callback allowing the plugin to take part in handling incoming
         * calls to [Activity.onDestroy].
         *
         * @param listener a [ViewDestroyListener] callback.
         * @return this [Registrar].
         */
        fun addViewDestroyListener(listener: ViewDestroyListener): Registrar
    }

    /**
     * Delegate interface for handling result of permissions requests on
     * behalf of the main [Activity].
     */
    interface RequestPermissionsResultListener {
        /**
         * @return true if the result has been handled.
         */
        fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ): Boolean
    }

    /*
     * interface RequestPermissionResultListener was made unavailable on
     * 2018-02-28, leaving this comment as a temporary tombstone for reference.
     * This comment will be removed on 2018-03-28 (or at least four weeks after
     * the unavailability is released).
     *
     * https://github.com/flutter/flutter/wiki/Changelog#typo-fixed-in-flutter-engine-android-api
     *
     * Delegate interface for handling result of permissions requests on
     * behalf of the main {@link Activity}.
     *
     * Deprecated on 2018-01-02 because of misspelling. This interface will be made
     * unavailable on 2018-02-06 (or at least four weeks after the deprecation is released).
     * Use {@link RequestPermissionsResultListener} instead.
     */

    /**
     * Delegate interface for handling activity results on behalf of the main
     * [Activity].
     */
    interface ActivityResultListener {
        /**
         * @return true if the result has been handled.
         */
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean
    }

    /**
     * Delegate interface for handling new intents on behalf of the main
     * [Activity].
     */
    interface NewIntentListener {
        /**
         * @return true if the new intent has been handled.
         */
        fun onNewIntent(intent: Intent): Boolean
    }

    /**
     * Delegate interface for handling user leave hints on behalf of the main
     * [Activity].
     */
    interface UserLeaveHintListener {
        fun onUserLeaveHint()
    }

    /**
     * Delegate interface for handling an [Activity]'s onDestroy
     * method being called. A plugin that implements this interface can
     * adopt the FlutterNativeView by retaining a reference and returning true.
     */
    interface ViewDestroyListener {
        fun onViewDestroy(view: FlutterNativeView): Boolean
    }

    /**
     * Callback interface for registering plugins with a plugin registry.
     *
     *
     * For example, an Application may use this callback interface to
     * provide a background service with a callback for calling its
     * GeneratedPluginRegistrant.registerWith method.
     */
    interface PluginRegistrantCallback {
        fun registerWith(registry: PluginRegistry)
    }
}