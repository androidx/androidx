/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.navigation

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.use
import java.util.regex.Pattern

/**
 * ActivityNavigator implements cross-activity navigation.
 */
@Navigator.Name("activity")
public open class ActivityNavigator(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val context: Context
) : Navigator<ActivityNavigator.Destination>() {
    private val hostActivity: Activity? = generateSequence(context) {
        if (it is ContextWrapper) {
            it.baseContext
        } else
            null
    }.firstOrNull {
        it is Activity
    } as Activity?

    override fun createDestination(): Destination {
        return Destination(this)
    }

    override fun popBackStack(): Boolean {
        if (hostActivity != null) {
            hostActivity.finish()
            return true
        }
        return false
    }

    /**
     * Navigate to a destination.
     *
     * <p>Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * NavController will delegate to it when appropriate.</p>
     *
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     * @return The NavDestination that should be added to the back stack or null if
     * no change was made to the back stack (i.e., in cases of single top operations
     * where the destination is already on top of the back stack).
     *
     * @throws IllegalArgumentException if the given destination has no Intent
     */
    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        checkNotNull(destination.intent) {
            ("Destination ${destination.id} does not have an Intent set.")
        }
        val intent = Intent(destination.intent)
        if (args != null) {
            intent.putExtras(args)
            val dataPattern = destination.dataPattern
            if (!dataPattern.isNullOrEmpty()) {
                // Fill in the data pattern with the args to build a valid URI
                val data = StringBuffer()
                val fillInPattern = Pattern.compile("\\{(.+?)\\}")
                val matcher = fillInPattern.matcher(dataPattern)
                while (matcher.find()) {
                    val argName = matcher.group(1)
                    if (args.containsKey(argName)) {
                        matcher.appendReplacement(data, "")
                        data.append(Uri.encode(args[argName].toString()))
                    } else {
                        throw IllegalArgumentException(
                            "Could not find $argName in $args to fill data pattern $dataPattern"
                        )
                    }
                }
                matcher.appendTail(data)
                intent.data = Uri.parse(data.toString())
            }
        }
        if (navigatorExtras is Extras) {
            intent.addFlags(navigatorExtras.flags)
        }
        if (hostActivity == null) {
            // If we're not launching from an Activity context we have to launch in a new task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (navOptions != null && navOptions.shouldLaunchSingleTop()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (hostActivity != null) {
            val hostIntent = hostActivity.intent
            if (hostIntent != null) {
                val hostCurrentId = hostIntent.getIntExtra(EXTRA_NAV_CURRENT, 0)
                if (hostCurrentId != 0) {
                    intent.putExtra(EXTRA_NAV_SOURCE, hostCurrentId)
                }
            }
        }
        val destId = destination.id
        intent.putExtra(EXTRA_NAV_CURRENT, destId)
        val resources = context.resources
        if (navOptions != null) {
            val popEnterAnim = navOptions.popEnterAnim
            val popExitAnim = navOptions.popExitAnim
            if (
                popEnterAnim > 0 && resources.getResourceTypeName(popEnterAnim) == "animator" ||
                popExitAnim > 0 && resources.getResourceTypeName(popExitAnim) == "animator"
            ) {
                Log.w(
                    LOG_TAG,
                    "Activity destinations do not support Animator resource. Ignoring " +
                        "popEnter resource ${resources.getResourceName(popEnterAnim)} and " +
                        "popExit resource ${resources.getResourceName(popExitAnim)} when " +
                        "launching $destination"
                )
            } else {
                // For use in applyPopAnimationsToPendingTransition()
                intent.putExtra(EXTRA_POP_ENTER_ANIM, popEnterAnim)
                intent.putExtra(EXTRA_POP_EXIT_ANIM, popExitAnim)
            }
        }
        if (navigatorExtras is Extras) {
            val activityOptions = navigatorExtras.activityOptions
            if (activityOptions != null) {
                ActivityCompat.startActivity(context, intent, activityOptions.toBundle())
            } else {
                context.startActivity(intent)
            }
        } else {
            context.startActivity(intent)
        }
        if (navOptions != null && hostActivity != null) {
            var enterAnim = navOptions.enterAnim
            var exitAnim = navOptions.exitAnim
            if (
                enterAnim > 0 && (resources.getResourceTypeName(enterAnim) == "animator") ||
                exitAnim > 0 && (resources.getResourceTypeName(exitAnim) == "animator")
            ) {
                Log.w(
                    LOG_TAG,
                    "Activity destinations do not support Animator resource. " +
                        "Ignoring " + "enter resource " + resources.getResourceName(enterAnim) +
                        " and exit resource " + resources.getResourceName(exitAnim) + "when " +
                        "launching " + destination
                )
            } else if (enterAnim >= 0 || exitAnim >= 0) {
                enterAnim = enterAnim.coerceAtLeast(0)
                exitAnim = exitAnim.coerceAtLeast(0)
                hostActivity.overridePendingTransition(enterAnim, exitAnim)
            }
        }

        // You can't pop the back stack from the caller of a new Activity,
        // so we don't add this navigator to the controller's back stack
        return null
    }

    /**
     * NavDestination for activity navigation
     *
     * Construct a new activity destination. This destination is not valid until you set the
     * Intent via [setIntent] or one or more of the other set method.
     *
     * @param activityNavigator The [ActivityNavigator] which this destination
     * will be associated with. Generally retrieved via a
     * [NavController]'s
     * [NavigatorProvider.getNavigator] method.
     */
    @NavDestination.ClassType(Activity::class)
    public open class Destination(
        activityNavigator: Navigator<out Destination>
    ) : NavDestination(activityNavigator) {
        /**
         * The Intent associated with this destination.
         */
        public var intent: Intent? = null
            private set

        /**
         * The dynamic data URI pattern, if any
         */
        public var dataPattern: String? = null
            private set

        /**
         * Set the Intent to start when navigating to this destination.
         * @param intent Intent to associated with this destination.
         * @return this [Destination]
         */
        public fun setIntent(intent: Intent?): Destination {
            this.intent = intent
            return this
        }

        /**
         * Sets a dynamic data URI pattern that is sent when navigating to this destination.
         *
         *
         * If a non-null arguments Bundle is present when navigating, any segments in the form
         * `{argName}` will be replaced with a URI encoded string from the arguments.
         * @param dataPattern A URI pattern with segments in the form of `{argName}` that
         * will be replaced with URI encoded versions of the Strings in the
         * arguments Bundle.
         * @see Destination.setData
         *
         * @return this [Destination]
         */
        public fun setDataPattern(dataPattern: String?): Destination {
            this.dataPattern = dataPattern
            return this
        }

        /**
         * Construct a new activity destination. This destination is not valid until you set the
         * Intent via [setIntent] or one or more of the other set method.
         *
         *
         * @param navigatorProvider The [NavController] which this destination
         * will be associated with.
         */
        public constructor(
            navigatorProvider: NavigatorProvider
        ) : this(navigatorProvider.getNavigator(ActivityNavigator::class.java))

        @CallSuper
        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.resources.obtainAttributes(
                attrs,
                R.styleable.ActivityNavigator
            ).use { array ->
                var targetPackage = array.getString(R.styleable.ActivityNavigator_targetPackage)
                if (targetPackage != null) {
                    targetPackage = targetPackage.replace(
                        NavInflater.APPLICATION_ID_PLACEHOLDER,
                        context.packageName
                    )
                }
                setTargetPackage(targetPackage)
                var className = array.getString(R.styleable.ActivityNavigator_android_name)
                if (className != null) {
                    if (className[0] == '.') {
                        className = context.packageName + className
                    }
                    setComponentName(ComponentName(context, className))
                }
                setAction(array.getString(R.styleable.ActivityNavigator_action))
                val data = array.getString(R.styleable.ActivityNavigator_data)
                if (data != null) {
                    setData(Uri.parse(data))
                }
                setDataPattern(array.getString(R.styleable.ActivityNavigator_dataPattern))
            }
        }

        /**
         * The explicit application package name associated with this destination, if any
         */
        public var targetPackage: String? = null
            private set
            get() = intent?.`package`

        /**
         * Set an explicit application package name that limits
         * the components this destination will navigate to.
         *
         *
         * When inflated from XML, you can use `${applicationId}` as the
         * package name to automatically use [Context.getPackageName].
         *
         * @param packageName packageName to set
         * @return this [Destination]
         */
        public fun setTargetPackage(packageName: String?): Destination {
            if (intent == null) {
                intent = Intent()
            }
            intent!!.setPackage(packageName)
            return this
        }

        /**
         * The explicit [ComponentName] associated with this destination, if any
         */
        public var component: ComponentName? = null
            private set
            get() = intent?.component

        /**
         * Set an explicit [ComponentName] to navigate to.
         *
         * @param name The component name of the Activity to start.
         * @return this [Destination]
         */
        public fun setComponentName(name: ComponentName?): Destination {
            if (intent == null) {
                intent = Intent()
            }
            intent!!.component = name
            return this
        }

        /**
         * The action used to start the Activity, if any
         */
        public var action: String? = null
            private set
            get() = intent?.action

        /**
         * Sets the action sent when navigating to this destination.
         * @param action The action string to use.
         * @return this [Destination]
         */
        public fun setAction(action: String?): Destination {
            if (intent == null) {
                intent = Intent()
            }
            intent!!.action = action
            return this
        }

        /**
         * The data URI used to start the Activity, if any
         */
        public var data: Uri? = null
            private set
            get() = intent?.data

        /**
         * Sets a static data URI that is sent when navigating to this destination.
         *
         *
         * To use a dynamic URI that changes based on the arguments passed in when navigating,
         * use [setDataPattern], which will take precedence when arguments are
         * present.
         *
         * @param data A static URI that should always be used.
         * @see Destination.setDataPattern
         * @return this [Destination]
         */
        public fun setData(data: Uri?): Destination {
            if (intent == null) {
                intent = Intent()
            }
            intent!!.data = data
            return this
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public override fun supportsActions(): Boolean {
            return false
        }

        override fun toString(): String {
            val componentName = component
            val sb = StringBuilder()
            sb.append(super.toString())
            if (componentName != null) {
                sb.append(" class=")
                sb.append(componentName.className)
            } else {
                val action = action
                if (action != null) {
                    sb.append(" action=")
                    sb.append(action)
                }
            }
            return sb.toString()
        }
    }

    /**
     * Extras that can be passed to ActivityNavigator to customize what
     * [ActivityOptionsCompat] and flags are passed through to the call to
     * [ActivityCompat.startActivity].
     */
    public class Extras internal constructor(
        /**
         * The `Intent.FLAG_ACTIVITY_` flags that should be added to the Intent.
         */
        public val flags: Int,
        /**
         * The [ActivityOptionsCompat] that should be used with [ActivityCompat.startActivity].
         */
        public val activityOptions: ActivityOptionsCompat?
    ) : Navigator.Extras {

        /**
         * Builder for constructing new [Extras] instances. The resulting instances are
         * immutable.
         */
        public class Builder {
            private var flags = 0
            private var activityOptions: ActivityOptionsCompat? = null

            /**
             * Adds one or more `Intent.FLAG_ACTIVITY_` flags
             *
             * @param flags the flags to add
             * @return this [Builder]
             */
            public fun addFlags(flags: Int): Builder {
                this.flags = this.flags or flags
                return this
            }

            /**
             * Sets the [ActivityOptionsCompat] that should be used with
             * [ActivityCompat.startActivity].
             *
             * @param activityOptions The [ActivityOptionsCompat] to pass through
             * @return this [Builder]
             */
            public fun setActivityOptions(activityOptions: ActivityOptionsCompat): Builder {
                this.activityOptions = activityOptions
                return this
            }

            /**
             * Constructs the final [Extras] instance.
             *
             * @return An immutable [Extras] instance.
             */
            public fun build(): Extras {
                return Extras(flags, activityOptions)
            }
        }
    }

    public companion object {
        private const val EXTRA_NAV_SOURCE = "android-support-navigation:ActivityNavigator:source"
        private const val EXTRA_NAV_CURRENT = "android-support-navigation:ActivityNavigator:current"
        private const val EXTRA_POP_ENTER_ANIM =
            "android-support-navigation:ActivityNavigator:popEnterAnim"
        private const val EXTRA_POP_EXIT_ANIM =
            "android-support-navigation:ActivityNavigator:popExitAnim"
        private const val LOG_TAG = "ActivityNavigator"

        /**
         * Apply any pop animations in the Intent of the given Activity to a pending transition.
         * This should be used in place of [Activity.overridePendingTransition]
         * to get the appropriate pop animations.
         * @param activity An activity started from the [ActivityNavigator].
         * @see NavOptions.popEnterAnim
         * @see NavOptions.popExitAnim
         */
        @JvmStatic
        public fun applyPopAnimationsToPendingTransition(activity: Activity) {
            val intent = activity.intent ?: return
            var popEnterAnim = intent.getIntExtra(EXTRA_POP_ENTER_ANIM, -1)
            var popExitAnim = intent.getIntExtra(EXTRA_POP_EXIT_ANIM, -1)
            if (popEnterAnim != -1 || popExitAnim != -1) {
                popEnterAnim = if (popEnterAnim != -1) popEnterAnim else 0
                popExitAnim = if (popExitAnim != -1) popExitAnim else 0
                activity.overridePendingTransition(popEnterAnim, popExitAnim)
            }
        }
    }
}
