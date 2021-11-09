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

package androidx.camera.core.impl.utils;

import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_DOUBLE;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_SINGLE;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_SLONG;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_SSHORT;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_ULONG;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_UNDEFINED;
import static androidx.camera.core.impl.utils.ExifAttribute.IFD_FORMAT_USHORT;

import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

/**
 * This class stores information of an EXIF tag. For more information about
 * defined EXIF tags, please read the Jeita EXIF 2.2 standard.
 *
 * This class was pulled from the {@link ExifInterface} class.
 *
 * @see ExifInterface
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ExifTag {
    public final int number;
    public final String name;
    public final int primaryFormat;
    public final int secondaryFormat;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ExifTag(String name, int number, int format) {
        this.name = name;
        this.number = number;
        this.primaryFormat = format;
        this.secondaryFormat = -1;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ExifTag(String name, int number, int primaryFormat, int secondaryFormat) {
        this.name = name;
        this.number = number;
        this.primaryFormat = primaryFormat;
        this.secondaryFormat = secondaryFormat;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isFormatCompatible(int format) {
        if (primaryFormat == IFD_FORMAT_UNDEFINED || format == IFD_FORMAT_UNDEFINED) {
            return true;
        } else if (primaryFormat == format || secondaryFormat == format) {
            return true;
        } else if ((primaryFormat == IFD_FORMAT_ULONG || secondaryFormat == IFD_FORMAT_ULONG)
                && format == IFD_FORMAT_USHORT) {
            return true;
        } else if ((primaryFormat == IFD_FORMAT_SLONG || secondaryFormat == IFD_FORMAT_SLONG)
                && format == IFD_FORMAT_SSHORT) {
            return true;
        } else return (primaryFormat == IFD_FORMAT_DOUBLE || secondaryFormat == IFD_FORMAT_DOUBLE)
                && format == IFD_FORMAT_SINGLE;
    }
}

