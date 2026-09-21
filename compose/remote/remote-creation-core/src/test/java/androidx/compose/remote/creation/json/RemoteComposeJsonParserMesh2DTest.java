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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.operations.AddMesh2D;
import androidx.compose.remote.core.operations.DrawMesh2D;
import androidx.compose.remote.core.operations.MatrixFromMesh2D;
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.json.JSONException;
import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Tests for the JSON spelling of the 2D mesh commands.
 *
 * <p>These check the layer a document author actually touches: that the readable names for {@code
 * type}, {@code layout}, {@code blend} and {@code flags} reach the wire as the right ints, that a
 * mesh declared with an {@code id} can be referred to by that name later, and that an omitted
 * channel stays omitted rather than being silently defaulted to zero.
 */
public class RemoteComposeJsonParserMesh2DTest {

    private RemoteComposeWriter mWriter;
    private RemoteComposeJsonParser mParser;

    @Before
    public void setup() {
        // Meshes are registered in the experimental profiles only, so a document must opt in
        // before it can write them - the same deal as sound and macros.
        mWriter =
                new RemoteComposeWriter(
                        400,
                        800,
                        "Test",
                        7,
                        RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL,
                        new MockMeshPlatform());
        mParser = new RemoteComposeJsonParser(mWriter);
    }

    /** Wrap canvas commands in the surrounding document boilerplate. */
    private static String canvas(String commands) {
        return "{\"root\": {\"type\": \"canvas\", \"commands\": [" + commands + "]}}";
    }

    private ArrayList<Operation> parse(String commands) throws JSONException {
        mParser.parse(canvas(commands));
        ArrayList<Operation> operations = new ArrayList<>();
        mWriter.getBuffer().inflateFromBuffer(operations);
        return operations;
    }

    private static <T> T first(ArrayList<Operation> operations, Class<T> type) {
        for (Operation operation : operations) {
            if (type.isInstance(operation)) {
                return type.cast(operation);
            }
        }
        throw new AssertionError("no " + type.getSimpleName() + " in " + operations);
    }

