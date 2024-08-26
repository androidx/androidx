/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.models;

import android.annotation.SuppressLint;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ext.SdkExtensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Represents the content associated with the destination where a goto link is directing.
 * Should be a nested class of {@link GotoLink}, but AIDL prevents that.
 */
// TODO: Use android.graphics.pdf.content.PdfPageGotoLinkContent#Destination and remove this class
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class GotoLinkDestination implements Parcelable {

    public static final Creator<GotoLinkDestination> CREATOR =
            new Creator<GotoLinkDestination>() {
                @Override
                public GotoLinkDestination createFromParcel(Parcel parcel) {
                    return new GotoLinkDestination(parcel.readInt(), parcel.readFloat(),
                            parcel.readFloat(), parcel.readFloat());
                }

                @Override
                public GotoLinkDestination[] newArray(int size) {
                    return new GotoLinkDestination[size];
                }
            };

    private final int mPageNumber;
    private Float mXCoordinate = null;
    private Float mYCoordinate = null;
    private final float mZoom;

    /**
     * Creates a new instance of {@link GotoLinkDestination} using the page number, x coordinate,
     * and y coordinate of the destination where goto link is directing, and the zoom factor of the
     * page when goto link takes to the destination.
     *
     * @param pageNumber  Page number of the goto link Destination
     * @param xCoordinate X coordinate of the goto link Destination in points (1/72")
     * @param yCoordinate Y coordinate of the goto link Destination in points (1/72")
     * @param zoom        Zoom factor {@link GotoLinkDestination#getZoom()} of the page when
     *                    goto link
     *                    takes to the destination
     * @throws IllegalArgumentException If pageNumber or either of the coordinates or zoom are
     *                                  less than zero
     */
    public GotoLinkDestination(int pageNumber, float xCoordinate, float yCoordinate, float zoom) {
        Preconditions.checkArgument(pageNumber >= 0,
                "Page number must be" + " greater than or equal to 0");
        Preconditions.checkArgument(xCoordinate >= 0,
                "X coordinate " + "must be greater than or equal to 0");
        Preconditions.checkArgument(yCoordinate >= 0,
                "Y coordinate must " + "be greater than or equal to 0");
        Preconditions.checkArgument(zoom >= 0,
                "Zoom factor number must be " + "greater than or equal to 0");
        this.mPageNumber = pageNumber;
        this.mXCoordinate = xCoordinate;
        this.mYCoordinate = yCoordinate;
        this.mZoom = zoom;
    }

    /**
     * Gets the page number of the destination where the {@link GotoLink} is directing.
     *
     * @return page number of the destination where goto link is directing the user.
     */
    public int getPageNumber() {
        return mPageNumber;
    }

    /**
     * Gets the x coordinate of the destination where the {@link GotoLink} is directing.
     * <p><strong>Note:</strong> If underlying pdfium library can't determine the x coordinate,
     * it will be set to 0
     *
     * @return x coordinate of the Destination where the goto link is directing the user.
     */
    @Nullable
    public Float getXCoordinate() {
        return mXCoordinate;
    }

    /**
     * Gets the y coordinate of the destination where the {@link GotoLink} is directing.
     * <p><strong>Note:</strong> If underlying pdfium library can't determine the y coordinate,
     * it will be set to 0
     *
     * @return y coordinate of the Destination where the goto link is directing the user.
     */
    @Nullable
    public Float getYCoordinate() {
        return mYCoordinate;
    }

    /**
     * Gets the zoom factor of the page when the goto link takes to the destination
     * <p><strong>Note:</strong> If there is no zoom value embedded, default value of zoom
     * will be zero. Otherwise it will be less than 1.0f in case of zoom out and greater
     * than 1.0f in case of zoom in.
     *
     * @return zoom factor of the page when the goto link takes to the destination
     */
    public float getZoom() {
        return mZoom;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeInt(mPageNumber);
        parcel.writeFloat(mXCoordinate);
        parcel.writeFloat(mYCoordinate);
        parcel.writeFloat(mZoom);
    }

    /**
     * Converts android.graphics.pdf.content.PdfPageGotoLinkContent.Destination object to its
     * androidx.pdf.aidl.GotoLinkDestination representation.
     */
    @NonNull
    public static GotoLinkDestination convert(
            @NonNull PdfPageGotoLinkContent.Destination pdfPageGotoLinkContentDest) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            return new GotoLinkDestination(pdfPageGotoLinkContentDest.getPageNumber(),
                    pdfPageGotoLinkContentDest.getXCoordinate(),
                    pdfPageGotoLinkContentDest.getYCoordinate(),
                    pdfPageGotoLinkContentDest.getZoom());
        }
        throw new UnsupportedOperationException("Operation support above S");
    }
}
