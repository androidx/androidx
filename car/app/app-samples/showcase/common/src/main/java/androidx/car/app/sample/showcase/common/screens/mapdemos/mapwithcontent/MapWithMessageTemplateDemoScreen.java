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

package androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent;

import android.content.res.TypedArray;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** Simple demo of how to present a map template with a list. */
public class MapWithMessageTemplateDemoScreen extends Screen {
    TypedArray mTypedArray =
            getCarContext().obtainStyledAttributes(R.style.CarAppTheme, R.styleable.ShowcaseTheme);
    CarColor mIconTintColor =
            CarColor.createCustom(
                    mTypedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColor, -1),
                    mTypedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColorDark, -1));
    private final RoutingDemoModelFactory mRoutingDemoModelFactory;
    public MapWithMessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mRoutingDemoModelFactory = new RoutingDemoModelFactory(carContext);
    }

    @ExperimentalCarApi
    @RequiresCarApi(7)
    @Override
    public @NonNull Template onGetTemplate() {

        MessageTemplate messageTemplate = new MessageTemplate.Builder("Continue to Google "
                + "Kirkland Urban WA 98101?")
                .setHeader(new Header.Builder().setStartHeaderAction(Action.BACK)
                        .setTitle("Drive to Google Kirkland")
                        .build())
                .setIcon(new CarIcon.Builder(
                        IconCompat.createWithResource(
                                getCarContext(),
                                R.drawable.ic_commute_24px))
                        .setTint(mIconTintColor)
                        .build())
                .addAction(new Action.Builder()
                        .setOnClickListener(() -> {
                            CarToast.makeText(
                                    getCarContext(),
                                    "Let's start navigation",
                                    CarToast.LENGTH_SHORT
                            ).show();
                        })
                        .setTitle("Navigate").build())
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(() -> {
                                    CarToast.makeText(
                                            getCarContext(),
                                            "Quitting navigation",
                                            CarToast.LENGTH_SHORT
                                    ).show();
                                })
                                .setTitle("Cancel").build())

                .build();


        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mRoutingDemoModelFactory.getMapActionStrip())
                .build();
        MapWithContentTemplate.Builder builder = new MapWithContentTemplate.Builder()
                .setContentTemplate(messageTemplate)
                .setMapController(mapController)
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle(getCarContext().getString(
                                                        R.string.search_action_title))
                                                .setOnClickListener(() -> {
                                                })
                                                .build())
                                .build());

        return builder.build();
    }
}
