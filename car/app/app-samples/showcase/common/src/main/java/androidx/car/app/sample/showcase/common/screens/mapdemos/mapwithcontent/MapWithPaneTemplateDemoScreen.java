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

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.CarToast.LENGTH_SHORT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Simple demo of how to present a map template with a pane. */
public class MapWithPaneTemplateDemoScreen extends Screen {
    private final @Nullable IconCompat mPaneImage;

    private final @Nullable IconCompat mRowLargeIcon;

    private static final int LIST_LIMIT = 4;

    private boolean mIsFavorite;
    private final RoutingDemoModelFactory mRoutingDemoModelFactory;

    public MapWithPaneTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.patio);
        mPaneImage = IconCompat.createWithBitmap(bitmap);
        mRowLargeIcon = IconCompat.createWithResource(getCarContext(),
                R.drawable.ic_fastfood_white_48dp);
        mRoutingDemoModelFactory = new RoutingDemoModelFactory(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();

        paneBuilder.addRow(createRowWithExcessivelyLargeContent());
        paneBuilder.addRow(createRowWithSecondaryAction(1));
        for (int i = 2; i < LIST_LIMIT; i++) {
            paneBuilder.addRow(createRow(i));
        }

        // Also set a large image outside of the rows.
        paneBuilder.setImage(new CarIcon.Builder(mPaneImage).build());

        Action.Builder primaryActionBuilder = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.primary_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(
                        () -> CarToast.makeText(
                                        getCarContext(),
                                        getCarContext().getString(R.string.primary_toast_msg),
                                        LENGTH_SHORT)
                                .show());
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        paneBuilder
                .addAction(primaryActionBuilder.build())
                .addAction(
                        new Action.Builder()
                                .setTitle(getCarContext().getString(R.string.options_action_title))
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.options_toast_msg),
                                                        LENGTH_SHORT)
                                                .show())
                                .build());

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
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
                            mIsFavorite = !mIsFavorite;
                            CarToast.makeText(
                                            getCarContext(),
                                            mIsFavorite
                                                    ? getCarContext()
                                                    .getString(R.string.favorite_toast_msg)
                                                    : getCarContext().getString(
                                                            R.string.not_favorite_toast_msg),
                                            LENGTH_SHORT)
                                    .show();
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
                .setTitle(getCarContext().getString(R.string.map_template_pane_demo_title))
                .build();


        MapController mapController = new MapController.Builder()
                .setMapActionStrip(mRoutingDemoModelFactory.getMapActionStrip())
                .build();

        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.bug_reported_toast_msg),
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

        MapWithContentTemplate.Builder builder = new MapWithContentTemplate.Builder()
                .setActionStrip(actionStrip)
                .setContentTemplate(new PaneTemplate.Builder(paneBuilder.build())
                        .setHeader(header)
                        .build())
                .setMapController(mapController);

        return builder.build();
    }

    private Row createRow(int index) {
        return new Row.Builder()
                .setTitle(createRowTitle(index))
                .addText(getCarContext().getString(R.string.other_row_text))
                .addText(getCarContext().getString(R.string.other_row_text))
                .build();
    }

    private Row createRowWithExcessivelyLargeContent() {
        return new Row.Builder()
            .setTitle(getCarContext().getString(R.string.first_row_title))
            .addText(getCarContext().getString(R.string.long_line_text))
            .setImage(new CarIcon.Builder(mRowLargeIcon).build())
            .build();
    }

    private Row createRowWithSecondaryAction(int index) {
        Action action = new Action.Builder()
                .setIcon(buildCarIconWithResources(R.drawable.baseline_question_mark_24))
                .setOnClickListener(() -> CarToast.makeText(getCarContext(),
                        R.string.secondary_action_toast, LENGTH_LONG).show())
                .build();

        Row.Builder rowBuilder = new Row.Builder()
                .setTitle(createRowTitle(index))
                .addText(getCarContext().getString(R.string.other_row_text));

        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_6) {
            rowBuilder.addAction(action);
        }

        return rowBuilder.build();
    }

    private CharSequence createRowTitle(int index) {
        return getCarContext().getString(R.string.other_row_title_prefix) + (index + 1);
    }

    private CarIcon buildCarIconWithResources(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }
}
