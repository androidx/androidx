/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent

import android.content.res.TypedArray
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIconStyle
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory
import androidx.car.app.sample.showcase.common.utils.CarContextAware
import androidx.car.app.sample.showcase.common.utils.createAction

/** Simple demo of how to present a map template with a message. */
class MapWithMessageTemplateDemoScreen(carContext: CarContext) :
    Screen(carContext), CarContextAware {

    private val routingDemoModelFactory = RoutingDemoModelFactory(carContext)
    private val icMessage = getCarIcon(R.drawable.ic_commute_24px, makeTintedIconStyle())

    @RequiresCarApi(7)
    override fun onGetTemplate(): Template {
        return MapWithContentTemplate.Builder()
            .setContentTemplate(makeMessageTemplate())
            .setMapController(makeMapController())
            .setActionStrip(makeActionStrip())
            .build()
    }

    private fun makeMessageTemplate(): MessageTemplate {
        return MessageTemplate.Builder("Continue to Google Kirkland Urban WA 98101?")
            .setHeader(makeHeader())
            .setIcon(icMessage)
            .addAction(
                createAction(title = "Navigate") {
                    makeToast("Let's start navigation").show()
                }
            )
            .addAction(
                createAction(title = "Cancel") {
                    makeToast("Quitting navigation").show()
                }
            )
            .build()
    }

    private fun makeHeader(): Header {
        return Header.Builder()
            .setStartHeaderAction(Action.BACK)
            .setTitle("Drive to Google Kirkland")
            .build()
    }

    private fun makeMapController(): MapController {
        return MapController.Builder()
            .setMapActionStrip(routingDemoModelFactory.mapActionStrip)
            .build()
    }

    private fun makeActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                createAction(
                    title = getString(R.string.search_action_title),
                    clickListener = {},
                )
            )
            .build()
    }

    private fun makeTintedIconStyle(): CarIconStyle {
        val typedArray: TypedArray =
            carContext.obtainStyledAttributes(R.style.CarAppTheme, R.styleable.ShowcaseTheme)

        val iconTintColor: CarColor =
            CarColor.createCustom(
                typedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColor, -1),
                typedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColorDark, -1),
            )

        typedArray.recycle()

        return CarIconStyle.Builder(CarIconStyle.TINTED).setTint(iconTintColor).build()
    }
}
