/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.testing;

import android.graphics.ImageFormat;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import androidx.camera.core.ImageFormatConstants;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Utility functions to obtain fake {@link StreamConfigurationMap} for testing */
public final class StreamConfigurationMapUtil {
    /**
     * Generates fake StreamConfigurationMap for testing usage.
     *
     * @return a fake {@link StreamConfigurationMap} object
     */
    public static StreamConfigurationMap generateFakeStreamConfigurationMap() {
        /**
         * Defined in StreamConfigurationMap.java: 0x21 is internal defined legal format
         * corresponding to ImageFormat.JPEG. 0x22 is internal defined legal format
         * IMPLEMENTATION_DEFINED and at least one stream configuration for
         * IMPLEMENTATION_DEFINED(0x22) must exist, otherwise, there will be AssertionError threw.
         * 0x22 is also mapped to ImageFormat.PRIVATE after Android level 23.
         */
        int[] supportedFormats =
                new int[]{
                        ImageFormat.YUV_420_888,
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_JPEG,
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                };
        Size[] supportedSizes =
                new Size[]{
                        new Size(4032, 3024),
                        new Size(3840, 2160),
                        new Size(1920, 1080),
                        new Size(640, 480),
                        new Size(320, 240),
                        new Size(320, 180)
                };

        return generateFakeStreamConfigurationMap(supportedFormats, supportedSizes);
    }

    /**
     * Generates fake StreamConfigurationMap for testing usage.
     *
     * @param supportedFormats The supported {@link ImageFormat} list to be added
     * @param supportedSizes   The supported sizes to be added
     * @return a fake {@link StreamConfigurationMap} object
     */
    public static StreamConfigurationMap generateFakeStreamConfigurationMap(
            int[] supportedFormats, Size[] supportedSizes) {
        StreamConfigurationMap map;

        // TODO(b/123938482): Remove usage of reflection in this class
        Class<?> streamConfigurationClass;
        Class<?> streamConfigurationDurationClass;
        Class<?> highSpeedVideoConfigurationClass;
        Class<?> reprocessFormatsMapClass;

        try {
            streamConfigurationClass =
                    Class.forName("android.hardware.camera2.params.StreamConfiguration");
            streamConfigurationDurationClass =
                    Class.forName("android.hardware.camera2.params.StreamConfigurationDuration");
            highSpeedVideoConfigurationClass =
                    Class.forName("android.hardware.camera2.params.HighSpeedVideoConfiguration");
            reprocessFormatsMapClass =
                    Class.forName("android.hardware.camera2.params.ReprocessFormatsMap");
        } catch (ClassNotFoundException e) {
            throw new AssertionError(
                    "Class can not be found when trying to generate a StreamConfigurationMap "
                            + "object.",
                    e);
        }

        Constructor<?> streamConfigurationMapConstructor;
        Constructor<?> streamConfigurationConstructor;
        Constructor<?> streamConfigurationDurationConstructor;

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                streamConfigurationMapConstructor =
                        StreamConfigurationMap.class.getDeclaredConstructor(
                                Array.newInstance(streamConfigurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(highSpeedVideoConfigurationClass, 1).getClass(),
                                reprocessFormatsMapClass,
                                boolean.class);
            } else {
                streamConfigurationMapConstructor =
                        StreamConfigurationMap.class.getDeclaredConstructor(
                                Array.newInstance(streamConfigurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(streamConfigurationDurationClass, 1).getClass(),
                                Array.newInstance(highSpeedVideoConfigurationClass, 1).getClass());
            }

            streamConfigurationConstructor =
                    streamConfigurationClass.getDeclaredConstructor(
                            int.class, int.class, int.class, boolean.class);

            streamConfigurationDurationConstructor =
                    streamConfigurationDurationClass.getDeclaredConstructor(
                            int.class, int.class, int.class, long.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "Constructor can not be found when trying to generate a "
                            + "StreamConfigurationMap object.",
                    e);
        }

        Object configurationArray =
                Array.newInstance(
                        streamConfigurationClass, supportedFormats.length * supportedSizes.length);
        Object minFrameDurationArray = Array.newInstance(streamConfigurationDurationClass, 1);
        Object stallDurationArray = Array.newInstance(streamConfigurationDurationClass, 1);
        Object depthConfigurationArray = Array.newInstance(streamConfigurationClass, 1);
        Object depthMinFrameDurationArray = Array.newInstance(streamConfigurationDurationClass, 1);
        Object depthStallDurationArray = Array.newInstance(streamConfigurationDurationClass, 1);

        try {
            for (int i = 0; i < supportedFormats.length; i++) {
                for (int j = 0; j < supportedSizes.length; j++) {
                    Array.set(
                            configurationArray,
                            i * supportedSizes.length + j,
                            streamConfigurationConstructor.newInstance(
                                    supportedFormats[i],
                                    supportedSizes[j].getWidth(),
                                    supportedSizes[j].getHeight(),
                                    false));
                }
            }

            Array.set(
                    minFrameDurationArray,
                    0,
                    streamConfigurationDurationConstructor.newInstance(
                            ImageFormat.YUV_420_888, 1920, 1080, 0));

            Array.set(
                    stallDurationArray,
                    0,
                    streamConfigurationDurationConstructor.newInstance(
                            ImageFormat.YUV_420_888, 1920, 1080, 0));

            // Need depth configuration to create the object successfully
            // 0x24 is internal format type of HAL_PIXEL_FORMAT_RAW_OPAQUE
            Array.set(
                    depthConfigurationArray,
                    0,
                    streamConfigurationConstructor.newInstance(0x24, 1920, 1080, false));

            Array.set(
                    depthMinFrameDurationArray,
                    0,
                    streamConfigurationDurationConstructor.newInstance(0x24, 1920, 1080, 0));

            Array.set(
                    depthStallDurationArray,
                    0,
                    streamConfigurationDurationConstructor.newInstance(0x24, 1920, 1080, 0));

            if (Build.VERSION.SDK_INT >= 23) {
                map =
                        (StreamConfigurationMap)
                                streamConfigurationMapConstructor.newInstance(
                                        configurationArray,
                                        minFrameDurationArray,
                                        stallDurationArray,
                                        depthConfigurationArray,
                                        depthMinFrameDurationArray,
                                        depthStallDurationArray,
                                        null,
                                        null,
                                        false);
            } else {
                map =
                        (StreamConfigurationMap)
                                streamConfigurationMapConstructor.newInstance(
                                        configurationArray,
                                        minFrameDurationArray,
                                        stallDurationArray,
                                        null);
            }

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new AssertionError(
                    "Failed to create new instance when trying to generate a "
                            + "StreamConfigurationMap object.",
                    e);
        }

        return map;
    }
}