    @Test
    public void expressionMeshReachesTheWire() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": 1, \"layout\": \"grid\","
                                + " \"uCount\": 24, \"vCount\": 16,"
                                + " \"x\": \"u * 300\", \"y\": \"v * 200\"}");

        AddMesh2D mesh = first(operations, AddMesh2D.class);
        String description = mesh.toString();
        assertTrue(description, description.contains("type=" + AddMesh2D.TYPE_EXPRESSION));
        assertTrue(description, description.contains("layout=" + Mesh2DGenerator.LAYOUT_GRID));
        assertTrue(description, description.contains("u=24"));
        assertTrue(description, description.contains("v=16"));
    }

    @Test
    public void layoutNamesMapToTheirWireValues() throws JSONException {
        String[] names = {"grid", "polar", "ring", "strip", "fan", "pathStrip"};
        int[] expected = {
            Mesh2DGenerator.LAYOUT_GRID,
            Mesh2DGenerator.LAYOUT_POLAR,
            Mesh2DGenerator.LAYOUT_RING,
            Mesh2DGenerator.LAYOUT_STRIP,
            Mesh2DGenerator.LAYOUT_FAN,
            Mesh2DGenerator.LAYOUT_PATH_STRIP,
        };
        for (int i = 0; i < names.length; i++) {
            setup(); // a fresh writer per case, so each mesh is the only one in its document
            ArrayList<Operation> operations =
                    parse(
                            "{\"type\": \"addMesh2D\", \"layout\": \""
                                    + names[i]
                                    + "\", \"uCount\": 8, \"vCount\": 4}");
            AddMesh2D mesh = first(operations, AddMesh2D.class);
            assertTrue(names[i] + " -> " + mesh, mesh.toString().contains("layout=" + expected[i]));
        }
    }

    @Test
    public void meshCanBeReferredToByTheNameItWasDeclaredWith() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": \"flag\", \"layout\": \"grid\","
                                + " \"uCount\": 4, \"vCount\": 4},"
                                + "{\"type\": \"drawMesh2D\", \"mesh\": \"flag\"}");

        AddMesh2D mesh = first(operations, AddMesh2D.class);
        DrawMesh2D draw = first(operations, DrawMesh2D.class);
        // The draw must point at the id the writer actually allocated, not at the author's name.
        assertTrue(
                "mesh " + mesh + " draw " + draw, draw.toString().contains(idOf(mesh.toString())));
    }

    /** Pull the bracketed id out of an operation's toString. */
    private static String idOf(String description) {
        int open = description.indexOf('[');
        int close = description.indexOf(']');
        return description.substring(open + 1, close);
    }

    @Test
    public void untexturedMeshDefaultsToColoursOnly() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": 1, \"uCount\": 4, \"vCount\": 4},"
                                + "{\"type\": \"drawMesh2D\", \"mesh\": 1}");

        DrawMesh2D draw = first(operations, DrawMesh2D.class);
        assertTrue(draw.toString(), draw.toString().contains("" + DrawMesh2D.BLEND_COLORS_ONLY));
    }

    @Test
    public void matrixFlagNamesMapToTheirWireValues() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": 1, \"uCount\": 4, \"vCount\": 4},"
                                + "{\"type\": \"matrixFromMesh2D\", \"mesh\": 1,"
                                + " \"u\": 0.35, \"v\": 0.5, \"flags\": \"rotation\"}");

        MatrixFromMesh2D matrix = first(operations, MatrixFromMesh2D.class);
        assertTrue(
                matrix.toString(), matrix.toString().endsWith("" + MatrixFromMesh2D.FLAG_ROTATION));
    }

    @Test
    public void literalValuesForm() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": 1, \"source\": \"values\","
                                + " \"layout\": \"grid\","
                                + " \"verts\": [0,0, 10,0, 0,10],"
                                + " \"indices\": [0,1,2],"
                                + " \"colors\": [\"#FF0000\", \"#00FF00\", \"#0000FF\"]}");

        AddMesh2D mesh = first(operations, AddMesh2D.class);
        assertTrue(mesh.toString(), mesh.toString().contains("type=" + AddMesh2D.TYPE_VALUES));
    }

    @Test
    public void halfFloatValuesForm() throws JSONException {
        ArrayList<Operation> operations =
                parse(
                        "{\"type\": \"addMesh2D\", \"id\": 1, \"source\": \"f16Values\","
                                + " \"verts\": [0,0, 16,0, 0,16],"
                                + " \"uv\": [0,0, 1,0, 0,1]}");

        AddMesh2D mesh = first(operations, AddMesh2D.class);
        assertTrue(mesh.toString(), mesh.toString().contains("type=" + AddMesh2D.TYPE_F16_VALUES));
    }

    @Test
    public void nestedCommandFormKeepsItsOwnTypeField() throws JSONException {
        // The proposal writes meshes in the nested form, where the command name is the key and the
        // body carries its own "type". Without care that inner field overwrites the command name
        // and the whole command dispatches to "expression" instead of "addMesh2D".
        ArrayList<Operation> operations =
                parse(
                        "{\"addMesh2D\": {\"id\": 7, \"type\": \"expression\","
                                + " \"layout\": \"pathStrip\", \"uCount\": 64, \"vCount\": 2,"
                                + " \"width\": 28}}");

        AddMesh2D mesh = first(operations, AddMesh2D.class);
        assertTrue(mesh.toString(), mesh.toString().contains("type=" + AddMesh2D.TYPE_EXPRESSION));
        assertTrue(
                mesh.toString(),
                mesh.toString().contains("layout=" + Mesh2DGenerator.LAYOUT_PATH_STRIP));
    }

    @Test
    public void expressionMeshRequiresItsResolution() {
        // uCount and vCount are required rather than defaulted, so the runtime cost of a mesh is
        // visible at the place it is authored instead of buried in a default.
        assertThrows(
                JSONException.class,
                () -> parse("{\"type\": \"addMesh2D\", \"id\": 1, \"layout\": \"grid\"}"));
    }

    @Test
    public void unknownLayoutIsRejected() {
        assertThrows(
                JSONException.class,
                () ->
                        parse(
                                "{\"type\": \"addMesh2D\", \"id\": 1, \"layout\": \"hexagons\","
                                        + " \"uCount\": 4, \"vCount\": 4}"));
    }

    @Test
    public void documentWithAMeshEncodes() throws JSONException {
        mParser.parse(
                canvas(
                        "{\"type\": \"addMesh2D\", \"id\": \"ripple\", \"layout\": \"polar\","
                            + " \"uCount\": 32, \"vCount\": 8, \"alpha\": 1, \"red\": \"v\","
                            + " \"green\": 0, \"blue\": \"1 - v\"},{\"type\": \"drawMesh2D\","
                            + " \"mesh\": \"ripple\"}"));
        byte[] encoded = mWriter.encodeToByteArray();
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    @Test
    public void jsonSupportsDynamicVertsApplyAliasMeshIdAndHalfFloatFlag() throws JSONException {
        mParser.parse(
                "{\"resources\": {\"variables\": {\"topY\": \"10 + 5\"}},"
                        + " \"root\": {\"type\": \"canvas\","
                        + " \"commands\": ["
                        + "{\"type\": \"addMesh2D\", \"id\": \"dynPatch\","
                        + " \"verts\": [0, \"@topY\", 20, 0, 0, 20]},"
                        + "{\"type\": \"addMesh2D\", \"id\": \"halfPatch\", \"halfFloat\": true,"
                        + " \"verts\": [0, 15, 20, 0, 0, 20]},"
                        + "{\"type\": \"drawMesh2D\", \"meshId\": \"halfPatch\"},"
                        + "{\"type\": \"matrixFromMesh2D\", \"meshId\": \"dynPatch\","
                        + " \"u\": 0.5, \"v\": 0.5, \"apply\": \"scale\"}"
                        + "]}}");
        ArrayList<Operation> operations = new ArrayList<>();
        mWriter.getBuffer().inflateFromBuffer(operations);

        ArrayList<AddMesh2D> meshes = new ArrayList<>();
        for (Operation op : operations) {
            if (op instanceof AddMesh2D) {
                meshes.add((AddMesh2D) op);
            }
        }
        assertEquals(2, meshes.size());
        assertTrue(
                meshes.get(0).toString(),
                meshes.get(0).toString().contains("type=" + AddMesh2D.TYPE_VALUES));
        assertTrue(
                meshes.get(1).toString(),
                meshes.get(1).toString().contains("type=" + AddMesh2D.TYPE_F16_VALUES));

        DrawMesh2D draw = first(operations, DrawMesh2D.class);
        MatrixFromMesh2D matrix = first(operations, MatrixFromMesh2D.class);

        assertTrue(draw.toString(), draw.toString().contains(idOf(meshes.get(1).toString())));
        assertTrue(matrix.toString(), matrix.toString().endsWith("" + MatrixFromMesh2D.FLAG_SCALE));
    }

    /** The smallest platform a writer will accept. */
    private static class MockMeshPlatform implements RcPlatformServices {
        @Override
        public byte[] imageToByteArray(Object image) {
            return new byte[0];
        }

        @Override
        public int getImageWidth(Object image) {
            return 0;
        }

        @Override
        public int getImageHeight(Object image) {
            return 0;
        }

        @Override
        public boolean isAlpha8Image(Object image) {
            return false;
        }

        @Override
        public Object parsePath(String pathData) {
            return null;
        }

        @Override
        public float[] pathToFloatArray(Object path) {
            return new float[0];
        }

        @Override
        public void log(
                RcPlatformServices.@NonNull LogCategory category, @NonNull String message) {}
    }
}
