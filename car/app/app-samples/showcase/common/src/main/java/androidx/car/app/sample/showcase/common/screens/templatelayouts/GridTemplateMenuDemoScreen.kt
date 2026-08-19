/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action.BACK
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.screens.templatelayouts.gridtemplates.GridTemplateDemoScreen
import androidx.car.app.sample.showcase.common.screens.templatelayouts.gridtemplates.NotificationDemoScreen
import androidx.car.app.sample.showcase.common.utils.CarContextAware
import androidx.car.app.sample.showcase.common.utils.createRowAndPushScreen

/**
 * Creates a screen that demonstrates usage of the full screen [ListTemplate] to display a
 * full-screen list.
 */
class GridTemplateMenuDemoScreen(carContext: CarContext) : Screen(carContext), CarContextAware {

    private val demoScreens: List<Pair<Int, (CarContext) -> Screen>> =
        listOf(
            R.string.grid_template_demo_title to ::GridTemplateDemoScreen,
            R.string.notification_template_demo_title to ::NotificationDemoScreen,
        )

    override fun onGetTemplate(): Template {
        val list =
            ItemList.Builder()
                .apply {
                    demoScreens.forEach { (titleRes, screenFactory) ->
                        addItem(createRowAndPushScreen(titleRes, screenFactory))
                    }
                }
                .build()

        val pageHeader =
            Header.Builder()
                .setTitle(getString(R.string.grid_template_menu_demo_title))
                .setStartHeaderAction(BACK)
                .build()

        return ListTemplate.Builder().setSingleList(list).setHeader(pageHeader).build()
    }
}
