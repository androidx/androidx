/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation.dynamicfeatures

import android.content.ComponentName
import android.net.Uri
import androidx.annotation.IdRes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.get

/**
 * Construct a new [DynamicActivityNavigator.Destination]
 * @param id Destination id.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to build your DynamicActivityDestination instead",
    ReplaceWith("activity(route = id.toString()) { builder.invoke() }")
)
public inline fun DynamicNavGraphBuilder.activity(
    @IdRes id: Int,
    builder: DynamicActivityNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    DynamicActivityNavigatorDestinationBuilder(
        provider[DynamicActivityNavigator::class],
        id
    ).apply(builder)
)

/**
 * Construct a new [DynamicActivityNavigator.Destination]
 * @param route Destination route.
 */
public inline fun DynamicNavGraphBuilder.activity(
    route: String,
    builder: DynamicActivityNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    DynamicActivityNavigatorDestinationBuilder(
        provider[DynamicActivityNavigator::class],
        route
    ).apply(builder)
)

/**
 * DSL for constructing a new [DynamicActivityNavigator.Destination]
 */
@NavDestinationDsl
public class DynamicActivityNavigatorDestinationBuilder :
    NavDestinationBuilder<ActivityNavigator.Destination> {
    private var activityNavigator: DynamicActivityNavigator

    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to build your DynamicActivityDestination instead",
        ReplaceWith(
            "DynamicActivityNavigatorDestinationBuilder(activityNavigator, route = id.toString())"
        )
    )
    public constructor(
        activityNavigator: DynamicActivityNavigator,
        @IdRes id: Int
    ) : super(activityNavigator, id) {
        this.activityNavigator = activityNavigator
    }

    public constructor(
        activityNavigator: DynamicActivityNavigator,
        route: String
    ) : super(activityNavigator, route) {
        this.activityNavigator = activityNavigator
    }

    public var moduleName: String? = null

    public var targetPackage: String? = null

    public var activityClassName: String? = null

    public var action: String? = null

    public var data: Uri? = null

    public var dataPattern: String? = null

    override fun build(): DynamicActivityNavigator.Destination =
        (super.build() as DynamicActivityNavigator.Destination).also { destination ->
            activityClassName?.also {
                destination.setComponentName(
                    ComponentName(
                        if (targetPackage != null) {
                            targetPackage!!
                        } else {
                            activityNavigator.packageName
                        },
                        it
                    )
                )
            }
            destination.setTargetPackage(targetPackage)
            destination.moduleName = moduleName
            destination.setAction(action)
            destination.setData(data)
            destination.setDataPattern(dataPattern)
        }
}
