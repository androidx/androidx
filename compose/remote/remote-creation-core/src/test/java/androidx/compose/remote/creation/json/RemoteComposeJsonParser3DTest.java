/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.creation.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.d3.MeshExpression3D;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriter3D;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.zip.CRC32;

/**
 * Tests for the JSON spelling of the 3D commands and of {@code touchExpression}.
 *
 * <p>Each case compares bytes: the parsed document must contain exactly what the matching {@link
 * RemoteComposeWriter} calls write on their own. That is the contract with the reference
 * converters (rcj and json2rc.js), which produce the same bytes, so a default, a name table entry
 * or a parse order that drifted would show up here rather than as a subtly wrong picture.
 */
public class RemoteComposeJsonParser3DTest {

    private static final float VAR1 = AnimatedFloatExpression.VAR1;
    private static final float VAR2 = AnimatedFloatExpression.VAR2;
    private static final float SIN = AnimatedFloatExpression.SIN;
    private static final float MUL = AnimatedFloatExpression.MUL;

    @Rule public TemporaryFolder mFolder = new TemporaryFolder();

    private RemoteComposeWriter mWriter;
    private RemoteComposeJsonParser mParser;

    @Before
    public void setup() {
        mWriter = newWriter();
        mParser = new RemoteComposeJsonParser(mWriter);
    }

    // ---------------------------------------------------------------------------------------------
    // State commands

