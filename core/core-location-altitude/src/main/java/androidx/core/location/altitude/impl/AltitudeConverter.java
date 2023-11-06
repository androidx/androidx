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

package androidx.core.location.altitude.impl;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.location.LocationCompat;
import androidx.core.location.altitude.impl.proto.MapParamsProto;
import androidx.core.util.Preconditions;

import java.io.IOException;

/** Implements {@link androidx.core.location.altitude.AltitudeConverterCompat}. */
public final class AltitudeConverter {

    private static final double MAX_ABS_VALID_LATITUDE = 90;
    private static final double MAX_ABS_VALID_LONGITUDE = 180;

    /** Manages a mapping of geoid heights associated with S2 cells. */
    private final GeoidHeightMap mGeoidHeightMap = new GeoidHeightMap();

    /**
     * Creates an instance that manages an independent cache to optimized conversions of locations
     * in proximity to one another.
     */
    public AltitudeConverter() {
    }

    /**
     * Throws an {@link IllegalArgumentException} if the {@code location} has an invalid latitude,
     * longitude, or altitude above WGS84.
     */
    private static void validate(@NonNull Location location) {
        Preconditions.checkArgument(
                isFiniteAndAtAbsMost(location.getLatitude(), MAX_ABS_VALID_LATITUDE),
                "Invalid latitude: %f", location.getLatitude());
        Preconditions.checkArgument(
                isFiniteAndAtAbsMost(location.getLongitude(), MAX_ABS_VALID_LONGITUDE),
                "Invalid longitude: %f", location.getLongitude());
        Preconditions.checkArgument(location.hasAltitude(), "Missing altitude above WGS84");
        Preconditions.checkArgument(Double.isFinite(location.getAltitude()),
                "Invalid altitude above WGS84: %f", location.getAltitude());
    }

    private static boolean isFiniteAndAtAbsMost(double value, double rhs) {
        return isFinite(value) && Math.abs(value) <= rhs;
    }

    private static boolean isFinite(double value) {
        return !Double.isInfinite(value) && !Double.isNaN(value);
    }

    /**
     * Returns the four S2 cell IDs for the map square associated with the {@code location}.
     *
     * <p>The first map cell, denoted z11 in the appendix of the referenced paper below, contains
     * the location. The others are the map cells denoted z21, z12, and z22, in that order.
     *
     * <p>Reference:
     *
     * <pre>
     * Brian Julian and Michael Angermann.
     * "Resource efficient and accurate altitude conversion to Mean Sea Level."
     * 2023 IEEE/ION Position, Location and Navigation Symposium (PLANS).
     * </pre>
     */
    @NonNull
    private static long[] findMapSquare(@NonNull MapParamsProto params,
            @NonNull Location location) {
        long s2CellId =
                S2CellIdUtils.fromLatLngDegrees(location.getLatitude(), location.getLongitude());

        // Cell-space properties and coordinates.
        int sizeIj = 1 << (S2CellIdUtils.MAX_LEVEL - params.getMapS2Level());
        int maxIj = 1 << S2CellIdUtils.MAX_LEVEL;
        long z11 = S2CellIdUtils.getParent(s2CellId, params.getMapS2Level());
        int f11 = S2CellIdUtils.getFace(s2CellId);
        int i1 = S2CellIdUtils.getI(s2CellId);
        int j1 = S2CellIdUtils.getJ(s2CellId);
        int i2 = i1 + sizeIj;
        int j2 = j1 + sizeIj;

        // Non-boundary region calculation - simplest and most common case.
        if (i2 < maxIj && j2 < maxIj) {
            return new long[]{
                    z11,
                    S2CellIdUtils.getParent(S2CellIdUtils.fromFij(f11, i2, j1),
                            params.getMapS2Level()),
                    S2CellIdUtils.getParent(S2CellIdUtils.fromFij(f11, i1, j2),
                            params.getMapS2Level()),
                    S2CellIdUtils.getParent(S2CellIdUtils.fromFij(f11, i2, j2),
                            params.getMapS2Level())
            };
        }

        // Boundary region calculation
        long[] edgeNeighbors = new long[4];
        S2CellIdUtils.getEdgeNeighbors(z11, edgeNeighbors);
        long z11W = edgeNeighbors[0];
        long z11S = edgeNeighbors[1];
        long z11E = edgeNeighbors[2];
        long z11N = edgeNeighbors[3];

        long[] otherEdgeNeighbors = new long[4];
        S2CellIdUtils.getEdgeNeighbors(z11W, otherEdgeNeighbors);
        S2CellIdUtils.getEdgeNeighbors(z11S, edgeNeighbors);
        long z11SW = findCommonNeighbor(edgeNeighbors, otherEdgeNeighbors, z11);
        S2CellIdUtils.getEdgeNeighbors(z11E, otherEdgeNeighbors);
        long z11SE = findCommonNeighbor(edgeNeighbors, otherEdgeNeighbors, z11);
        S2CellIdUtils.getEdgeNeighbors(z11N, edgeNeighbors);
        long z11NE = findCommonNeighbor(edgeNeighbors, otherEdgeNeighbors, z11);

        long z21 = (f11 % 2 == 1 && i2 >= maxIj) ? z11SW : z11S;
        long z12 = (f11 % 2 == 0 && j2 >= maxIj) ? z11NE : z11E;
        long z22 = (z21 == z11SW) ? z11S : (z12 == z11NE) ? z11E : z11SE;

        // Reuse edge neighbors' array to avoid an extra allocation.
        edgeNeighbors[0] = z11;
        edgeNeighbors[1] = z21;
        edgeNeighbors[2] = z12;
        edgeNeighbors[3] = z22;
        return edgeNeighbors;
    }

