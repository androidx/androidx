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

package androidx.car.app.sample.showcase.common.screens.navigationdemos;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Distance;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.navigation.model.RoutingInfo;
import androidx.lifecycle.DefaultLifecycleObserver;

import org.jspecify.annotations.NonNull;

/** A screen that shows the navigation template in routing state. */
public final class NavigatingDemoScreen extends Screen implements DefaultLifecycleObserver {
    private final RoutingDemoModelFactory mRoutingDemoModelFactory;
    public NavigatingDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mRoutingDemoModelFactory = new RoutingDemoModelFactory(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        return new NavigationTemplate.Builder()
                .setNavigationInfo(
                        new RoutingInfo.Builder()
                                .setCurrentStep(
                                        mRoutingDemoModelFactory.getCurrentStep(),
                                        Distance.create(200, Distance.UNIT_METERS))
                                .setNextStep(mRoutingDemoModelFactory.getNextStep())
                                .build())
                .setDestinationTravelEstimate(mRoutingDemoModelFactory.getTravelEstimate())
                .setActionStrip(mRoutingDemoModelFactory.getActionStrip(this::finish))
                .setMapActionStrip(mRoutingDemoModelFactory.getMapActionStrip())
                .setBackgroundColor(CarColor.SECONDARY)
                .build();
    }
}
