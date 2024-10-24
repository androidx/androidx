/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.common;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.DrawableRes;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarIconSpan;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Distance;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Provides sample place data used in the demos. */
public class SamplePlaces {
    /** The location to use as an anchor for calculating distances. */
    private final Location mAnchorLocation;

    private final List<PlaceInfo> mPlaces;
    private final Screen mDemoScreen;

    private SamplePlaces(Screen demoScreen) {
        mDemoScreen = demoScreen;

        CarContext carContext = demoScreen.getCarContext();

        mAnchorLocation = new Location("ShowcaseDemo");
        mAnchorLocation.setLatitude(47.6204588);
        mAnchorLocation.setLongitude(-122.1918818);

        mPlaces = getSamplePlaces(carContext);
    }

    /** Create an instance of {@link SamplePlaces}. */
    public static @NonNull SamplePlaces create(@NonNull Screen demoScreen) {
        return new SamplePlaces(demoScreen);
    }

    /**
     * Returns the list of sample places.
     *
     * <p>We use a few Google locations around the Seattle area, using different types of markers to
     * showcase those options. The "description" field of each place describes the type of marker
     * itself.
     */
    private static List<PlaceInfo> getSamplePlaces(@NonNull CarContext carContext) {
        List<PlaceInfo> places = new ArrayList<>();

        TypedArray typedArray =
                carContext.obtainStyledAttributes(R.style.CarAppTheme, R.styleable.ShowcaseTheme);
        CarColor iconTintColor =
                CarColor.createCustom(
                        typedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColor, -1),
                        typedArray.getColor(R.styleable.ShowcaseTheme_markerIconTintColorDark, -1));


