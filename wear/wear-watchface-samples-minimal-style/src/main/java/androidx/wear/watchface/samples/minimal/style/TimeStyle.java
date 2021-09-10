/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.samples.minimal.style;

import android.content.Context;
import android.graphics.drawable.Icon;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.wear.watchface.style.MutableUserStyle;
import androidx.wear.watchface.style.UserStyle;
import androidx.wear.watchface.style.UserStyleSetting;
import androidx.wear.watchface.style.WatchFaceLayer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

final class TimeStyle {

    public enum Value {
        MINIMAL,
        SECONDS,
    }

    private static final UserStyleSetting.Id ID = new UserStyleSetting.Id("TimeStyle");

    private static final UserStyleSetting.Option.Id MINIMAL_ID =
            new UserStyleSetting.Option.Id("minimal");
    private static final UserStyleSetting.Option.Id SECONDS_ID =
            new UserStyleSetting.Option.Id("seconds");

    private static final EnumMap<Value, UserStyleSetting.Option.Id> VALUE_ID_MAP =
            createOptionIdMap();

    private static final Map<UserStyleSetting.Option.Id, Value> ID_VALUE_MAP =
            createIdOptionMap();

    private final UserStyleSetting mSetting;

    TimeStyle(Context context) {
        mSetting = create(context);
    }

    public UserStyleSetting get() {
        return mSetting;
    }

    public Value get(UserStyle userStyle) {
        UserStyleSetting.Option current = userStyle.get(mSetting);
        return current == null ? Value.MINIMAL : ID_VALUE_MAP.get(current.getId());
    }

    public UserStyle set(UserStyle userStyle, Value value) {
        MutableUserStyle mutableUserStyle = userStyle.toMutableUserStyle();
        mutableUserStyle.set(mSetting, getOptionForValue(value));
        return mutableUserStyle.toUserStyle();
    }

    public CharSequence getDisplayName(Value value) {
        return getOptionForValue(value).getDisplayName();
    }

    private UserStyleSetting.ListUserStyleSetting.ListOption getOptionForValue(Value value) {
        return (UserStyleSetting.ListUserStyleSetting.ListOption)
                mSetting.getOptionForId(VALUE_ID_MAP.get(value));
    }

    private static EnumMap<Value, UserStyleSetting.Option.Id> createOptionIdMap() {
        EnumMap<Value, UserStyleSetting.Option.Id> map = new EnumMap<>(Value.class);
        map.put(Value.MINIMAL, MINIMAL_ID);
        map.put(Value.SECONDS, SECONDS_ID);
        return map;
    }

    private static Map<UserStyleSetting.Option.Id, Value> createIdOptionMap() {
        Map<UserStyleSetting.Option.Id, Value> map = new HashMap<>();
        map.put(MINIMAL_ID, Value.MINIMAL);
        map.put(SECONDS_ID, Value.SECONDS);
        return map;
    }

    private static UserStyleSetting create(Context context) {
        return new UserStyleSetting.ListUserStyleSetting(
                ID,
                getString(context, R.string.time_style_name),
                getString(context, R.string.time_style_description),
                getIcon(context, R.drawable.time_style_icon),
                Arrays.asList(
                        new UserStyleSetting.ListUserStyleSetting.ListOption(
                                MINIMAL_ID,
                                getString(context, R.string.time_style_minimal_name),
                                getIcon(context, R.drawable.time_style_minimal_icon)
                        ),
                        new UserStyleSetting.ListUserStyleSetting.ListOption(
                                SECONDS_ID,
                                getString(context, R.string.time_style_seconds_name),
                                getIcon(context, R.drawable.time_style_seconds_icon)
                        )
                ),
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS);
    }

    private static String getString(Context context, @StringRes int resId) {
        return context.getString(resId);
    }

    private static Icon getIcon(Context context, @DrawableRes int resId) {
        return Icon.createWithResource(context, resId);
    }
}