    /**
     * Returns the first common non-z11 neighbor found between the two arrays of edge neighbors. If
     * such a common neighbor does not exist, returns z11.
     */
    private static long findCommonNeighbor(
            @NonNull long[] edgeNeighbors, @NonNull long[] otherEdgeNeighbors, long z11) {
        for (long edgeNeighbor : edgeNeighbors) {
            if (edgeNeighbor == z11) {
                continue;
            }
            for (long otherEdgeNeighbor : otherEdgeNeighbors) {
                if (edgeNeighbor == otherEdgeNeighbor) {
                    return edgeNeighbor;
                }
            }
        }
        return z11;
    }

    /**
     * Adds to {@code location} the bilinearly interpolated Mean Sea Level altitude. In addition, a
     * Mean Sea Level altitude accuracy is added if the {@code location} has a valid vertical
     * accuracy; otherwise, does not add a corresponding accuracy.
     */
    private static void addMslAltitude(@NonNull MapParamsProto params,
            @NonNull double[] geoidHeightsMeters, @NonNull Location location) {
        double h0 = geoidHeightsMeters[0];
        double h1 = geoidHeightsMeters[1];
        double h2 = geoidHeightsMeters[2];
        double h3 = geoidHeightsMeters[3];

        // Bilinear interpolation on an S2 square of size equal to that of a map cell. wi and wj
        // are the normalized [0,1] weights in the i and j directions, respectively, allowing us to
        // employ the simplified unit square formulation.
        long s2CellId = S2CellIdUtils.fromLatLngDegrees(location.getLatitude(),
                location.getLongitude());
        double sizeIj = 1 << (S2CellIdUtils.MAX_LEVEL - params.getMapS2Level());
        double wi = (S2CellIdUtils.getI(s2CellId) % sizeIj) / sizeIj;
        double wj = (S2CellIdUtils.getJ(s2CellId) % sizeIj) / sizeIj;
        double offsetMeters = h0 + (h1 - h0) * wi + (h2 - h0) * wj + (h3 - h1 - h2 + h0) * wi * wj;

        LocationCompat.setMslAltitudeMeters(location, location.getAltitude() - offsetMeters);
        if (LocationCompat.hasVerticalAccuracy(location)) {
            double verticalAccuracyMeters = LocationCompat.getVerticalAccuracyMeters(location);
            if (isFinite(verticalAccuracyMeters) && verticalAccuracyMeters >= 0) {
                LocationCompat.setMslAltitudeAccuracyMeters(location,
                        (float) Math.hypot(verticalAccuracyMeters, params.getModelRmseMeters()));
            }
        }
    }

    /**
     * Implements
     * {@link androidx.core.location.altitude.AltitudeConverterCompat#addMslAltitudeToLocation(Context, Location)}.
     */
    public void addMslAltitudeToLocation(@NonNull Context context, @NonNull Location location)
            throws IOException {
        validate(location);
        MapParamsProto params = GeoidHeightMap.getParams(context);
        long[] s2CellIds = findMapSquare(params, location);
        double[] geoidHeightsMeters = mGeoidHeightMap.readGeoidHeights(params, context, s2CellIds);
        addMslAltitude(params, geoidHeightsMeters, location);
    }
}
