/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.navigation;

import static androidx.car.app.CarToast.LENGTH_SHORT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.navigation.routing.RoutingDemoModels;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

/** Simple demo of how to present a map template. */
public class MapTemplateDemoScreen extends Screen {
    @Nullable
    private final IconCompat mPaneImage;

    @Nullable
    private final IconCompat mRowLargeIcon;

    private boolean mIsFavorite = false;

    protected MapTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.patio);
        mPaneImage = IconCompat.createWithBitmap(bitmap);
        mRowLargeIcon = IconCompat.createWithResource(getCarContext(),
                R.drawable.ic_fastfood_white_48dp);
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    @NonNull
    @Override
    public Template onGetTemplate() {
        int listLimit = 4;

        Pane.Builder paneBuilder = new Pane.Builder();
        for (int i = 0; i < listLimit; i++) {
            paneBuilder.addRow(createRow(i));
        }

        // Also set a large image outside of the rows.
        paneBuilder.setImage(new CarIcon.Builder(mPaneImage).build());

        Action.Builder primaryActionBuilder = new Action.Builder()
                .setTitle("Reserve Chair")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(
                        () -> CarToast.makeText(
                                        getCarContext(),
                                        "Reserve/Primary button pressed",
                                        LENGTH_SHORT)
                                .show());
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        paneBuilder
                .addAction(primaryActionBuilder.build())
                .addAction(
                        new Action.Builder()
                                .setTitle("Options")
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        "Options button pressed",
                                                        LENGTH_SHORT)
                                                .show())
                                .build());

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.APP_ICON)
                .addEndHeaderAction(new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                mIsFavorite
                                                        ? R.drawable.ic_favorite_filled_white_24dp
                                                        : R.drawable.ic_favorite_white_24dp))
                                        .build())
                        .setOnClickListener(() -> {
                            CarToast.makeText(
                                            getCarContext(),
                                            mIsFavorite ? "Not a favorite!" : "Favorite!",
                                            LENGTH_SHORT)
                                    .show();
                            mIsFavorite = !mIsFavorite;
                            invalidate();
                        })
                        .build())
                .addEndHeaderAction(new Action.Builder()
                        .setOnClickListener(() -> finish())
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_close_white_24dp))
                                        .build())
                        .build())
                .setTitle("Map Template with Pane Demo")
                .build();


        MapController mapController = new MapController.Builder()
                .setMapActionStrip(RoutingDemoModels.getMapActionStrip(getCarContext()))
                .build();

        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        "Bug reported!",
                                                        CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        getCarContext(),
                                                        R.drawable.ic_bug_report_24px))
                                                .build())
                                .setFlags(Action.FLAG_IS_PERSISTENT)
                                .build())
                .build();

        MapTemplate.Builder builder = new MapTemplate.Builder()
                .setActionStrip(actionStrip)
                .setPane(paneBuilder.build())
                .setHeader(header)
                .setMapController(mapController);

        return builder.build();
    }

    private Row createRow(int index) {
        switch (index) {
            case 0:
                // Row with a large image.
                return new Row.Builder()
                        .setTitle("Row with a large image and long text long text long text long "
                                + "text long text")
                        .addText("Text text text")
                        .addText("Text text text")
                        .setImage(new CarIcon.Builder(mRowLargeIcon).build())
                        .build();
            default:
                return new Row.Builder()
                        .setTitle("Row title " + (index + 1))
                        .addText("R"
                                + "ow text 1")
                        .addText("Row text 2")
                        .build();

        }
    }
}