        Location location1 = new Location(SamplePlaces.class.getSimpleName());
        location1.setLatitude(47.6696482);
        location1.setLongitude(-122.19950278);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_1_title),
                        carContext.getString(R.string.location_1_address),
                        carContext.getString(R.string.location_1_description),
                        carContext.getString(R.string.location_1_phone),
                        location1,
                        new PlaceMarker.Builder()
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        carContext,
                                                        R.drawable.ic_commute_24px))
                                                .setTint(iconTintColor)
                                                .build(),
                                        PlaceMarker.TYPE_ICON)
                                .build()));

        Location location2 = new Location(SamplePlaces.class.getSimpleName());
        location2.setLatitude(47.6204588);
        location2.setLongitude(-122.1918818);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_2_title),
                        carContext.getString(R.string.location_2_address),
                        carContext.getString(R.string.location_2_description),
                        carContext.getString(R.string.location_2_phone),
                        location2,
                        new PlaceMarker.Builder()
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        carContext, R.drawable.ic_520))
                                                .build(),
                                        PlaceMarker.TYPE_IMAGE)
                                .build()));

        Location location3 = new Location(SamplePlaces.class.getSimpleName());
        location3.setLatitude(47.625567);
        location3.setLongitude(-122.336427);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_3_title),
                        carContext.getString(R.string.location_3_address),
                        carContext.getString(R.string.location_3_description),
                        carContext.getString(R.string.location_3_phone),
                        location3,
                        new PlaceMarker.Builder().setLabel("SLU").setColor(CarColor.RED).build()));

        Location location4 = new Location(SamplePlaces.class.getSimpleName());
        location4.setLatitude(47.6490374);
        location4.setLongitude(-122.3527127);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_4_title),
                        carContext.getString(R.string.location_4_address),
                        carContext.getString(R.string.location_4_description),
                        carContext.getString(R.string.location_4_phone),
                        location4,
                        new PlaceMarker.Builder()
                                .setIcon(
                                        createCarIconWithBitmap(carContext, R.drawable.banana),
                                        PlaceMarker.TYPE_IMAGE
                                )
                                .build()));

        Location location5 = new Location(SamplePlaces.class.getSimpleName());
        location5.setLatitude(37.422014);
        location5.setLongitude(-122.084776);
        SpannableString title5 = new SpannableString("  Googleplex");
        title5.setSpan(
                CarIconSpan.create(
                        createCarIconWithBitmap(carContext, R.drawable.ic_hi),
                        CarIconSpan.ALIGN_CENTER
                ),
                0,
                1,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        SpannableString description5 = new SpannableString(" ");
        places.add(
                new PlaceInfo(
                        title5,
                        carContext.getString(R.string.location_5_address),
                        description5,
                        carContext.getString(R.string.location_5_phone),
                        location5,
                        new PlaceMarker.Builder()
                                .setIcon(
                                        createCarIconWithBitmap(
                                                carContext,
                                                R.drawable.test_image_square
                                        ),
                                        PlaceMarker.TYPE_IMAGE)
                                .build()));

        Location location6 = new Location(SamplePlaces.class.getSimpleName());
        location6.setLatitude(47.6490374);
        location6.setLongitude(-122.3527127);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_6_title),
                        carContext.getString(R.string.location_6_address),
                        carContext.getString(R.string.location_description_text_label),
                        carContext.getString(R.string.location_phone_not_available),
                        location6,
                        new PlaceMarker.Builder().build()));

        // Some hosts may display more items in the list than others, so create 3 more items.
        Location location7 = new Location(SamplePlaces.class.getSimpleName());
        location7.setLatitude(47.5496056);
        location7.setLongitude(-122.2571713);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_7_title),
                        carContext.getString(R.string.location_7_address),
                        carContext.getString(R.string.location_description_text_label),
                        carContext.getString(R.string.location_phone_not_available),
                        location7,
                        new PlaceMarker.Builder().build()));

        Location location8 = new Location(SamplePlaces.class.getSimpleName());
        location8.setLatitude(47.5911456);
        location8.setLongitude(-122.2256602);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_8_title),
                        carContext.getString(R.string.location_8_address),
                        carContext.getString(R.string.location_description_text_label),
                        carContext.getString(R.string.location_phone_not_available),
                        location8,
                        new PlaceMarker.Builder().build()));

        Location location9 = new Location(SamplePlaces.class.getSimpleName());
        location9.setLatitude(47.6785932);
        location9.setLongitude(-122.2113821);
        places.add(
                new PlaceInfo(
                        carContext.getString(R.string.location_9_title),
                        carContext.getString(R.string.location_9_address),
                        carContext.getString(R.string.location_description_text_label),
                        carContext.getString(R.string.location_phone_not_available),
                        location9,
                        new PlaceMarker.Builder().build()));

        return places;
    }

    /** Return the {@link ItemList} of the sample places. */
    public @NonNull ItemList getPlaceList(boolean randomOrder) {
        ItemList.Builder listBuilder = new ItemList.Builder();

        int listLimit = 6;
        CarContext carContext = mDemoScreen.getCarContext();
        if (carContext.getCarAppApiLevel() > CarAppApiLevels.LEVEL_1) {
            // Some hosts may allow more items in the grid than others, so put more items if
            // possible
            listLimit =
                    max(listLimit,
                            carContext.getCarService(ConstraintManager.class).getContentLimit(
                                    ConstraintManager.CONTENT_LIMIT_TYPE_LIST));
        }
        listLimit = min(listLimit, mPlaces.size());

        for (int index = 0; index < listLimit; index++) {
            PlaceInfo place = mPlaces.get(randomOrder ? new Random().nextInt(listLimit) : index);

            // Build a description string that includes the required distance span.
            int distanceKm = getDistanceFromCurrentLocation(place.location) / 1000;
            SpannableStringBuilder descriptionBuilder = new SpannableStringBuilder();

            descriptionBuilder.append(
                    " ",
                    DistanceSpan.create(Distance.create(distanceKm, Distance.UNIT_KILOMETERS)),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            descriptionBuilder.setSpan(
                    ForegroundCarColorSpan.create(CarColor.BLUE),
                    0,
                    1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            descriptionBuilder.append(" · ");
            descriptionBuilder.append(place.description);
            if (index == 4) {
                descriptionBuilder.append(" ",
                        CarIconSpan.create(createCarIconWithBitmap(carContext, R.drawable.ic_hi)),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            boolean isBrowsable = index > mPlaces.size() / 2;

            // Add the row for this place to the list.
            listBuilder.addItem(
                    new Row.Builder()
                            .setTitle(place.title)
                            .addText(descriptionBuilder)
                            .setOnClickListener(() -> onClickPlace(place))
                            .setBrowsable(isBrowsable)
                            .setMetadata(
                                    new Metadata.Builder()
                                            .setPlace(
                                                    new Place.Builder(
                                                            CarLocation.create(
                                                                    place.location))
                                                            .setMarker(place.marker)
                                                            .build())
                                            .build())
                            .build());
        }

        return listBuilder.build();
    }

    /** Returns the distance in meters of the {@code location} from the current location. */
    private int getDistanceFromCurrentLocation(Location location) {
        return (int) mAnchorLocation.distanceTo(location);
    }

    private void onClickPlace(PlaceInfo place) {
        mDemoScreen
                .getScreenManager()
                .push(PlaceDetailsScreen.create(mDemoScreen.getCarContext(), place));
    }

    private static CarIcon createCarIconWithBitmap(CarContext carContext,
            @DrawableRes int drawable) {
        return new CarIcon.Builder(
                IconCompat.createWithBitmap(
                        BitmapFactory.decodeResource(carContext.getResources(), drawable)
                )
        ).build();
    }
}