    @Test
    public void clearDepthMaterialAndDepthBiasMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'clearDepth3D'},"
                                + " {'type': 'material3D', 'specular': 0.5, 'shininess': 16},"
                                + " {'type': 'depthBias3D', 'constant': 0.001, 'slope': 2}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.clearDepth3D();
                            w.setMaterial3D(0.5f, 16f);
                            w.setDepthBias3D(0.001f, 2f);
                        }));
    }

    @Test
    public void materialAndDepthBiasDefaults() throws JSONException {
        byte[] document = parse("{'type': 'material3D'}, {'type': 'depthBias3D'}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.setMaterial3D(0f, 32f);
                            w.setDepthBias3D(0f, 0f);
                        }));
    }

    @Test
    public void nestedCommandFormIsAccepted() throws JSONException {
        byte[] document = parse("{'material3D': {'specular': 0.25}}");
        assertWrites(document, op(w -> w.setMaterial3D(0.25f, 32f)));
    }

    // ---------------------------------------------------------------------------------------------
    // camera3D

    @Test
    public void cameraDefaultsToAPerspectiveLookingDownZ() throws JSONException {
        byte[] document = parse("{'type': 'camera3D'}");
        assertWrites(
                document,
                op(
                        w ->
                                w.setCamera3D(
                                        Paint3DContext.PROJECTION_PERSPECTIVE,
                                        new float[] {0.9f, 1f, 0.1f, 100f},
                                        new float[] {0, 0, 4, 0, 0, 0, 0, 1, 0})));
    }

    @Test
    public void cameraPerspectiveAndView() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'camera3D', 'fovY': 1.2, 'aspect': 0.5, 'near': 1, 'far': 50,"
                                + " 'eye': [1, 2, 3], 'center': [0, 1, 0], 'up': [0, 0, 1]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.setCamera3D(
                                        Paint3DContext.PROJECTION_PERSPECTIVE,
                                        new float[] {1.2f, 0.5f, 1f, 50f},
                                        new float[] {1, 2, 3, 0, 1, 0, 0, 0, 1})));
    }

    @Test
    public void cameraOrthoTakesItsBounds() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'camera3D', 'projection': 'Orthographic', 'left': -1,"
                                + " 'right': 1, 'bottom': -2, 'top': 2}");
        assertWrites(
                document,
                op(
                        w ->
                                w.setCamera3D(
                                        Paint3DContext.PROJECTION_ORTHO,
                                        new float[] {-1, 1, -2, 2, 0.1f, 100f},
                                        new float[] {0, 0, 4, 0, 0, 0, 0, 1, 0})));
    }

    @Test
    public void cameraRejectsMalformedInput() {
        assertParseFails(
                "{'type': 'camera3D', 'projection': 'ortho', 'right': 1, 'bottom': -1, 'top': 1}");
        assertParseFails("{'type': 'camera3D', 'projection': 'fisheye'}");
        assertParseFails("{'type': 'camera3D', 'eye': [1, 2]}");
    }

    // ---------------------------------------------------------------------------------------------
    // lights3D

    @Test
    public void lightsMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'lights3D', 'lights': ["
                                + "{'type': 'directional', 'dir': [0, -1, 0], 'color': '#FF808080',"
                                + " 'intensity': 0.5},"
                                + " {'type': 'point', 'pos': [1, 2, 3]}]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.setLights3D(
                                        new int[] {
                                            Paint3DContext.LIGHT_DIRECTIONAL,
                                            Paint3DContext.LIGHT_POINT
                                        },
                                        new int[] {0xFF808080, 0xFFFFFFFF},
                                        new float[] {0, -1, 0, 0.5f, 1, 2, 3, 1})));
    }

    @Test
    public void noLightsMeansAmbientOnly() throws JSONException {
        byte[] document = parse("{'type': 'lights3D'}");
        assertWrites(document, op(w -> w.setLights3D(new int[0], new int[0], new float[0])));
    }

    @Test
    public void lightsRejectMalformedInput() {
        assertParseFails("{'type': 'lights3D', 'lights': [{'type': 'spot', 'dir': [0, 0, 1]}]}");
        assertParseFails("{'type': 'lights3D', 'lights': [{'type': 'point'}]}");
        assertParseFails("{'type': 'lights3D', 'lights': [{'dir': [0, 1]}]}");
    }

    // ---------------------------------------------------------------------------------------------
    // matrix3D

    @Test
    public void matrixOpsMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'matrix3D', 'op': 'identity'},"
                                + " {'type': 'matrix3D', 'op': 'translate', 'x': 1, 'y': 2,"
                                + " 'z': 3},"
                                + " {'type': 'matrix3D', 'op': 'scale', 'x': 2, 'y': 3, 'z': 4},"
                                + " {'type': 'matrix3D', 'op': 'rotate', 'angle': 0.5,"
                                + " 'axis': [1, 0, 0]},"
                                + " {'type': 'matrix3D', 'op': 'rotate', 'angle': 1},"
                                + " {'type': 'matrix3D', 'op': 'multiply',"
                                + " 'm': [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 5, 6, 7, 1]},"
                                + " {'type': 'matrix3D'}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0]);
                            w.matrix3DOp(Paint3DContext.M3_TRANSLATE, new float[] {1, 2, 3});
                            w.matrix3DOp(Paint3DContext.M3_SCALE, new float[] {2, 3, 4});
                            w.matrix3DOp(
                                    Paint3DContext.M3_ROTATE_AXIS, new float[] {0.5f, 1, 0, 0});
                            w.matrix3DOp(Paint3DContext.M3_ROTATE_AXIS, new float[] {1, 0, 1, 0});
                            w.matrix3DOp(
                                    Paint3DContext.M3_MULTIPLY,
                                    new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 5, 6, 7, 1});
                            w.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0]);
                        }));
    }

    @Test
    public void matrixWithoutAnOpIsAnErrorNotAnIdentity() {
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () -> parse("{'type': 'matrix3D', 'translate': [1, 2, 3]}"));
        assertTrue(e.getMessage(), e.getMessage().contains("\"op\""));
    }

    @Test
    public void matrixRejectsMalformedInput() {
        assertParseFails("{'type': 'matrix3D', 'op': 'shear'}");
        assertParseFails("{'type': 'matrix3D', 'op': 'rotate', 'angle': 1, 'axis': [1, 0]}");
        assertParseFails("{'type': 'matrix3D', 'op': 'rotate'}");
        assertParseFails("{'type': 'matrix3D', 'op': 'multiply', 'm': [1, 0, 0]}");
    }

    // ---------------------------------------------------------------------------------------------
    // defineMesh3D

    @Test
    public void defineMeshMatchesTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'defineMesh3D', 'id': 3, 'verts': [0, 0, 0, 1, 0, 0, 0, 1, 0],"
                                + " 'indices': [0, 1, 2], 'normals': [0, 0, 1, 0, 0, 1, 0, 0, 1],"
                                + " 'uv': [0, 0, 1, 0, 0, 1]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.defineMesh3D(
                                        3,
                                        new int[] {0, 1, 2},
                                        new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0},
                                        new float[] {0, 0, 1, 0, 0, 1, 0, 0, 1},
                                        new float[] {0, 0, 1, 0, 0, 1})));
    }

    @Test
    public void emptyNormalsAndUvCountAsAbsent() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'defineMesh3D', 'id': 3, 'verts': [0, 0, 0, 1, 0, 0, 0, 1, 0],"
                                + " 'indices': [0, 1, 2], 'normals': [], 'uv': []}");
        assertWrites(
                document,
                op(
                        w ->
                                w.defineMesh3D(
                                        3,
                                        new int[] {0, 1, 2},
                                        new float[] {0, 0, 0, 1, 0, 0, 0, 1, 0},
                                        null,
                                        null)));
    }

    @Test
    public void defineMeshRejectsInconsistentArrays() {
        String verts = "'verts': [0, 0, 0, 1, 0, 0, 0, 1, 0]";
        assertParseFails("{'type': 'defineMesh3D', 'id': 1, " + verts + ", 'indices': [0, 1]}");
        assertParseFails(
                "{'type': 'defineMesh3D', 'id': 1, 'verts': [0, 0, 0, 1], 'indices': [0, 0, 0]}");
        assertParseFails(
                "{'type': 'defineMesh3D', 'id': 1, " + verts + ", 'indices': [0, 1, 2],"
                        + " 'normals': [0, 0, 1]}");
        assertParseFails(
                "{'type': 'defineMesh3D', 'id': 1, " + verts + ", 'indices': [0, 1, 2],"
                        + " 'uv': [0, 0, 1, 0]}");
    }

    // ---------------------------------------------------------------------------------------------
    // meshPrimitive3D

    @Test
    public void namedPrimitiveParametersReachTheWireInOrder() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere', 'radius': 2,"
                                + " 'center': [1, 2, 3], 'segments': 24, 'uv': 'uv2'},"
                                + " {'type': 'meshPrimitive3D', 'id': 2,"
                                + " 'primitive': 'Rounded_Cube',"
                                + " 'dimX': 1, 'dimY': 2, 'dimZ': 3, 'cornerRadius': 0.25}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshPrimitive(
                                    1,
                                    Primitive3D.SPHERE,
                                    24f,
                                    Primitive3D.UV2 << Primitive3D.FLAG_UV_SHIFT,
                                    new float[][] {{2, 1, 2, 3}});
                            // center defaults to the origin and sits before cornerRadius.
                            w.meshPrimitive(
                                    2,
                                    Primitive3D.ROUNDED_CUBE,
                                    0f,
                                    0,
                                    new float[][] {{1, 2, 3, 0, 0, 0, 0.25f}});
                        }));
    }

    @Test
    public void positionalParamsAndFlagsPassThrough() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 4, 'primitive': 'torus', 'flags': 8,"
                                + " 'params': [2, 0.5, 0, 0, 0], 'channels': [[1, 2]]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        4,
                                        Primitive3D.TORUS,
                                        0f,
                                        8,
                                        new float[][] {{2, 0.5f, 0, 0, 0}, {1, 2}})));
    }

    @Test
    public void tubeTakesRadiusDensityAndPoints() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 5, 'primitive': 'tube', 'radius': 0.1,"
                                + " 'ringsPerSpan': 4, 'points': [[0, 0, 0], [1, 0, 0], [1, 1, 0]],"
                                + " 'spline': true}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        5,
                                        Primitive3D.TUBE,
                                        0f,
                                        Primitive3D.FLAG_TUBE_PATH_DENSITY
                                                | Primitive3D.FLAG_SPLINE,
                                        new float[][] {{0.1f, 4, 0, 0, 0, 1, 0, 0, 1, 1, 0}})));
    }

    @Test
    public void profileTubeIsCappedByDefault() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 6, 'primitive': 'profileTube',"
                                + " 'points': [[0, 0, 0], [0, 1, 0]], 'radii': [0.2, 0.1]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        6,
                                        Primitive3D.PROFILE_TUBE,
                                        0f,
                                        Primitive3D.FLAG_TUBE_CAP,
                                        new float[][] {{2, 0, 0, 0, 0, 1, 0, 0.2f, 0.1f}})));
    }

    @Test
    public void sweepCarriesSectionPathScalesAndTwistsAsChannels() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 7, 'primitive': 'sweep',"
                                + " 'closed': false, 'section': [[0, 0], [1, 0], [0, 1]],"
                                + " 'path': [[0, 0, 0], [0, 0, 1]], 'scales': [1, 0.5],"
                                + " 'twists': [0, 1.5]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        7,
                                        Primitive3D.SWEEP,
                                        0f,
                                        0,
                                        new float[][] {
                                            {0, 1, 1, 0, 0, 0},
                                            {0, 0, 1, 0, 0, 1},
                                            {0, 0, 0, 0, 0, 1},
                                            {1, 0.5f},
                                            {0, 1.5f}
                                        })));
    }

    @Test
    public void extrudePathFlattensItsContours() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 8, 'primitive': 'extrudePath',"
                                + " 'depth': 0.5, 'bevel': 0.1,"
                                + " 'contours': [[[0, 0], [1, 0], [0, 1]]]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        8,
                                        Primitive3D.EXTRUDE_PATH,
                                        0f,
                                        0,
                                        new float[][] {
                                            {0.5f, 0, 0, 0, 0.1f, 1, 3, 0, 0, 1, 0, 0, 1}
                                        })));
    }

    @Test
    public void latheProfileRidesTheLastChannel() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 9, 'primitive': 'lathe',"
                                + " 'segments': 32, 'profile': [[1, 0], [0.5, 1]]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        9,
                                        Primitive3D.LATHE,
                                        32f,
                                        0,
                                        new float[][] {{0, 0, 0}, {1, 0, 0.5f, 1}})));
    }

    @Test
    public void meshPrimitiveRejectsMalformedInput() {
        assertParseFails("{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'teapot'}");
        assertParseFails("{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere'}");
        assertParseFails(
                "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere', 'radius': 1,"
                        + " 'uv': 'uv9'}");
        assertParseFails(
                "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'profileTube',"
                        + " 'points': [[0, 0, 0], [0, 1, 0]]}");
        assertParseFails(
                "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'tube', 'radius': 1,"
                        + " 'points': [[0, 0], [1, 1]]}");
        assertParseFails("{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'extrudePath'}");
        assertParseFails(
                "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'extrudePath',"
                        + " 'contours': [[[0, 0, 0]]]}");
    }

    @Test
    public void sweepTwistsWithoutScalesIsAnError() {
        // Channels are positional, so twists alone would be read as scales.
        assertParseFails(
                "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sweep',"
                        + " 'section': [[0, 0], [1, 0]], 'path': [[0, 0, 0], [0, 0, 1]],"
                        + " 'twists': [0, 1]}");
    }

    // ---------------------------------------------------------------------------------------------
    // meshExpression3D

    @Test
    public void heightFieldCompilesItsExpressionOverUAndV() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshExpression3D', 'id': 10, 'surface': 'heightField',"
                                + " 'xRange': [-1, 1], 'uCount': 16, 'zRange': [-2, 2],"
                                + " 'vCount': 8, 'height': 'sin(u) * v'}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshExpression(
                                        10,
                                        MeshExpression3D.SURFACE_HEIGHT_FIELD,
                                        0,
                                        new float[] {-1, 1, 16, -2, 2, 8, -1e6f, 1e6f},
                                        new float[][] {{VAR1, SIN, VAR2, MUL}},
                                        new float[0][],
                                        new float[0][])));
    }

    @Test
    public void heightFieldYRangeAndFlipWinding() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshExpression3D', 'id': 10, 'surface': 'height_field',"
                                + " 'xRange': [0, 1], 'uCount': 2, 'zRange': [0, 1], 'vCount': 2,"
                                + " 'yRange': [0, 3], 'height': 1, 'flipWinding': true}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshExpression(
                                        10,
                                        MeshExpression3D.SURFACE_HEIGHT_FIELD,
                                        MeshExpression3D.FLAG_FLIP_WINDING,
                                        new float[] {0, 1, 2, 0, 1, 2, 0, 3},
                                        new float[][] {{1}},
                                        new float[0][],
                                        new float[0][])));
    }

    @Test
    public void sphereSurfaceTakesADisplacement() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshExpression3D', 'id': 11, 'surface': 'sphere',"
                                + " 'radius': 1, 'uCount': 32, 'vCount': 16,"
                                + " 'displacement': '0.1 * sin(u)'},"
                                + " {'type': 'meshExpression3D', 'id': 12, 'surface': 'cylinder',"
                                + " 'radius': 1, 'height': 2, 'uCount': 8, 'vCount': 4}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshExpression(
                                    11,
                                    MeshExpression3D.SURFACE_SPHERE,
                                    0,
                                    new float[] {1, 32, 16},
                                    new float[][] {{0.1f, VAR1, SIN, MUL}},
                                    new float[0][],
                                    new float[0][]);
                            w.meshExpression(
                                    12,
                                    MeshExpression3D.SURFACE_CYLINDER,
                                    0,
                                    new float[] {1, 2, 8, 4},
                                    new float[][] {{0}},
                                    new float[0][],
                                    new float[0][]);
                        }));
    }

    @Test
    public void generalSurfaceWithNormalsAndUv() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'meshExpression3D', 'id': 13, 'uRange': [0, 1], 'uCount': 4,"
                                + " 'vRange': [0, 2], 'vCount': 5, 'x': 'u', 'y': '@v', 'z': 0,"
                                + " 'normal': [0, 0, 1], 'uv': ['@u', 'v']}");
        assertWrites(
                document,
                op(
                        w ->
                                w.meshExpression(
                                        13,
                                        MeshExpression3D.SURFACE_GENERAL,
                                        0,
                                        new float[] {0, 1, 4, 0, 2, 5},
                                        new float[][] {{VAR1}, {VAR2}, {0}},
                                        new float[][] {{0}, {0}, {1}},
                                        new float[][] {{VAR1}, {VAR2}})));
    }

    @Test
    public void gridCoordinatesDoNotLeakIntoOtherExpressions() throws JSONException {
        parse(
                "{'variables': {'u': 5}}",
                "{'type': 'meshExpression3D', 'id': 1, 'surface': 'heightField',"
                        + " 'xRange': [0, 1], 'uCount': 2, 'zRange': [0, 1], 'vCount': 2,"
                        + " 'height': '@u + @v'}");
        // A document's own "u" is restored and no "v" is left behind.
        float u = mParser.mVariables.get("u");
        assertTrue(Float.isNaN(u));
        assertTrue(Utils.idFromNan(u) != Utils.idFromNan(VAR1));
        assertFalse(mParser.mVariables.containsKey("v"));
    }

    @Test
    public void meshExpressionRejectsMalformedInput() {
        String grid = "'xRange': [0, 1], 'uCount': 2, 'zRange': [0, 1], 'vCount': 2";
        assertParseFails("{'type': 'meshExpression3D', 'id': 1, 'surface': 'torus'}");
        assertParseFails(
                "{'type': 'meshExpression3D', 'id': 1, 'surface': 'heightField', " + grid + "}");
        assertParseFails(
                "{'type': 'meshExpression3D', 'id': 1, 'surface': 'heightField',"
                        + " 'xRange': [0], 'uCount': 2, 'zRange': [0, 1], 'vCount': 2,"
                        + " 'height': 0}");
        assertParseFails(
                "{'type': 'meshExpression3D', 'id': 1, 'surface': 'heightField', " + grid
                        + ", 'height': 0, 'normal': [0, 1]}");
        assertParseFails(
                "{'type': 'meshExpression3D', 'id': 1, 'surface': 'heightField', " + grid
                        + ", 'height': 0, 'uv': ['u']}");
    }

    // ---------------------------------------------------------------------------------------------
    // texture3D and resources.bitmaps

    @Test
    public void base64BitmapIsEmbeddedAndNamedByTexture3D() throws JSONException {
        byte[] png = png(3, 2);
        byte[] document =
                parse(
                        "{'bitmaps': {'checker': {'base64': '" + base64(png) + "'}}}",
                        "{'type': 'texture3D', 'bitmap': '@checker'},"
                                + " {'type': 'texture3D', 'bitmap': '$checker'},"
                                + " {'type': 'texture3D', 'bitmap': 'checker'},"
                                + " {'type': 'texture3D'},"
                                + " {'type': 'texture3D', 'bitmap': 0},"
                                + " {'type': 'texture3D', 'bitmap': 5}");
        int id = mParser.mBitmapIds.get("checker");
        assertWrites(document, bitmapOp(id, 3, 2, png));
        assertWrites(
                document,
                op(
                        w -> {
                            w.setTexture3D(id);
                            w.setTexture3D(id);
                            w.setTexture3D(id);
                            w.setTexture3D(0);
                            w.setTexture3D(0);
                            w.setTexture3D(5);
                        }));

        // The packed words read back as an inline PNG of the header's size.
        ArrayList<Operation> operations = new ArrayList<>();
        mWriter.getBuffer().inflateFromBuffer(operations);
        BitmapData bitmap = first(operations, BitmapData.class);
        assertEquals(id, bitmap.getId());
        assertEquals(3, bitmap.getWidth());
        assertEquals(2, bitmap.getHeight());
        assertEquals(BitmapData.TYPE_PNG_8888, bitmap.getType());
        assertEquals(BitmapData.ENCODING_INLINE, bitmap.getEncoding());
    }

    @Test
    public void inlineBitmapHasTheTypePlayersDecode() throws JSONException {
        // Players refuse TYPE_PNG with ENCODING_INLINE, so unlike rcj (which writes TYPE_PNG) the
        // parser writes TYPE_PNG_8888: the same bytes as the writer's own bitmap path.
        byte[] png = png(2, 3);
        byte[] document = parse("{'bitmaps': {'img': {'base64': '" + base64(png) + "'}}}", "");
        int id = mParser.mBitmapIds.get("img");
        assertWrites(document, bytesOf(newWriter(), w -> w.getBuffer().storeBitmap(id, 2, 3, png)));
        ArrayList<Operation> operations = new ArrayList<>();
        mWriter.getBuffer().inflateFromBuffer(operations);
        BitmapData bitmap = first(operations, BitmapData.class);
        assertEquals(BitmapData.TYPE_PNG_8888, bitmap.getType());
        assertEquals(BitmapData.ENCODING_INLINE, bitmap.getEncoding());
    }

    @Test
    public void bitmapFileResolvesAgainstTheBaseDirectory() throws Exception {
        byte[] png = png(4, 5);
        Files.write(new File(mFolder.getRoot(), "tex.png").toPath(), png);
        mParser.setBaseDirectory(mFolder.getRoot());
        byte[] document =
                parse(
                        "{'bitmaps': [{'name': 'tex', 'value': {'file': 'tex.png'}}]}",
                        "{'type': 'texture3D', 'bitmap': '@tex'}");
        int id = mParser.mBitmapIds.get("tex");
        assertWrites(document, bitmapOp(id, 4, 5, png));
        assertWrites(document, op(w -> w.setTexture3D(id)));
    }

    @Test
    public void staticParseTakesTheBaseDirectory() throws Exception {
        byte[] png = png(1, 1);
        Files.write(new File(mFolder.getRoot(), "one.png").toPath(), png);
        // A bare string is a file name.
        String json = doc("{'bitmaps': {'one': 'one.png'}}", "");
        ByteBuffer buffer = RemoteComposeJsonParser.parse(json, null, mFolder.getRoot());
        byte[] document = Arrays.copyOf(buffer.array(), buffer.limit());
        assertTrue(indexOf(document, png) >= 0);
        // Without a base directory the name resolves against the working directory instead.
        assertThrows(JSONException.class, () -> RemoteComposeJsonParser.parse(json, null));
    }

    @Test
    public void unknownBitmapNamesTheDeclaredOnes() {
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () ->
                                parse(
                                        "{'bitmaps': {'checker': {'base64': '"
                                                + base64(png(1, 1))
                                                + "'}}}",
                                        "{'type': 'texture3D', 'bitmap': '@missing'}"));
        assertTrue(e.getMessage(), e.getMessage().contains("declared: checker"));
    }

    @Test
    public void bitmapsMustBePngsWithASource() {
        String notPng = base64("not a png, just some text".getBytes(StandardCharsets.UTF_8));
        assertParseFails("{'bitmaps': {'x': {'base64': '" + notPng + "'}}}", "");
        assertParseFails("{'bitmaps': {'x': {'url': 'https://example.com/x.png'}}}", "");
        assertParseFails("{'bitmaps': {'x': 'no-such-file.png'}}", "");
    }

    // ---------------------------------------------------------------------------------------------
    // drawMesh3D

    @Test
    public void drawMeshModesMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'drawMesh3D', 'mesh': 7},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 'smooth'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7,"
                                + " 'mode': 'drawmesh-zbuf-smooth'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 'gl-flat'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 5},"
                                + " {'drawMesh3D': {'mesh': 8, 'mode': 'canvas-smooth'}}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.drawMesh3D(7, Paint3DContext.MODE_SOFTWARE_FLAT);
                            w.drawMesh3D(7, Paint3DContext.MODE_SOFTWARE_SMOOTH);
                            w.drawMesh3D(7, Paint3DContext.MODE_DRAWMESH_ZBUF_SMOOTH);
                            w.drawMesh3D(7, 6);
                            w.drawMesh3D(7, 5);
                            w.drawMesh3D(8, Paint3DContext.MODE_CANVAS_SMOOTH);
                        }));
    }

    @Test
    public void wireframeSelectsEdges() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'drawMesh3D', 'mesh': 7, 'mode': 'canvas-flat',"
                                + " 'wireframe': true, 'edges': [0, 2]},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'edges': [1]}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.drawMesh3D(
                                    7,
                                    Paint3DContext.MODE_CANVAS_FLAT
                                            | Paint3DContext.MODE_WIREFRAME
                                            | Paint3DContext.WIRE_EDGE0
                                            | Paint3DContext.WIRE_EDGE2);
                            // Edges only mean something for a wireframe.
                            w.drawMesh3D(7, Paint3DContext.MODE_SOFTWARE_FLAT);
                        }));
    }

    @Test
    public void drawMeshRejectsMalformedInput() {
        assertParseFails("{'type': 'drawMesh3D', 'mesh': 7, 'mode': 'raytraced'}");
        assertParseFails("{'type': 'drawMesh3D', 'mesh': 7, 'wireframe': true, 'edges': [3]}");
        assertParseFails("{'type': 'drawMesh3D', 'mode': 'flat'}");
    }

    // ---------------------------------------------------------------------------------------------
    // touchExpression

    @Test
    public void touchExpressionCompilesAnInfixExpression() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'touchExpression', 'name': 'angle', 'defaultValue': 90,"
                                + " 'min': 0, 'max': 360, 'expression': 'touchX() * 0.5',"
                                + " 'stopMode': 'notchesEven', 'touchSpec': [12]}");
        int id = touchId("angle");
        assertWrites(
                document,
                touchOp(
                        id,
                        90f,
                        0f,
                        360f,
                        Float.NaN,
                        0,
                        new float[] {RemoteContext.FLOAT_TOUCH_POS_X, 0.5f, MUL},
                        TouchExpression.STOP_NOTCHES_EVEN,
                        new float[] {12},
                        null));
    }

    @Test
    public void touchExpressionDefaults() throws JSONException {
        // No expression means touchX(); no min means NaN, which wraps around max.
        byte[] document = parse("{'type': 'touchExpression', 'name': 't'}");
        assertWrites(
                document,
                touchOp(
                        touchId("t"),
                        0f,
                        Float.NaN,
                        1f,
                        Float.NaN,
                        0,
                        new float[] {RemoteContext.FLOAT_TOUCH_POS_X},
                        TouchExpression.STOP_GENTLY,
                        null,
                        null));
    }

    @Test
    public void touchExpressionNumberAndArrayForms() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'touchExpression', 'name': 'a', 'expression': 2,"
                                + " 'touchMode': 2, 'velocityId': 7, 'touchEffects': 1,"
                                + " 'easingSpec': [1, 2, 3]},"
                                + " {'type': 'touchExpression', 'name': 'b',"
                                + " 'expression': [0.25]}");
        assertWrites(
                document,
                touchOp(
                        touchId("a"),
                        0f,
                        Float.NaN,
                        1f,
                        7f,
                        1,
                        new float[] {2},
                        TouchExpression.STOP_ENDS,
                        null,
                        new float[] {1, 2, 3}));
        assertWrites(
                document,
                touchOp(
                        touchId("b"),
                        0f,
                        Float.NaN,
                        1f,
                        Float.NaN,
                        0,
                        new float[] {0.25f},
                        TouchExpression.STOP_GENTLY,
                        null,
                        null));
    }

    @Test
    public void touchStopModeNames() throws JSONException {
        String[] names = {"NOTCHES_EVEN", "notches_even", "notchesEven", "NotchesEven"};
        for (String name : names) {
            setup();
            byte[] document =
                    parse(
                            "{'type': 'touchExpression', 'name': 't', 'stopMode': '" + name
                                    + "', 'touchMode': 1}");
            assertWrites(
                    document,
                    touchOp(
                            touchId("t"),
                            0f,
                            Float.NaN,
                            1f,
                            Float.NaN,
                            0,
                            new float[] {RemoteContext.FLOAT_TOUCH_POS_X},
                            TouchExpression.STOP_NOTCHES_EVEN,
                            null,
                            null));
        }
    }

    @Test
    public void touchStopModeNumericStringIsTheModeNumber() throws JSONException {
        for (String key : new String[] {"stopMode", "touchMode"}) {
            setup();
            byte[] document = parse("{'type': 'touchExpression', 'name': 't', '" + key + "': '2'}");
            assertWrites(
                    document,
                    touchOp(
                            touchId("t"),
                            0f,
                            Float.NaN,
                            1f,
                            Float.NaN,
                            0,
                            new float[] {RemoteContext.FLOAT_TOUCH_POS_X},
                            TouchExpression.STOP_ENDS,
                            null,
                            null));
        }
    }

    @Test
    public void unknownTouchStopModeListsTheNames() {
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () -> parse("{'type': 'touchExpression', 'stopMode': 'bouncy'}"));
        assertTrue(e.getMessage(), e.getMessage().contains("notcheseven"));
    }

    // ---------------------------------------------------------------------------------------------
    // 3D2 keys

    @Test
    public void default3D2OptionsLeave3D1BytesUnchanged() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'drawMesh3D', 'mesh': 7, 'transparent': false, 'gpu': false,"
                                + " 'pbr': false, 'doubleSided': false, 'frontFace': 'ccw'},"
                                + " {'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere',"
                                + " 'radius': 1, 'axis': 'native'},"
                                + " {'type': 'meshPrimitive3D', 'id': 2, 'primitive': 'tube',"
                                + " 'radius': 0.1, 'points': [[0, 0, 0], [1, 0, 0]],"
                                + " 'curvatureDensity': false},"
                                + " {'type': 'meshExpression3D', 'id': 3, 'surface': 'sphere',"
                                + " 'radius': 1, 'uCount': 4, 'vCount': 3, 'flipNormals': false}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.drawMesh3D(7, Paint3DContext.MODE_SOFTWARE_FLAT);
                            w.meshPrimitive(
                                    1, Primitive3D.SPHERE, 0f, 0, new float[][] {{1, 0, 0, 0}});
                            w.meshPrimitive(
                                    2,
                                    Primitive3D.TUBE,
                                    0f,
                                    0,
                                    new float[][] {{0.1f, 0, 0, 0, 1, 0, 0}});
                            w.meshExpression(
                                    3,
                                    MeshExpression3D.SURFACE_SPHERE,
                                    0,
                                    new float[] {1, 4, 3},
                                    new float[][] {{0}},
                                    new float[0][],
                                    new float[0][]);
                        }));
    }

    @Test
    public void matrixPushAndPopMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'matrix3D', 'op': 'push'},"
                                + " {'type': 'matrix3D', 'op': 'translate', 'x': 1, 'y': 0,"
                                + " 'z': 0},"
                                + " {'type': 'matrix3D', 'op': 'POP'}");
        assertWrites(
                document,
                op(
                        w -> {
                            w.matrix3DPush();
                            w.matrix3DOp(Paint3DContext.M3_TRANSLATE, new float[] {1, 0, 0});
                            w.matrix3DPop();
                        }));
    }

    @Test
    public void unknownMatrixOpListsPushAndPop() {
        JSONException e =
                assertThrows(
                        JSONException.class, () -> parse("{'type': 'matrix3D', 'op': 'save'}"));
        assertTrue(e.getMessage(), e.getMessage().contains("push"));
        assertTrue(e.getMessage(), e.getMessage().contains("pop"));
    }

    @Test
    public void meshModeFlagsMatchTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'drawMesh3D', 'mesh': 7, 'transparent': true},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 'smooth',"
                                + " 'gpu': true},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'pbr': true},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'doubleSided': true},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 'canvas-flat',"
                                + " 'wireframe': true, 'edges': [1], 'transparent': true,"
                                + " 'doubleSided': true},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'transparent': false}");
        int flat = Paint3DContext.MODE_SOFTWARE_FLAT;
        assertWrites(
                document,
                op(
                        w -> {
                            w.drawMesh3D(7, flat | Paint3DContext.MODE_TRANSPARENT);
                            w.drawMesh3D(
                                    7,
                                    Paint3DContext.MODE_SOFTWARE_SMOOTH | Paint3DContext.MODE_GPU);
                            w.drawMesh3D(7, flat | Paint3DContext.MODE_PBR);
                            w.drawMesh3D(7, flat | Paint3DContext.MODE_DOUBLE_SIDED);
                            w.drawMesh3D(
                                    7,
                                    Paint3DContext.MODE_CANVAS_FLAT
                                            | Paint3DContext.MODE_WIREFRAME
                                            | Paint3DContext.WIRE_EDGE1
                                            | Paint3DContext.MODE_TRANSPARENT
                                            | Paint3DContext.MODE_DOUBLE_SIDED);
                            w.drawMesh3D(7, flat);
                        }));
    }

    @Test
    public void frontFaceMatchesTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'drawMesh3D', 'mesh': 7, 'frontFace': 'cw'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'mode': 'smooth',"
                                + " 'doubleSided': true, 'frontFace': 'CW'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7, 'frontFace': 'ccw'},"
                                + " {'type': 'drawMesh3D', 'mesh': 7}");
        int flat = Paint3DContext.MODE_SOFTWARE_FLAT;
        assertWrites(
                document,
                op(
                        w -> {
                            w.drawMesh3D(7, flat | Paint3DContext.MODE_FRONT_CW);
                            w.drawMesh3D(
                                    7,
                                    Paint3DContext.MODE_SOFTWARE_SMOOTH
                                            | Paint3DContext.MODE_DOUBLE_SIDED
                                            | Paint3DContext.MODE_FRONT_CW);
                            // "ccw" is the default.
                            w.drawMesh3D(7, flat);
                            w.drawMesh3D(7, flat);
                        }));
    }

    @Test
    public void unknownFrontFaceListsTheValues() {
        String draw = "{'type': 'drawMesh3D', 'mesh': 7, 'frontFace': ";
        for (String value : new String[] {"'clockwise'", "'left'", "''", "true", "1"}) {
            setup();
            JSONException e =
                    assertThrows(value, JSONException.class, () -> parse(draw + value + "}"));
            assertTrue(e.getMessage(), e.getMessage().contains("frontFace"));
            assertTrue(e.getMessage(), e.getMessage().contains("\"ccw\" or \"cw\""));
        }
    }

    @Test
    public void flipNormalsMatchesTheWriter() throws JSONException {
        String sphere = "'surface': 'sphere', 'radius': 1, 'uCount': 4, 'vCount': 3";
        byte[] document =
                parse(
                        "{'type': 'meshExpression3D', 'id': 11, " + sphere + ","
                                + " 'flipNormals': true},"
                                + " {'type': 'meshExpression3D', 'id': 12, " + sphere + ","
                                + " 'flipWinding': true, 'flipNormals': true},"
                                + " {'type': 'meshExpression3D', 'id': 13, " + sphere + ","
                                + " 'flipNormals': false}");
        float[][] none = new float[0][];
        float[][] zero = {{0}};
        float[] params = {1, 4, 3};
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshExpression(
                                    11,
                                    MeshExpression3D.SURFACE_SPHERE,
                                    MeshExpression3D.FLAG_FLIP_NORMALS,
                                    params,
                                    zero,
                                    none,
                                    none);
                            w.meshExpression(
                                    12,
                                    MeshExpression3D.SURFACE_SPHERE,
                                    MeshExpression3D.FLAG_FLIP_WINDING
                                            | MeshExpression3D.FLAG_FLIP_NORMALS,
                                    params,
                                    zero,
                                    none,
                                    none);
                            w.meshExpression(
                                    13, MeshExpression3D.SURFACE_SPHERE,
                                    0, params, zero, none, none);
                        }));
    }

    @Test
    public void infinitePlaneDefaults() throws JSONException {
        byte[] document =
                parse("{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'infinitePlane'}");
        // At the origin, no grid, 1-pixel lines, opaque mid grey (#FF808080).
        float grey = 0x80 / 255f;
        assertWrites(
                document,
                op(
                        w ->
                                w.meshPrimitive(
                                        1,
                                        Primitive3D.INFINITE_PLANE,
                                        0f,
                                        0,
                                        new float[][] {{0, 0, 0, 0, 1, 1, grey, grey, grey}})));
    }

    @Test
    public void infinitePlaneCenterSpacingLineWidthAndColor() throws JSONException {
        String plane = " {'type': 'meshPrimitive3D', 'primitive': 'infinitePlane', 'id': ";
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'Infinite_Plane',"
                                + " 'center': [0, -1, 0], 'spacing': 0.5, 'lineWidth': 2,"
                                + " 'lineColor': '#336699'},"
                                + plane
                                + "2, 'lineColor': '#80FF0000'},"
                                + plane
                                + "3, 'lineColor': 4278255360},"
                                + plane
                                + "4, 'lineColor': -16776961},"
                                + plane
                                + "5, 'lineColor': [1, 0.25, 0.5, 'continuousSec()']},"
                                + plane
                                + "6, 'params': [0, 2, 0, 1, 1, 1, 1, 1, 1], 'segments': 3}");
        int type = Primitive3D.INFINITE_PLANE;
        float[] explicit = {0, -1, 0, 0.5f, 2, 1, 0x33 / 255f, 0x66 / 255f, 0x99 / 255f};
        float[] halfRed = {0, 0, 0, 0, 1, 0x80 / 255f, 1, 0, 0};
        float[] animated = {0, 0, 0, 0, 1, 1, 0.25f, 0.5f, RemoteContext.FLOAT_CONTINUOUS_SEC};
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshPrimitive(1, type, 0f, 0, new float[][] {explicit});
                            w.meshPrimitive(2, type, 0f, 0, new float[][] {halfRed});
                            w.meshPrimitive(
                                    3, type, 0f, 0, new float[][] {{0, 0, 0, 0, 1, 1, 0, 1, 0}});
                            w.meshPrimitive(
                                    4, type, 0f, 0, new float[][] {{0, 0, 0, 0, 1, 1, 0, 0, 1}});
                            w.meshPrimitive(5, type, 0f, 0, new float[][] {animated});
                            w.meshPrimitive(
                                    6, type, 3f, 0, new float[][] {{0, 2, 0, 1, 1, 1, 1, 1, 1}});
                        }));
    }

    @Test
    public void infinitePlaneRejectsMalformedInput() {
        String plane = "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'infinitePlane', ";
        String[] bad = {
            "'center': [0, 0]",
            "'lineColor': 'red'",
            "'lineColor': '#12345'",
            "'lineColor': [1, 1, 1]",
            "'lineColor': 8589934591",
            "'lineColor': true"
        };
        for (String option : bad) {
            setup();
            assertThrows(option, JSONException.class, () -> parse(plane + option + "}"));
        }
        // 0..255 channels would all clamp to 1, so they are refused rather than guessed at.
        setup();
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () -> parse(plane + "'lineColor': [255, 128, 0, 0]}"));
        assertTrue(e.getMessage(), e.getMessage().contains("0..1"));
    }

    @Test
    public void axisNamesAndNumbersSetTheAxisFlag() throws JSONException {
        String plane =
                " {'type': 'meshPrimitive3D', 'primitive': 'plane', 'width': 2, 'height': 3,";
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere', 'radius': 1,"
                                + " 'axis': 'X'},"
                                + plane
                                + " 'id': 2, 'axis': 'z', 'uv': 'uv2'},"
                                + plane
                                + " 'id': 3, 'axis': 2},"
                                + plane
                                + " 'id': 4, 'axis': 'Native'}");
        int shift = Primitive3D.FLAG_AXIS_SHIFT;
        float[][] params = {{2, 3, 0, 0, 0}};
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshPrimitive(
                                    1,
                                    Primitive3D.SPHERE,
                                    0f,
                                    Primitive3D.AXIS_X << shift,
                                    new float[][] {{1, 0, 0, 0}});
                            w.meshPrimitive(
                                    2,
                                    Primitive3D.PLANE,
                                    0f,
                                    Primitive3D.AXIS_Z << shift
                                            | Primitive3D.UV2 << Primitive3D.FLAG_UV_SHIFT,
                                    params);
                            w.meshPrimitive(
                                    3, Primitive3D.PLANE, 0f, Primitive3D.AXIS_Y << shift, params);
                            // Native is no flag at all, so it is also valid in a 3D1 document.
                            w.meshPrimitive(4, Primitive3D.PLANE, 0f, 0, params);
                        }));
    }

    @Test
    public void unknownAxisListsTheNames() {
        String sphere = "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'sphere', 'radius': 1,";
        for (String axis : new String[] {"'w'", "4", "-1", "1.5", "true"}) {
            setup();
            JSONException e =
                    assertThrows(
                            JSONException.class, () -> parse(sphere + " 'axis': " + axis + "}"));
            assertTrue(e.getMessage(), e.getMessage().contains("native"));
        }
    }

    @Test
    public void curvatureDensityFlagsTubes() throws JSONException {
        String points = " 'points': [[0, 0, 0], [1, 0, 0]], 'curvatureDensity': true},";
        byte[] document =
                parse(
                        "{'type': 'meshPrimitive3D', 'id': 1, 'primitive': 'tube', 'radius': 0.1,"
                                + points
                                + " {'type': 'meshPrimitive3D', 'id': 2, 'primitive': 'capTube',"
                                + " 'radius': 0.1,"
                                + points
                                + " {'type': 'meshPrimitive3D', 'id': 3,"
                                + " 'primitive': 'profileTube', 'radii': [0.2, 0.1],"
                                + points
                                + " {'type': 'meshPrimitive3D', 'id': 4, 'primitive': 'tube',"
                                + " 'params': [0.1, 0, 0, 0, 1, 0, 0], 'curvatureDensity': true}");
        int density = Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        float[][] tube = {{0.1f, 0, 0, 0, 1, 0, 0}};
        assertWrites(
                document,
                op(
                        w -> {
                            w.meshPrimitive(1, Primitive3D.TUBE, 0f, density, tube);
                            w.meshPrimitive(2, Primitive3D.CAP_TUBE, 0f, density, tube);
                            w.meshPrimitive(
                                    3,
                                    Primitive3D.PROFILE_TUBE,
                                    0f,
                                    Primitive3D.FLAG_TUBE_CAP | density,
                                    new float[][] {{2, 0, 0, 0, 1, 0, 0, 0.2f, 0.1f}});
                            w.meshPrimitive(4, Primitive3D.TUBE, 0f, density, tube);
                        }));
    }

    @Test
    public void curvatureDensityIsOnlyForTubes() {
        setup();
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () ->
                                parse(
                                        "{'type': 'meshPrimitive3D', 'id': 1,"
                                                + " 'primitive': 'sphere', 'radius': 1,"
                                                + " 'curvatureDensity': true}"));
        assertTrue(e.getMessage(), e.getMessage().contains("curvatureDensity"));
    }

    @Test
    public void ambientLightMatchesTheWriter() throws JSONException {
        byte[] document =
                parse(
                        "{'type': 'lights3D', 'lights': ["
                                + "{'type': 'ambient', 'color': '#FF202020', 'intensity': 0.3},"
                                + " {'type': 'directional', 'dir': [0, -1, 0]},"
                                + " {'type': 'Ambient'}]}");
        assertWrites(
                document,
                op(
                        w ->
                                w.setLights3D(
                                        new int[] {
                                            Paint3DContext.LIGHT_AMBIENT,
                                            Paint3DContext.LIGHT_DIRECTIONAL,
                                            Paint3DContext.LIGHT_AMBIENT
                                        },
                                        new int[] {0xFF202020, 0xFFFFFFFF, 0xFFFFFFFF},
                                        new float[] {0, 0, 0, 0.3f, 0, -1, 0, 1, 0, 0, 0, 1})));
    }

    @Test
    public void ambientLightRejectsDirectionAndPosition() {
        for (String key : new String[] {"dir", "pos"}) {
            setup();
            JSONException e =
                    assertThrows(
                            JSONException.class,
                            () ->
                                    parse(
                                            "{'type': 'lights3D', 'lights': [{'type': 'ambient', '"
                                                    + key
                                                    + "': [0, 1, 0]}]}"));
            assertTrue(e.getMessage(), e.getMessage().contains("only color and intensity"));
        }
        // An unknown type names the valid ones, ambient included.
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () -> parse("{'type': 'lights3D', 'lights': [{'type': 'spot'}]}"));
        assertTrue(e.getMessage(), e.getMessage().contains("ambient"));
    }

    @Test
    public void bitmapListTakesNameAndData() throws JSONException {
        byte[] a = png(3, 2);
        byte[] b = png(4, 4);
        byte[] c = png(1, 5);
        byte[] document =
                parse(
                        "{'bitmaps': [{'name': 'a', 'data': '" + base64(a) + "'},"
                                + " {'name': 'b', 'data': 'data:image/png;base64,"
                                + base64(b)
                                + "'},"
                                + " {'c': {'data': '"
                                + base64(c)
                                + "'}}]}",
                        "{'type': 'texture3D', 'bitmap': '@a'},"
                                + " {'type': 'texture3D', 'bitmap': '@b'},"
                                + " {'type': 'texture3D', 'bitmap': '@c'}");
        int idA = mParser.mBitmapIds.get("a");
        int idB = mParser.mBitmapIds.get("b");
        int idC = mParser.mBitmapIds.get("c");
        assertWrites(document, bitmapOp(idA, 3, 2, a));
        assertWrites(document, bitmapOp(idB, 4, 4, b));
        assertWrites(document, bitmapOp(idC, 1, 5, c));
        assertWrites(
                document,
                op(
                        w -> {
                            w.setTexture3D(idA);
                            w.setTexture3D(idB);
                            w.setTexture3D(idC);
                        }));
    }

    @Test
    public void jpegBitmapIsRejectedWithAClearMessage() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I'};
        JSONException e =
                assertThrows(
                        JSONException.class,
                        () ->
                                parse(
                                        "{'bitmaps': [{'name': 'photo', 'data': '"
                                                 + base64(jpeg)
                                                 + "'}]}",
                                        ""));
        assertTrue(e.getMessage(), e.getMessage().contains("JPEG"));
        assertParseFails("{'bitmaps': [{'name': 'x', 'data': 'data:image/png,abc'}]}", "");
        assertParseFails("{'bitmaps': [{'name': 'x'}]}", "");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers

    /** 3D ops are registered in the experimental profile only. */
    private static RemoteComposeWriter newWriter() {
        return new RemoteComposeWriter(
                400,
                800,
                "Test",
                7,
                RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL,
                RemoteComposeJsonParser.DEFAULT_PLATFORM);
    }

    /** A document with {@code resources} (or none) and a canvas of {@code commands}. */
    private static String doc(String resources, String commands) {
        String prefix = resources == null ? "{" : "{'resources': " + resources + ", ";
        // Single quotes keep the JSON in these tests readable; none of the values contain one.
        return (prefix + "'root': {'type': 'canvas', 'commands': [" + commands + "]}}")
                .replace('\'', '"');
    }

    private byte[] parse(String commands) throws JSONException {
        return parse(null, commands);
    }

    private byte[] parse(String resources, String commands) throws JSONException {
        mParser.parse(doc(resources, commands));
        WireBuffer buffer = mWriter.getBuffer().getBuffer();
        return Arrays.copyOf(buffer.getBuffer(), buffer.getSize());
    }

    private void assertParseFails(String commands) {
        assertParseFails(null, commands);
    }

    private void assertParseFails(String resources, String commands) {
        setup();
        assertThrows(JSONException.class, () -> parse(resources, commands));
    }

    /** The bytes {@code write} adds to a fresh writer: the reference encoding. */
    private static byte[] op(Consumer<RemoteComposeWriter3D> write) {
        return bytesOf(newWriter(), w -> write.accept(w.getRc3DWriter()));
    }

    private static byte[] bytesOf(RemoteComposeWriter writer, Consumer<RemoteComposeWriter> write) {
        int start = writer.getBuffer().getBuffer().getSize();
        write.accept(writer);
        WireBuffer buffer = writer.getBuffer().getBuffer();
        return Arrays.copyOfRange(buffer.getBuffer(), start, buffer.getSize());
    }

    /**
     * A DATA_BITMAP of an inline PNG, spelled out word by word: TYPE_PNG_8888 and ENCODING_INLINE
     * (both 0) in the high halves of the width and height words.
     */
    private static byte[] bitmapOp(int id, int width, int height, byte[] png) {
        return bytesOf(
                newWriter(),
                w -> {
                    WireBuffer buffer = w.getBuffer().getBuffer();
                    buffer.start(Operations.DATA_BITMAP);
                    buffer.writeInt(id);
                    buffer.writeInt((BitmapData.TYPE_PNG_8888 << 16) | width);
                    buffer.writeInt((BitmapData.ENCODING_INLINE << 16) | height);
                    buffer.writeBuffer(png);
                });
    }

    private static byte[] touchOp(
            int id,
            float defaultValue,
            float min,
            float max,
            float velocityId,
            int touchEffects,
            float[] exp,
            int stopMode,
            float[] touchSpec,
            float[] easingSpec) {
        return bytesOf(
                newWriter(),
                w ->
                        w.getBuffer()
                                .addTouchExpression(
                                        id,
                                        defaultValue,
                                        min,
                                        max,
                                        velocityId,
                                        touchEffects,
                                        exp,
                                        stopMode,
                                        touchSpec,
                                        easingSpec));
    }

    /** The id of the touch expression named {@code name}. */
    private int touchId(String name) {
        return Utils.idFromNan(mParser.mVariables.get(name));
    }

    private static void assertWrites(byte[] document, byte[] expected) {
        assertTrue(
                "expected " + hex(expected) + "\nin " + hex(document),
                indexOf(document, expected) >= 0);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    private static <T> T first(ArrayList<Operation> operations, Class<T> type) {
        for (Operation operation : operations) {
            if (type.isInstance(operation)) {
                return type.cast(operation);
            }
        }
        throw new AssertionError("no " + type.getSimpleName() + " in " + operations);
    }

    /** The signature and IHDR chunk of a PNG: all the parser reads. */
    private static byte[] png(int width, int height) {
        ByteBuffer chunk = ByteBuffer.allocate(17);
        chunk.put(new byte[] {'I', 'H', 'D', 'R'}).putInt(width).putInt(height);
        chunk.put(new byte[] {8, 6, 0, 0, 0}); // 8-bit RGBA, no interlace
        CRC32 crc = new CRC32();
        crc.update(chunk.array());
        ByteBuffer png = ByteBuffer.allocate(8 + 4 + 17 + 4);
        png.put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        png.putInt(13).put(chunk.array()).putInt((int) crc.getValue());
        return png.array();
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
