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

import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.d3.MeshExpression3D;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;
import androidx.compose.remote.creation.RemoteComposeWriter3D;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * The JSON spelling of the 3D canvas commands: {@code clearDepth3D}, {@code material3D}, {@code
 * depthBias3D}, {@code camera3D}, {@code lights3D}, {@code matrix3D}, {@code defineMesh3D}, {@code
 * meshPrimitive3D}, {@code meshExpression3D}, {@code texture3D} and {@code drawMesh3D}.
 *
 * <p>The name tables, defaults and checks follow the reference converters (rcj and json2rc.js), so
 * a document compiles to the same bytes whichever toolchain builds it. Every float goes through
 * {@link RemoteComposeJsonParser#parseFloat}, so it may also be a variable or an expression, and
 * values are parsed in the same order as the reference converters do because an expression emits
 * its own ops as it is parsed.
 *
 * <p>A wrong name is an error rather than a default: a mistyped primitive, mode or projection
 * would otherwise draw something plausible and wrong.
 */
final class Command3DParser {

    /** Primitive names, lower-cased with {@code _} and {@code -} removed. */
    private static final Map<String, Integer> PRIMITIVES = new HashMap<>();

    /**
     * Named parameters per primitive, in wire order, so a document can say {@code {"radius": 1}}
     * instead of counting positions. {@code center}, {@code from} and {@code to} are 3 values that
     * default to the origin; every other key is required. Primitives absent from this table and
     * not special-cased take a positional {@code params} list.
     */
    private static final Map<String, String[]> PRIMITIVE_PARAMS = new HashMap<>();

    /** UV generation modes, stored in the primitive flags at {@link Primitive3D#FLAG_UV_SHIFT}. */
    private static final Map<String, Integer> UV_MODES = new HashMap<>();

    /**
     * Axis remaps, stored in the primitive flags at {@link Primitive3D#FLAG_AXIS_SHIFT}: the
     * axis a primitive's built-in up or depth direction is turned onto.
     */
    private static final Map<String, Integer> AXES = new HashMap<>();

    /** {@code meshExpression3D} surface families, lower-cased with {@code _} removed. */
    private static final Map<String, Integer> SURFACES = new HashMap<>();

    private static final Map<String, Integer> PROJECTIONS = new HashMap<>();

    private static final Map<String, Integer> MATRIX_OPS = new HashMap<>();

    private static final Map<String, Integer> LIGHT_TYPES = new HashMap<>();

    /**
     * Render modes, {@code (backend << 1) | smoothBit}. Spelled out because a document asking for
     * the wrong backend still draws - it falls back to software - so a typo would go unnoticed.
     */
    private static final Map<String, Integer> MESH_MODES = new HashMap<>();

    /** The {@code drawMesh3D} keys of the mode flags, parallel to {@link #MODE_FLAGS}. */
    private static final String[] MODE_FLAG_KEYS = {"transparent", "gpu", "pbr", "doubleSided"};

    /** The mode bit each of {@link #MODE_FLAG_KEYS} sets. */
    private static final int[] MODE_FLAGS = {
        Paint3DContext.MODE_TRANSPARENT,
        Paint3DContext.MODE_GPU,
        Paint3DContext.MODE_PBR,
        Paint3DContext.MODE_DOUBLE_SIDED
    };

    /** Wireframe edge-selection bits, indexed by edge number. */
    private static final int[] WIRE_EDGES = {
        Paint3DContext.WIRE_EDGE0, Paint3DContext.WIRE_EDGE1, Paint3DContext.WIRE_EDGE2
    };

    /** The default {@code yRange} of a height field: effectively unclamped. */
    private static final float HEIGHT_FIELD_Y_LIMIT = 1e6f;

    /** The default grid line color of an {@code infinitePlane}: opaque mid grey. */
    private static final int INFINITE_PLANE_LINE_COLOR = 0xFF808080;

    static {
        PRIMITIVES.put("sphere", Primitive3D.SPHERE);
        PRIMITIVES.put("cylinder", Primitive3D.CYLINDER);
        PRIMITIVES.put("cone", Primitive3D.CONE);
        PRIMITIVES.put("cube", Primitive3D.CUBE);
        PRIMITIVES.put("roundedcube", Primitive3D.ROUNDED_CUBE);
        PRIMITIVES.put("sphericalsector", Primitive3D.SPHERICAL_SECTOR);
        PRIMITIVES.put("sphericaldome", Primitive3D.SPHERICAL_DOME);
        PRIMITIVES.put("tube", Primitive3D.TUBE);
        PRIMITIVES.put("captube", Primitive3D.CAP_TUBE);
        PRIMITIVES.put("profiletube", Primitive3D.PROFILE_TUBE);
        PRIMITIVES.put("torus", Primitive3D.TORUS);
        PRIMITIVES.put("plane", Primitive3D.PLANE);
        PRIMITIVES.put("extrudecircle", Primitive3D.EXTRUDE_CIRCLE);
        PRIMITIVES.put("extrudesector", Primitive3D.EXTRUDE_SECTOR);
        PRIMITIVES.put("extrudesegment", Primitive3D.EXTRUDE_SEGMENT);
        PRIMITIVES.put("extrudearc", Primitive3D.EXTRUDE_ARC);
        PRIMITIVES.put("extruderoundedrect", Primitive3D.EXTRUDE_ROUNDED_RECT);
        PRIMITIVES.put("extrudesquircle", Primitive3D.EXTRUDE_SQUIRCLE);
        PRIMITIVES.put("extrudepath", Primitive3D.EXTRUDE_PATH);
        PRIMITIVES.put("lathe", Primitive3D.LATHE);
        PRIMITIVES.put("sweep", Primitive3D.SWEEP);
        PRIMITIVES.put("helix", Primitive3D.HELIX);
        PRIMITIVES.put("icosphere", Primitive3D.ICOSPHERE);
        PRIMITIVES.put("infiniteplane", Primitive3D.INFINITE_PLANE);

        PRIMITIVE_PARAMS.put("sphere", new String[] {"radius", "center"});
        PRIMITIVE_PARAMS.put("cylinder", new String[] {"radius", "from", "to"});
        PRIMITIVE_PARAMS.put("cone", new String[] {"radius", "height", "center"});
        PRIMITIVE_PARAMS.put("cube", new String[] {"dimX", "dimY", "dimZ", "center"});
        PRIMITIVE_PARAMS.put(
                "roundedcube", new String[] {"dimX", "dimY", "dimZ", "center", "cornerRadius"});
        PRIMITIVE_PARAMS.put("sphericalsector", new String[] {"radius", "angle", "center"});
        PRIMITIVE_PARAMS.put("sphericaldome", new String[] {"radius", "angle", "center"});
        PRIMITIVE_PARAMS.put("torus", new String[] {"majorRadius", "minorRadius", "center"});
        PRIMITIVE_PARAMS.put("plane", new String[] {"width", "height", "center"});
        PRIMITIVE_PARAMS.put("extrudecircle", new String[] {"radius", "depth", "center"});
        PRIMITIVE_PARAMS.put(
                "extrudesector",
                new String[] {"radius", "startAngle", "sweepAngle", "depth", "center"});
        PRIMITIVE_PARAMS.put(
                "extrudesegment",
                new String[] {"radius", "startAngle", "sweepAngle", "depth", "center"});
        PRIMITIVE_PARAMS.put(
                "extrudearc",
                new String[] {
                    "innerRadius", "outerRadius", "startAngle", "sweepAngle", "depth", "center"
                });
        PRIMITIVE_PARAMS.put(
                "extruderoundedrect",
                new String[] {"width", "height", "cornerRadius", "depth", "center"});
        PRIMITIVE_PARAMS.put(
                "extrudesquircle", new String[] {"radius", "exponent", "depth", "center"});
        PRIMITIVE_PARAMS.put("icosphere", new String[] {"radius", "center"});
        PRIMITIVE_PARAMS.put("lathe", new String[] {"center"});
        PRIMITIVE_PARAMS.put(
                "helix", new String[] {"coilRadius", "tubeRadius", "pitch", "turns", "center"});

        UV_MODES.put("none", Primitive3D.UV_NONE);
        UV_MODES.put("uv", Primitive3D.UV);
        UV_MODES.put("uv2", Primitive3D.UV2);
        UV_MODES.put("uv3", Primitive3D.UV3);

        AXES.put("native", Primitive3D.AXIS_NATIVE);
        AXES.put("x", Primitive3D.AXIS_X);
        AXES.put("y", Primitive3D.AXIS_Y);
        AXES.put("z", Primitive3D.AXIS_Z);

        SURFACES.put("general", MeshExpression3D.SURFACE_GENERAL);
        SURFACES.put("heightfield", MeshExpression3D.SURFACE_HEIGHT_FIELD);
        SURFACES.put("sphere", MeshExpression3D.SURFACE_SPHERE);
        SURFACES.put("cylinder", MeshExpression3D.SURFACE_CYLINDER);

        PROJECTIONS.put("perspective", Paint3DContext.PROJECTION_PERSPECTIVE);
        PROJECTIONS.put("ortho", Paint3DContext.PROJECTION_ORTHO);
        PROJECTIONS.put("orthographic", Paint3DContext.PROJECTION_ORTHO);

        MATRIX_OPS.put("identity", Paint3DContext.M3_IDENTITY);
        MATRIX_OPS.put("translate", Paint3DContext.M3_TRANSLATE);
        MATRIX_OPS.put("scale", Paint3DContext.M3_SCALE);
        MATRIX_OPS.put("rotate", Paint3DContext.M3_ROTATE_AXIS);
        MATRIX_OPS.put("multiply", Paint3DContext.M3_MULTIPLY);
        MATRIX_OPS.put("push", Paint3DContext.M3_PUSH);
        MATRIX_OPS.put("pop", Paint3DContext.M3_POP);

        LIGHT_TYPES.put("directional", Paint3DContext.LIGHT_DIRECTIONAL);
        LIGHT_TYPES.put("dir", Paint3DContext.LIGHT_DIRECTIONAL);
        LIGHT_TYPES.put("point", Paint3DContext.LIGHT_POINT);
        LIGHT_TYPES.put("ambient", Paint3DContext.LIGHT_AMBIENT);

        MESH_MODES.put("software-flat", Paint3DContext.MODE_SOFTWARE_FLAT);
        MESH_MODES.put("software-smooth", Paint3DContext.MODE_SOFTWARE_SMOOTH);
        MESH_MODES.put("canvas-flat", Paint3DContext.MODE_CANVAS_FLAT);
        MESH_MODES.put("canvas-smooth", Paint3DContext.MODE_CANVAS_SMOOTH);
        MESH_MODES.put("drawmesh-flat", Paint3DContext.MODE_DRAWMESH_FLAT);
        MESH_MODES.put("drawmesh-smooth", Paint3DContext.MODE_DRAWMESH_SMOOTH);
        // Reserved backends without a named constant: GL (3) and the canvas Z-buffer (5).
        MESH_MODES.put("gl-flat", 6);
        MESH_MODES.put("gl-smooth", 7);
        MESH_MODES.put("drawmesh-zbuf-flat", Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT);
        MESH_MODES.put("drawmesh-zbuf-smooth", Paint3DContext.MODE_DRAWMESH_ZBUF_SMOOTH);
        MESH_MODES.put("canvas-zbuf-flat", 10);
        MESH_MODES.put("canvas-zbuf-smooth", 11);
        MESH_MODES.put("flat", Paint3DContext.MODE_SOFTWARE_FLAT);
        MESH_MODES.put("smooth", Paint3DContext.MODE_SOFTWARE_SMOOTH);
    }

    private final RemoteComposeJsonParser mParser;
    private final RemoteComposeWriter3D mWriter;

    /**
     * Construct a Command3DParser instance.
     *
     * @param parser the parent JSON parser instance
     */
    Command3DParser(@NonNull RemoteComposeJsonParser parser) {
        mParser = parser;
        mWriter = parser.getWriter().getRc3DWriter();
    }

    /**
     * Write one 3D command.
     *
     * @param type the lower-case command type
     * @param command the normalized command object
     * @return false if {@code type} is not a 3D command, in which case nothing is written
     * @throws JSONException if the command is malformed
     */
    boolean parse(@NonNull String type, @NonNull JSONObject command) throws JSONException {
        switch (type) {
            case "cleardepth3d":
                mWriter.clearDepth3D();
                return true;
            case "material3d":
                mWriter.setMaterial3D(
                        floatOr(command, "specular", 0f), floatOr(command, "shininess", 32f));
                return true;
            case "depthbias3d":
                mWriter.setDepthBias3D(
                        floatOr(command, "constant", 0f), floatOr(command, "slope", 0f));
                return true;
            case "camera3d":
                parseCamera(command);
                return true;
            case "lights3d":
                parseLights(command);
                return true;
            case "matrix3d":
                parseMatrix(command);
                return true;
            case "definemesh3d":
                parseDefineMesh(command);
                return true;
            case "meshprimitive3d":
                parseMeshPrimitive(command);
                return true;
            case "meshexpression3d":
                parseMeshExpression(command);
                return true;
            case "texture3d":
                // 0 (or no bitmap) clears the texture; a name resolves against resources.bitmaps.
                mWriter.setTexture3D(bitmapId(command.opt("bitmap")));
                return true;
            case "drawmesh3d":
                mWriter.drawMesh3D(
                        RemoteComposeJsonParser.toInt(command.get("mesh")), meshMode(command));
                return true;
            default:
                return false;
        }
    }

    /**
     * {@code camera3D}: projection plus a look-at view. Perspective takes {@code fovY} (radians),
     * {@code aspect}, {@code near} and {@code far}; ortho requires {@code left}, {@code right},
     * {@code bottom} and {@code top}. The view is {@code eye}, {@code center} and {@code up}.
     */
    private void parseCamera(@NonNull JSONObject command) throws JSONException {
        String name = lowerName(command.opt("projection"), "perspective");
        Integer projection = PROJECTIONS.get(name);
        if (projection == null) {
            throw new JSONException("camera3D projection '" + name + "'");
        }
        float[] projParams;
        if (projection == Paint3DContext.PROJECTION_PERSPECTIVE) {
            projParams =
                    new float[] {
                        floatOr(command, "fovY", 0.9f),
                        floatOr(command, "aspect", 1f),
                        floatOr(command, "near", 0.1f),
                        floatOr(command, "far", 100f)
                    };
        } else {
            projParams =
                    new float[] {
                        mParser.parseFloat(command.get("left")),
                        mParser.parseFloat(command.get("right")),
                        mParser.parseFloat(command.get("bottom")),
                        mParser.parseFloat(command.get("top")),
                        floatOr(command, "near", 0.1f),
                        floatOr(command, "far", 100f)
                    };
        }
        Floats view = new Floats();
        addAll(view, arrayOr(command, "eye", 0, 0, 4));
        addAll(view, arrayOr(command, "center", 0, 0, 0));
        addAll(view, arrayOr(command, "up", 0, 1, 0));
        if (view.size() != 9) {
            throw new JSONException("camera3D needs eye/center/up of 3 components each");
        }
        mWriter.setCamera3D(projection, projParams, view.toArray());
    }

    /**
     * {@code lights3D}: the whole light set. Each light is {@code directional} (with {@code dir}),
     * {@code point} (with {@code pos}) or {@code ambient} (neither: it lights every
     * surface equally), plus a {@code color} and an {@code intensity}. No lights means the default
     * ambient only.
     */
    private void parseLights(@NonNull JSONObject command) throws JSONException {
        JSONArray lights = command.has("lights") ? command.getJSONArray("lights") : new JSONArray();
        int count = lights.length();
        int[] types = new int[count];
        int[] colors = new int[count];
        Floats params = new Floats();
        for (int i = 0; i < count; i++) {
            JSONObject light = lights.getJSONObject(i);
            String name = lowerName(light.opt("type"), "directional");
            Integer type = LIGHT_TYPES.get(name);
            if (type == null) {
                throw new JSONException(
                        "lights3D type '"
                                + name
                                + "' (expected one of "
                                + new TreeSet<>(LIGHT_TYPES.keySet())
                                + ")");
            }
            types[i] = type;
            colors[i] = mParser.parseColor(light.has("color") ? light.get("color") : "#FFFFFFFF");
            if (type == Paint3DContext.LIGHT_AMBIENT) {
                // A direction or position would be silently ignored, so it is more likely a
                // light given the wrong type than an ambient light.
                if (light.has("dir") || light.has("pos")) {
                    throw new JSONException(
                            "lights3D ambient takes only color and intensity, not "
                                    + (light.has("dir") ? "dir" : "pos"));
                }
                params.add(0f);
                params.add(0f);
                params.add(0f);
            } else {
                String key = type == Paint3DContext.LIGHT_POINT ? "pos" : "dir";
                JSONArray vector = light.optJSONArray(key);
                if (vector == null || vector.length() != 3) {
                    throw new JSONException("lights3D " + name + " needs a 3-component " + key);
                }
                addAll(params, vector);
            }
            params.add(floatOr(light, "intensity", 1f));
        }
        mWriter.setLights3D(types, colors, params.toArray());
    }

    /**
     * {@code matrix3D}: post-multiply the model-view by {@code identity}, {@code translate} or
     * {@code scale} ({@code x}, {@code y}, {@code z}), {@code rotate} ({@code angle} in radians
     * about {@code axis}) or {@code multiply} (16 column-major floats in {@code m}); or
     * save the model matrix with {@code push} and restore it with {@code pop}.
     *
     * <p>A command with other keys but no {@code op} is an error rather than an identity: {@code
     * {"translate": [x, y, z]}} used to draw every instance at the origin, which reads as a camera
     * or data bug instead of a typo.
     */
    private void parseMatrix(@NonNull JSONObject command) throws JSONException {
        if (!command.has("op")) {
            TreeSet<String> extra = new TreeSet<>();
            Iterator<String> keys = command.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!key.equals("type")) {
                    extra.add(key);
                }
            }
            if (!extra.isEmpty()) {
                throw new JSONException(
                        "matrix3D needs an \"op\""
                                + " (identity/translate/scale/rotate/multiply/push/pop);"
                                + " got keys "
                                + extra);
            }
        }
        String name = lowerName(command.opt("op"), "identity");
        Integer sub = MATRIX_OPS.get(name);
        if (sub == null) {
            throw new JSONException(
                    "matrix3D op '"
                            + name
                            + "' (expected one of "
                            + new TreeSet<>(MATRIX_OPS.keySet())
                            + ")");
        }
        float[] args;
        switch (sub) {
            case Paint3DContext.M3_IDENTITY:
            case Paint3DContext.M3_PUSH:
            case Paint3DContext.M3_POP:
                args = new float[0];
                break;
            case Paint3DContext.M3_TRANSLATE:
            case Paint3DContext.M3_SCALE:
                args =
                        new float[] {
                            floatOr(command, "x", 0f),
                            floatOr(command, "y", 0f),
                            floatOr(command, "z", 0f)
                        };
                break;
            case Paint3DContext.M3_ROTATE_AXIS: {
                Floats rotate = new Floats();
                rotate.add(mParser.parseFloat(command.get("angle")));
                addAll(rotate, arrayOr(command, "axis", 0, 1, 0));
                if (rotate.size() != 4) {
                    throw new JSONException("matrix3D rotate axis needs 3 components");
                }
                args = rotate.toArray();
                break;
            }
            default: {
                JSONArray m = command.getJSONArray("m");
                if (m.length() != 16) {
                    throw new JSONException(
                            "matrix3D multiply needs 16 floats, got " + m.length());
                }
                args = floats(m);
                break;
            }
        }
        mWriter.matrix3DOp(sub, args);
    }

    /**
     * {@code defineMesh3D}: an explicit triangle mesh. {@code verts} and the optional {@code
     * normals} are x,y,z triples, the optional {@code uv} is one u,v pair per vertex, and {@code
     * indices} lists triangles. An empty {@code normals} or {@code uv} counts as absent.
     */
    private void parseDefineMesh(@NonNull JSONObject command) throws JSONException {
        float[] verts = floats(command.getJSONArray("verts"));
        float[] normals = optFloats(command, "normals");
        float[] uv = optFloats(command, "uv");
        JSONArray indexArray = command.getJSONArray("indices");
        int[] indices = new int[indexArray.length()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = RemoteComposeJsonParser.toInt(indexArray.get(i));
        }
        if (indices.length % 3 != 0) {
            throw new JSONException(
                    "defineMesh3D indices length " + indices.length + " is not a multiple of 3");
        }
        if (verts.length % 3 != 0) {
            throw new JSONException(
                    "defineMesh3D verts length " + verts.length + " is not a multiple of 3");
        }
        if (normals != null && normals.length != verts.length) {
            throw new JSONException(
                    "defineMesh3D normals " + normals.length + " != verts " + verts.length);
        }
        int uvLength = verts.length / 3 * 2;
        if (uv != null && uv.length != uvLength) {
            throw new JSONException(
                    "defineMesh3D uv " + uv.length + " != 2 per vertex (" + uvLength + ")");
        }
        mWriter.defineMesh3D(
                RemoteComposeJsonParser.toInt(command.get("id")), indices, verts, normals, uv);
    }

    /**
     * {@code meshPrimitive3D}: a mesh generated on the player from a parametric primitive.
     *
     * <p>The scalar parameters come from a positional {@code params} list when present, otherwise
     * from named keys ({@link #PRIMITIVE_PARAMS}), with special forms for the tubes, {@code sweep},
     * {@code extrudePath} and {@code infinitePlane}. Geometry streams ride extra channels: a
     * sweep's section and path, a lathe's {@code profile}, and any explicit {@code channels}.
     *
     * <p>The flags are the raw {@code flags} plus the named options: {@code uv}, {@code axis}
     * (any primitive) and {@code curvatureDensity} (tubes only, in either parameter form).
     */
    private void parseMeshPrimitive(@NonNull JSONObject command) throws JSONException {
        Object primitive = command.get("primitive");
        String kind =
                String.valueOf(primitive)
                        .toLowerCase(Locale.ROOT)
                        .replace("_", "")
                        .replace("-", "");
        Integer type = PRIMITIVES.get(kind);
        if (type == null) {
            throw new JSONException(
                    "meshPrimitive3D primitive "
                            + quote(primitive)
                            + " (expected one of "
                            + new TreeSet<>(PRIMITIVES.keySet())
                            + ")");
        }
        int extraFlags = 0;
        Floats scalars = new Floats();
        List<float[]> extraChannels = new ArrayList<>();
        if (command.has("params")) {
            addAll(scalars, command.getJSONArray("params"));
        } else if (kind.equals("tube") || kind.equals("captube")) {
            // [radius, (ringsPerSpan), x0,y0,z0, ...]
            scalars.add(mParser.parseFloat(command.get("radius")));
            if (command.has("ringsPerSpan")) {
                extraFlags |= Primitive3D.FLAG_TUBE_PATH_DENSITY;
                scalars.add(mParser.parseFloat(command.get("ringsPerSpan")));
            }
            addAll(scalars, points(command, "points", 3));
            if (RemoteComposeJsonParser.isTruthy(command.opt("legacy"))) {
                extraFlags |= Primitive3D.FLAG_TUBE_LEGACY;
            }
            if (RemoteComposeJsonParser.isTruthy(command.opt("spline"))) {
                extraFlags |= Primitive3D.FLAG_SPLINE;
            }
        } else if (kind.equals("profiletube")) {
            // [nPoints, (ringsPerSpan), x,y,z * n, r0..rk]
            List<Object> points = points(command, "points", 3);
            JSONArray radii = command.optJSONArray("radii");
            if (radii == null || radii.length() == 0) {
                throw new JSONException("meshPrimitive3D profileTube: missing 'radii'");
            }
            scalars.add(points.size() / 3);
            if (command.has("ringsPerSpan")) {
                extraFlags |= Primitive3D.FLAG_TUBE_PATH_DENSITY;
                scalars.add(mParser.parseFloat(command.get("ringsPerSpan")));
            }
            addAll(scalars, points);
            addAll(scalars, radii);
            if (truthyOr(command, "capped", true)) {
                extraFlags |= Primitive3D.FLAG_TUBE_CAP;
            }
        } else if (kind.equals("sweep")) {
            // [closed, capStart, capEnd, cx,cy,cz]; the section and path ride channels 1 and 2,
            // with optional per-station scales and twists after them.
            List<Object> center = arrayOr(command, "center", 0, 0, 0);
            if (center.size() != 3) {
                throw new JSONException("meshPrimitive3D sweep: center needs 3 values");
            }
            scalars.add(truthyOr(command, "closed", true) ? 1f : 0f);
            scalars.add(truthyOr(command, "capStart", true) ? 1f : 0f);
            scalars.add(truthyOr(command, "capEnd", true) ? 1f : 0f);
            addAll(scalars, center);
            extraChannels.add(floats(points(command, "section", 2)));
            extraChannels.add(floats(points(command, "path", 3)));
            if (command.has("scales")) {
                extraChannels.add(floats(command.getJSONArray("scales")));
            }
            if (command.has("twists")) {
                if (!command.has("scales")) {
                    // Channels are positional: twists live in channel 4, so a document with
                    // twists but no scales must still send a scale channel.
                    throw new JSONException(
                            "meshPrimitive3D sweep: 'twists' requires 'scales'"
                                    + " (channel order is positional)");
                }
                extraChannels.add(floats(command.getJSONArray("twists")));
            }
        } else if (kind.equals("extrudepath")) {
            // [depth, cx,cy,cz, bevel, contourCount, (count, x,y...)*]
            List<Object> center = arrayOr(command, "center", 0, 0, 0);
            if (center.size() != 3) {
                throw new JSONException("meshPrimitive3D extrudePath: center needs 3 values");
            }
            JSONArray contours = command.optJSONArray("contours");
            if (contours == null || contours.length() == 0) {
                throw new JSONException("meshPrimitive3D extrudePath: missing 'contours'");
            }
            scalars.add(floatOr(command, "depth", 1f));
            addAll(scalars, center);
            scalars.add(floatOr(command, "bevel", 0f));
            scalars.add(contours.length());
            for (int i = 0; i < contours.length(); i++) {
                JSONArray ring = contours.getJSONArray(i);
                scalars.add(ring.length());
                for (int j = 0; j < ring.length(); j++) {
                    JSONArray point = ring.getJSONArray(j);
                    if (point.length() != 2) {
                        throw new JSONException(
                                "meshPrimitive3D extrudePath: contour points are [x, y] pairs");
                    }
                    addAll(scalars, point);
                }
            }
        } else if (kind.equals("infiniteplane")) {
            // [cx,cy,cz, spacing, lineWidth, lineA,lineR,lineG,lineB]
            List<Object> center = arrayOr(command, "center", 0, 0, 0);
            if (center.size() != 3) {
                throw new JSONException("meshPrimitive3D infinitePlane: center needs 3 components");
            }
            addAll(scalars, center);
            scalars.add(floatOr(command, "spacing", 0f));
            scalars.add(floatOr(command, "lineWidth", 1f));
            addLineColor(scalars, command);
        } else if (PRIMITIVE_PARAMS.containsKey(kind)) {
            String[] keys = PRIMITIVE_PARAMS.get(kind);
            for (String key : keys) {
                if (key.equals("center") || key.equals("from") || key.equals("to")) {
                    List<Object> vector = arrayOr(command, key, 0, 0, 0);
                    if (vector.size() != 3) {
                        throw new JSONException(
                                "meshPrimitive3D " + kind + ": " + key + " needs 3 components");
                    }
                    addAll(scalars, vector);
                } else {
                    if (!command.has(key)) {
                        throw new JSONException(
                                "meshPrimitive3D "
                                        + kind
                                        + ": missing '"
                                        + key
                                        + "' (needs "
                                        + Arrays.toString(keys)
                                        + ")");
                    }
                    scalars.add(mParser.parseFloat(command.get(key)));
                }
            }
        } else {
            throw new JSONException(
                    "meshPrimitive3D " + kind + ": pass an explicit \"params\" list");
        }
        if (RemoteComposeJsonParser.isTruthy(command.opt("curvatureDensity"))) {
            // The sampling bits are per type: on another primitive bit 8 is free for another use.
            if (!kind.equals("tube") && !kind.equals("captube") && !kind.equals("profiletube")) {
                throw new JSONException(
                        "meshPrimitive3D "
                                + kind
                                + ": 'curvatureDensity' applies only to tube, capTube and"
                                + " profileTube");
            }
            extraFlags |= Primitive3D.FLAG_TUBE_CURVATURE_DENSITY;
        }
        int flags = intOr(command, "flags", 0) | extraFlags;
        String uvName = lowerName(command.opt("uv"), "none");
        Integer uvMode = UV_MODES.get(uvName);
        if (uvMode == null) {
            throw new JSONException("meshPrimitive3D uv '" + uvName + "'");
        }
        flags |= uvMode << Primitive3D.FLAG_UV_SHIFT;
        flags |= axis(command.opt("axis")) << Primitive3D.FLAG_AXIS_SHIFT;
        List<float[]> channels = new ArrayList<>();
        channels.add(scalars.toArray());
        channels.addAll(extraChannels);
        if (command.has("channels")) {
            JSONArray extra = command.getJSONArray("channels");
            for (int i = 0; i < extra.length(); i++) {
                channels.add(floats(extra.getJSONArray(i)));
            }
        }
        if (kind.equals("lathe") && command.has("profile")) {
            JSONArray profile = command.getJSONArray("profile");
            Floats points = new Floats();
            for (int i = 0; i < profile.length(); i++) {
                JSONArray point = profile.getJSONArray(i);
                if (point.length() != 2) {
                    throw new JSONException(
                            "meshPrimitive3D lathe: profile points are [radius, y] pairs");
                }
                addAll(points, point);
            }
            channels.add(points.toArray());
        }
        int id = RemoteComposeJsonParser.toInt(command.get("id"));
        float segments = floatOr(command, "segments", 0f);
        mWriter.meshPrimitive(id, type, segments, flags, channels.toArray(new float[0][]));
    }

    /**
     * {@code meshExpression3D}: a mesh the player evaluates from expressions over a (u, v) grid.
     *
     * <p>{@code heightField} takes {@code xRange}, {@code uCount}, {@code zRange}, {@code vCount},
     * an optional {@code yRange} and a {@code height} expression; {@code sphere} and {@code
     * cylinder} take their dimensions, grid counts and an optional {@code displacement}; {@code
     * general} takes {@code uRange}, {@code uCount}, {@code vRange}, {@code vCount} and {@code x},
     * {@code y}, {@code z} expressions. Optional {@code normal} (three expressions) and {@code uv}
     * (two) replace the generated ones. Inside these expressions {@code u} and {@code v} are the
     * grid coordinates. {@code flipWinding} reverses the winding, and with it the generated
     * normals; {@code flipNormals} reverses the normals only.
     */
    private void parseMeshExpression(@NonNull JSONObject command) throws JSONException {
        String surface = lowerName(command.opt("surface"), "general").replace("_", "");
        Integer type = SURFACES.get(surface);
        if (type == null) {
            throw new JSONException(
                    "meshExpression3D surface "
                            + quote(command.opt("surface"))
                            + " (expected one of "
                            + new TreeSet<>(SURFACES.keySet())
                            + ")");
        }
        Floats params = new Floats();
        float[][] pos;
        switch (type) {
            case MeshExpression3D.SURFACE_HEIGHT_FIELD:
                addRange(params, command, surface, "xRange", false);
                params.add(mParser.parseFloat(need(command, surface, "uCount")));
                addRange(params, command, surface, "zRange", false);
                params.add(mParser.parseFloat(need(command, surface, "vCount")));
                addRange(params, command, surface, "yRange", true);
                pos = new float[][] {expression(need(command, surface, "height"))};
                break;
            case MeshExpression3D.SURFACE_SPHERE:
                params.add(mParser.parseFloat(need(command, surface, "radius")));
                params.add(mParser.parseFloat(need(command, surface, "uCount")));
                params.add(mParser.parseFloat(need(command, surface, "vCount")));
                pos = new float[][] {expression(displacement(command))};
                break;
            case MeshExpression3D.SURFACE_CYLINDER:
                params.add(mParser.parseFloat(need(command, surface, "radius")));
                params.add(mParser.parseFloat(need(command, surface, "height")));
                params.add(mParser.parseFloat(need(command, surface, "uCount")));
                params.add(mParser.parseFloat(need(command, surface, "vCount")));
                pos = new float[][] {expression(displacement(command))};
                break;
            default:
                addRange(params, command, surface, "uRange", false);
                params.add(mParser.parseFloat(need(command, surface, "uCount")));
                addRange(params, command, surface, "vRange", false);
                params.add(mParser.parseFloat(need(command, surface, "vCount")));
                pos =
                        new float[][] {
                            expression(need(command, surface, "x")),
                            expression(need(command, surface, "y")),
                            expression(need(command, surface, "z"))
                        };
                break;
        }
        float[][] normal = expressions(command, "normal");
        if (normal.length != 0 && normal.length != 3) {
            throw new JSONException(
                    "meshExpression3D: 'normal' needs three expressions (nx, ny, nz);"
                            + " omit it for finite-difference normals");
        }
        float[][] uv = expressions(command, "uv");
        if (uv.length != 0 && uv.length != 2) {
            throw new JSONException("meshExpression3D: 'uv' needs two expressions (u, v)");
        }
        int flags = intOr(command, "flags", 0);
        if (RemoteComposeJsonParser.isTruthy(command.opt("flipWinding"))) {
            flags |= MeshExpression3D.FLAG_FLIP_WINDING;
        }
        if (RemoteComposeJsonParser.isTruthy(command.opt("flipNormals"))) {
            flags |= MeshExpression3D.FLAG_FLIP_NORMALS;
        }
        mWriter.meshExpression(
                RemoteComposeJsonParser.toInt(command.get("id")),
                type,
                flags,
                params.toArray(),
                pos,
                normal,
                uv);
    }

    /** A sphere or cylinder surface's radial displacement expression; none is 0. */
    private static @NonNull Object displacement(@NonNull JSONObject command)
            throws JSONException {
        return command.has("displacement") ? command.get("displacement") : 0;
    }

    /** The value of a key a surface family requires. */
    private static @NonNull Object need(
            @NonNull JSONObject command, @NonNull String surface, @NonNull String key)
            throws JSONException {
        if (!command.has(key)) {
            throw new JSONException("meshExpression3D " + surface + ": missing '" + key + "'");
        }
        return command.get(key);
    }

    /** Append a {@code [min, max]} range; the optional one defaults to the height field limit. */
    private void addRange(
            @NonNull Floats out,
            @NonNull JSONObject command,
            @NonNull String surface,
            @NonNull String key,
            boolean optional)
            throws JSONException {
        if (optional && !command.has(key)) {
            out.add(-HEIGHT_FIELD_Y_LIMIT);
            out.add(HEIGHT_FIELD_Y_LIMIT);
            return;
        }
        JSONArray range = command.optJSONArray(key);
        if (range == null || range.length() != 2) {
            throw new JSONException(
                    "meshExpression3D " + surface + ": '" + key + "' must be [min, max]");
        }
        addAll(out, range);
    }

    /** Compile the optional list of expressions under {@code key}; absent means none. */
    private float @NonNull [] @NonNull [] expressions(
            @NonNull JSONObject command, @NonNull String key) throws JSONException {
        if (!command.has(key)) {
            return new float[0][];
        }
        JSONArray list = command.getJSONArray(key);
        float[][] result = new float[list.length()][];
        for (int i = 0; i < result.length; i++) {
            result[i] = expression(list.get(i));
        }
        return result;
    }

    /**
     * Compile one mesh expression with {@code u} and {@code v} bound to the grid coordinates.
     *
     * <p>The binding lasts only for this expression so the names cannot shadow a document's own
     * variables anywhere else. A plain number is a one-element expression.
     */
    private float @NonNull [] expression(@NonNull Object value) throws JSONException {
        if (value instanceof Number) {
            return new float[] {mParser.parseFloat(value)};
        }
        Map<String, Float> variables = mParser.mVariables;
        boolean hadU = variables.containsKey("u");
        boolean hadV = variables.containsKey("v");
        Float savedU = variables.get("u");
        Float savedV = variables.get("v");
        variables.put("u", AnimatedFloatExpression.VAR1);
        variables.put("v", AnimatedFloatExpression.VAR2);
        try {
            return mParser.parseFloatExpression(String.valueOf(value));
        } finally {
            restore(variables, "u", hadU, savedU);
            restore(variables, "v", hadV, savedV);
        }
    }

    private static void restore(
            @NonNull Map<String, Float> variables,
            @NonNull String name,
            boolean had,
            @Nullable Float saved) {
        if (had) {
            variables.put(name, saved);
        } else {
            variables.remove(name);
        }
    }

    /**
     * Resolve a texture reference: absent, null or 0 clears the texture, a number is an image id,
     * and a string ({@code "@name"}, {@code "$name"} or a bare name) is a {@code resources.bitmaps}
     * entry.
     */
    private int bitmapId(@Nullable Object value) throws JSONException {
        if (value == null || value == JSONObject.NULL) {
            return 0;
        }
        if (value instanceof String) {
            String reference = (String) value;
            String name = reference;
            if (reference.startsWith("$vars.") || reference.startsWith("@vars.")) {
                name = reference.substring(6);
            } else if (reference.startsWith("$") || reference.startsWith("@")) {
                name = reference.substring(1);
            }
            Integer id = mParser.mBitmapIds.get(name);
            if (id != null) {
                return id;
            }
            TreeSet<String> known = new TreeSet<>(mParser.mBitmapIds.keySet());
            throw new JSONException(
                    "bitmap '"
                            + reference
                            + "' not found (declared: "
                            + (known.isEmpty() ? "none declared" : String.join(", ", known))
                            + ")");
        }
        return RemoteComposeJsonParser.toInt(value);
    }

    /**
     * The packed {@code drawMesh3D} mode: a name from {@link #MESH_MODES} or a raw number, plus
     * the wireframe flag and, for a wireframe, the {@code edges} (0, 1 or 2) to draw, plus the
     * flags {@code transparent}, {@code gpu}, {@code pbr}, {@code doubleSided} and {@code
     * frontFace}.
     */
    private static int meshMode(@NonNull JSONObject command) throws JSONException {
        Object raw = command.has("mode") ? command.get("mode") : "software-flat";
        int mode;
        if (raw instanceof Number) {
            mode = ((Number) raw).intValue();
        } else {
            Integer named = MESH_MODES.get(String.valueOf(raw).toLowerCase(Locale.ROOT));
            if (named == null) {
                throw new JSONException(
                        "drawMesh3D mode "
                                + quote(raw)
                                + " (expected one of "
                                + new TreeSet<>(MESH_MODES.keySet())
                                + ")");
            }
            mode = named;
        }
        if (RemoteComposeJsonParser.isTruthy(command.opt("wireframe"))) {
            mode |= Paint3DContext.MODE_WIREFRAME;
            if (command.has("edges")) {
                JSONArray edges = command.getJSONArray("edges");
                for (int i = 0; i < edges.length(); i++) {
                    int edge = RemoteComposeJsonParser.toInt(edges.get(i));
                    if (edge < 0 || edge > 2) {
                        throw new JSONException(
                                "drawMesh3D edge " + quote(edges.get(i))
                                        + " (expected 0, 1 or 2)");
                    }
                    mode |= WIRE_EDGES[edge];
                }
            }
        }
        for (int i = 0; i < MODE_FLAG_KEYS.length; i++) {
            if (RemoteComposeJsonParser.isTruthy(command.opt(MODE_FLAG_KEYS[i]))) {
                mode |= MODE_FLAGS[i];
            }
        }
        return mode | frontFace(command.opt("frontFace"));
    }

    /**
     * The {@code drawMesh3D} {@code frontFace}: {@code "ccw"}, the default, or {@code "cw"}
     * ({@link Paint3DContext#MODE_FRONT_CW}: clockwise triangles are front-facing), in any case.
     */
    private static int frontFace(@Nullable Object value) throws JSONException {
        String name = value == null ? "ccw" : String.valueOf(value).toLowerCase(Locale.ROOT);
        switch (name) {
            case "ccw":
                return 0;
            case "cw":
                return Paint3DContext.MODE_FRONT_CW;
            default:
                throw new JSONException(
                        "drawMesh3D frontFace " + quote(value) + " (expected \"ccw\" or \"cw\")");
        }
    }

    /**
     * A primitive's {@code axis}: {@code "native"}, {@code "x"}, {@code "y"} or {@code "z"}
     * in any case, or the number 0 to 3. Absent is native.
     */
    private static int axis(@Nullable Object value) throws JSONException {
        if (value == null) {
            return Primitive3D.AXIS_NATIVE;
        }
        Integer axis;
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            axis = number == Math.rint(number) && number >= 0 && number <= 3 ? (int) number : null;
        } else {
            axis = AXES.get(String.valueOf(value).toLowerCase(Locale.ROOT));
        }
        if (axis == null) {
            throw new JSONException(
                    "meshPrimitive3D axis "
                            + quote(value)
                            + " (expected one of "
                            + new TreeSet<>(AXES.keySet())
                            + " or 0-3)");
        }
        return axis;
    }

    /**
     * Append an {@code infinitePlane}'s {@code lineColor} as {@code a, r, g, b} floats in 0..1.
     *
     * <p>The color is a {@code "#RRGGBB"} or {@code "#AARRGGBB"} string, an ARGB integer, or an
     * {@code [a, r, g, b]} list whose entries may be variables or expressions; absent is {@link
     * #INFINITE_PLANE_LINE_COLOR}. Named and theme colors are not accepted because the color is
     * written as plain float params rather than a color id.
     */
    private void addLineColor(@NonNull Floats out, @NonNull JSONObject command)
            throws JSONException {
        if (!command.has("lineColor")) {
            addArgb(out, INFINITE_PLANE_LINE_COLOR);
            return;
        }
        Object value = command.get("lineColor");
        if (value instanceof JSONArray && ((JSONArray) value).length() == 4) {
            JSONArray argb = (JSONArray) value;
            for (int i = 0; i < 4; i++) {
                Object component = argb.get(i);
                if (component instanceof Number) {
                    double number = ((Number) component).doubleValue();
                    if (!(number >= 0 && number <= 1)) {
                        // Most likely 0..255 channels, which would all clamp to 1.
                        throw new JSONException(
                                "meshPrimitive3D infinitePlane: lineColor components are 0..1,"
                                        + " got "
                                        + component);
                    }
                }
            }
            addAll(out, argb);
            return;
        }
        if (value instanceof Integer || value instanceof Long) {
            long argb = ((Number) value).longValue();
            if (argb >= Integer.MIN_VALUE && argb <= 0xFFFFFFFFL) {
                addArgb(out, (int) argb);
                return;
            }
        }
        if (value instanceof String && isHexColor((String) value)) {
            String hex = ((String) value).substring(1);
            long argb = Long.parseLong(hex, 16);
            addArgb(out, (int) (hex.length() == 6 ? argb | 0xFF000000L : argb));
            return;
        }
        throw new JSONException(
                "meshPrimitive3D infinitePlane: lineColor "
                        + quote(value)
                        + " (expected \"#RRGGBB\", \"#AARRGGBB\", an ARGB integer or"
                        + " [a, r, g, b] floats in 0..1)");
    }

    /** Whether {@code text} is {@code #} followed by exactly 6 or 8 hex digits. */
    private static boolean isHexColor(@NonNull String text) {
        int digits = text.length() - 1;
        if (!text.startsWith("#") || (digits != 6 && digits != 8)) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Append {@code argb} as {@code a, r, g, b} floats in 0..1. */
    private static void addArgb(@NonNull Floats out, int argb) {
        out.add(((argb >>> 24) & 0xFF) / 255f);
        out.add(((argb >> 16) & 0xFF) / 255f);
        out.add(((argb >> 8) & 0xFF) / 255f);
        out.add((argb & 0xFF) / 255f);
    }

    /**
     * The points under {@code key}, each of {@code dim} components, flattened but not yet
     * parsed, so the caller can parse them in the order the reference converters do.
     */
    private static @NonNull List<Object> points(
            @NonNull JSONObject command, @NonNull String key, int dim) throws JSONException {
        JSONArray points = command.optJSONArray(key);
        if (points == null || points.length() == 0) {
            throw new JSONException("meshPrimitive3D: missing '" + key + "'");
        }
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < points.length(); i++) {
            JSONArray point = points.getJSONArray(i);
            if (point.length() != dim) {
                throw new JSONException(
                        "meshPrimitive3D: "
                                + key
                                + " entries need "
                                + dim
                                + " components, got "
                                + point.length());
            }
            for (int j = 0; j < dim; j++) {
                out.add(point.get(j));
            }
        }
        return out;
    }

    /** The float under {@code key}, or {@code defaultValue} when the key is absent. */
    private float floatOr(@NonNull JSONObject object, @NonNull String key, float defaultValue)
            throws JSONException {
        return object.has(key) ? mParser.parseFloat(object.get(key)) : defaultValue;
    }

    private static int intOr(@NonNull JSONObject object, @NonNull String key, int defaultValue)
            throws JSONException {
        return object.has(key) ? RemoteComposeJsonParser.toInt(object.get(key)) : defaultValue;
    }

    private static boolean truthyOr(
            @NonNull JSONObject object, @NonNull String key, boolean defaultValue) {
        return object.has(key) ? RemoteComposeJsonParser.isTruthy(object.opt(key)) : defaultValue;
    }

    /** The elements of the array under {@code key}, or {@code defaults} when it is absent. */
    private static @NonNull List<Object> arrayOr(
            @NonNull JSONObject object, @NonNull String key, float @NonNull ... defaults)
            throws JSONException {
        List<Object> values = new ArrayList<>();
        if (object.has(key)) {
            JSONArray array = object.getJSONArray(key);
            for (int i = 0; i < array.length(); i++) {
                values.add(array.get(i));
            }
        } else {
            for (float value : defaults) {
                values.add(value);
            }
        }
        return values;
    }

    /** {@code value} lower-cased, or {@code defaultName} when absent. */
    private static @NonNull String lowerName(@Nullable Object value, @NonNull String defaultName) {
        return value == null ? defaultName : String.valueOf(value).toLowerCase(Locale.ROOT);
    }

    private static @NonNull String quote(@Nullable Object value) {
        return value instanceof String ? "'" + value + "'" : String.valueOf(value);
    }

    private float @Nullable [] optFloats(@NonNull JSONObject object, @NonNull String key)
            throws JSONException {
        if (!object.has(key)) {
            return null;
        }
        float[] values = floats(object.getJSONArray(key));
        return values.length == 0 ? null : values;
    }

    private float @NonNull [] floats(@NonNull JSONArray array) throws JSONException {
        float[] values = new float[array.length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = mParser.parseFloat(array.get(i));
        }
        return values;
    }

    private float @NonNull [] floats(@NonNull List<Object> list) throws JSONException {
        float[] values = new float[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = mParser.parseFloat(list.get(i));
        }
        return values;
    }

    private void addAll(@NonNull Floats out, @NonNull JSONArray array) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            out.add(mParser.parseFloat(array.get(i)));
        }
    }

    private void addAll(@NonNull Floats out, @NonNull List<Object> list) throws JSONException {
        for (Object value : list) {
            out.add(mParser.parseFloat(value));
        }
    }

    /** A growable float array, so values can be appended without boxing NaN-encoded ids. */
    private static final class Floats {
        private float[] mValues = new float[16];
        private int mSize;

        void add(float value) {
            if (mSize == mValues.length) {
                mValues = Arrays.copyOf(mValues, mSize * 2);
            }
            mValues[mSize++] = value;
        }

        int size() {
            return mSize;
        }

        float @NonNull [] toArray() {
            return Arrays.copyOf(mValues, mSize);
        }
    }
}
