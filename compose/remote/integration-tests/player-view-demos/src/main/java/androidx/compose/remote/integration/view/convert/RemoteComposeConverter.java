/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.integration.view.convert;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.CompanionOperation;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lossless converter between RemoteCompose binary (.rc) and JSON.
 * Guaranteed bit-for-bit round-trip fidelity.
 */
@SuppressLint("RestrictedApiAndroidX")
public class RemoteComposeConverter {
    @SuppressLint("PrimitiveInCollection")
    private static final Map<String, Integer> sFloatRpnMap = new HashMap<>();
    @SuppressLint("PrimitiveInCollection")
    private static final Map<String, Integer> sIntRpnMap = new HashMap<>();

    static {
        // Build reverse maps for RPN operators
        for (int i = 0; i < 256; i++) {
            String name = AnimatedFloatExpression.toMathName(
                    AnimatedFloatExpression.asNan(AnimatedFloatExpression.OFFSET + i));
            if (name != null) {
                sFloatRpnMap.put(name, AnimatedFloatExpression.OFFSET + i);
            }
            String iName = IntegerExpressionEvaluator.toMathName(
                    IntegerExpressionEvaluator.OFFSET + i);
            if (iName != null) {
                sIntRpnMap.put(iName, IntegerExpressionEvaluator.OFFSET + i);
            }
        }
    }

    private RemoteComposeConverter() {
    }

    /**
     * Convert RemoteCompose binary to JSON.
     *
     * @param rcBytes RemoteCompose binary
     * @return JSON string
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull String remoteComposeToJson(byte @NonNull [] rcBytes)
            throws JSONException {
        WireBuffer buffer = new WireBuffer(rcBytes.length);
        System.arraycopy(rcBytes, 0, buffer.getBuffer(), 0, rcBytes.length);

        int totalLen = rcBytes.length;

        buffer.setIndex(0);
        int apiLevel = Header.peekApiLevel(buffer);
        int profiles = 0;
        if (apiLevel >= 7) {
            try {
                Header header = Header.readDirect(buffer);
                profiles = header.getProfiles();
            } catch (IOException e) {
                // ignore
            }
        }
        buffer.setIndex(0);

        Operations.UniqueIntMap<CompanionOperation> map = Operations.getOperations(apiLevel,
                profiles);
        if (map == null) {
            throw new RuntimeException(
                    "No operations map found for api=" + apiLevel + " profiles=" + profiles);
        }

        JSONObject root = new JSONObject();
        root.put("format", "androidx.compose.remote.rc.json");
        root.put("version", 1);

        JSONObject rc = new JSONObject();
        rc.put("apiLevel", apiLevel);
        rc.put("profiles", profiles);
        JSONArray opsJson = new JSONArray();

        while (buffer.getIndex() < totalLen) {
            int startIdx = buffer.getIndex();
            int opcode = buffer.readByte();

            OpcodeRegistry.OpSpec spec = OpcodeRegistry.get(opcode);
            JSONObject opJson = new JSONObject();
            opJson.put("opcode", opcode);

            if (spec != null) {
                opJson.put("kind", "op");
                opJson.put("name", spec.name);

                JSONArray fields = new JSONArray();
                for (OpcodeRegistry.FieldSpec fSpec : spec.fields) {
                    fields.put(encodeField(buffer, fSpec, fields));
                }
                opJson.put("fields", fields);

                if (spec.isFixedLength() || spec.forceReconstruct) {
                    opJson.put("reconstructFromFields", true);
                }

                int endIdx = buffer.getIndex();
                if (!spec.isFixedLength() && !spec.forceReconstruct) {
                    byte[] payload = new byte[endIdx - startIdx - 1];
                    System.arraycopy(rcBytes, startIdx + 1, payload, 0, payload.length);
                    opJson.put("payloadBase64", Base64.getEncoder().encodeToString(payload));
                }
            } else {
                CompanionOperation companion = map.get(opcode);
                if (companion == null) {
                    throw new RuntimeException(
                            "Unknown opcode " + opcode + " at index " + startIdx);
                }
                List<Operation> tempOps = new ArrayList<>();
                companion.read(buffer, tempOps);
                int endIdx = buffer.getIndex();

                opJson.put("kind", "opaque");
                if (!tempOps.isEmpty()) {
                    opJson.put("name", tempOps.get(0).getClass().getSimpleName());
                }

                byte[] payload = new byte[endIdx - startIdx - 1];
                System.arraycopy(rcBytes, startIdx + 1, payload, 0, payload.length);
                opJson.put("payloadBase64", Base64.getEncoder().encodeToString(payload));
                buffer.setIndex(endIdx);
            }

            opsJson.put(opJson);
        }

        rc.put("ops", opsJson);
        root.put("rc", rc);

        return root.toString(2);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static String formatFloat(float f) {
        int bits = Float.floatToRawIntBits(f);
        if ((bits & 0xFF800000) == 0xFF800000) {
            int id = bits & 0x7FFFFF;
            return "NaN(" + id + ")";
        }
        return Float.toString(f);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static String formatDouble(double d) {
        if (Double.isNaN(d)) {
            long id = Double.doubleToRawLongBits(d) & 0xFFFFFFFFFFFFFL;
            return "NaN(" + id + ")";
        }
        return Double.toString(d);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static long findFieldLong(JSONArray fields, String name) throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (name.equals(f.getString("name"))) {
                return Long.parseLong(f.getString("value"));
            }
        }
        return 0;
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static int findFieldInt(JSONArray fields, String name) throws JSONException {
        return (int) findFieldLong(fields, name);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static JSONObject encodeField(WireBuffer buffer, OpcodeRegistry.FieldSpec spec,
            JSONArray currentFields) throws JSONException {
        JSONObject f = new JSONObject();
        f.put("name", spec.name);
        f.put("type", spec.type.name());
        switch (spec.type) {
            case BYTE:
                f.put("value", String.valueOf(buffer.readByte()));
                break;
            case SHORT:
                f.put("value", String.valueOf(buffer.readShort()));
                break;
            case INT:
                f.put("value", String.valueOf(buffer.readInt()));
                break;
            case LONG:
                f.put("value", String.valueOf(buffer.readLong()));
                break;
            case FLOAT:
                f.put("value", formatFloat(buffer.readFloat()));
                break;
            case DOUBLE:
                f.put("value", formatDouble(buffer.readDouble()));
                break;
            case UTF8:
                f.put("value", buffer.readUTF8());
                break;
            case BOOLEAN:
                f.put("value", String.valueOf(buffer.readBoolean()));
                break;
            case BUFFER:
                byte[] data = buffer.readBuffer();
                f.put("value", Base64.getEncoder().encodeToString(data));
                break;
            case HEADER_BODY: {
                long major = findFieldLong(currentFields, "major");
                if ((major & 0xFFFF0000) == 0x048C0000) {
                    f.put("format", "modern");
                    int propCount = buffer.readInt();
                    JSONArray props = new JSONArray();
                    for (int i = 0; i < propCount; i++) {
                        short tag = (short) buffer.readShort();
                        buffer.readShort(); // itemLen
                        int dataType = tag >> 10;
                        int propId = tag & 0x3F;
                        JSONObject prop = new JSONObject();
                        prop.put("tag", propId);
                        switch (dataType) {
                            case 0: // INT
                                prop.put("type", "INT");
                                prop.put("value", buffer.readInt());
                                break;
                            case 1: // FLOAT
                                prop.put("type", "FLOAT");
                                prop.put("value", formatFloat(buffer.readFloat()));
                                break;
                            case 2: // LONG
                                prop.put("type", "LONG");
                                prop.put("value", buffer.readLong());
                                break;
                            case 3: // STRING
                                prop.put("type", "STRING");
                                prop.put("value", buffer.readUTF8());
                                break;
                        }
                        props.put(prop);
                    }
                    f.put("value", props);
                } else {
                    f.put("format", "legacy");
                    JSONObject legacy = new JSONObject();
                    legacy.put("width", buffer.readInt());
                    legacy.put("height", buffer.readInt());
                    legacy.put("capabilities", buffer.readLong());
                    f.put("value", legacy);
                }
                break;
            }
            case FLOAT_ARRAY: {
                int len = findFieldInt(currentFields, "length");
                JSONArray arr = new JSONArray();
                for (int i = 0; i < len; i++) {
                    arr.put(formatFloat(buffer.readFloat()));
                }
                f.put("value", arr);
                break;
            }
            case INT_ARRAY: {
                int len = findFieldInt(currentFields, "length");
                JSONArray arr = new JSONArray();
                for (int i = 0; i < len; i++) {
                    arr.put(buffer.readInt());
                }
                f.put("value", arr);
                break;
            }
            case FLOAT_RPN: {
                int packed = findFieldInt(currentFields, "packedLen");
                int len = packed & 0xFFFF;
                JSONArray rpnArr = new JSONArray();
                for (int i = 0; i < len; i++) {
                    float v = buffer.readFloat();
                    if (AnimatedFloatExpression.isMathOperator(v)) {
                        rpnArr.put(AnimatedFloatExpression.toMathName(v));
                    } else {
                        rpnArr.put(formatFloat(v));
                    }
                }
                f.put("value", rpnArr);
                break;
            }
            case INT_RPN: {
                int len = findFieldInt(currentFields, "length");
                int mask = findFieldInt(currentFields, "mask");
                JSONArray rpnIntArr = new JSONArray();
                for (int i = 0; i < len; i++) {
                    int v = buffer.readInt();
                    if (IntegerExpressionEvaluator.isOperation(mask, i)) {
                        String name = IntegerExpressionEvaluator.toMathName(v);
                        rpnIntArr.put(name != null ? name : String.valueOf(v));
                    } else {
                        rpnIntArr.put(String.valueOf(v));
                    }
                }
                f.put("value", rpnIntArr);
                break;
            }
            case FLOAT_ARRAY_BASE64: {
                int packed = findFieldInt(currentFields, "packedLen");
                int len = (packed >> 16) & 0xFFFF;
                byte[] b64data = new byte[len * 4];
                int startIdx = buffer.getIndex();
                System.arraycopy(buffer.getBuffer(), startIdx, b64data, 0, b64data.length);
                buffer.setIndex(startIdx + b64data.length);
                f.put("value", Base64.getEncoder().encodeToString(b64data));
                break;
            }
            case GLYPH_ARRAY:
            case KERNING_TABLE:
                // For now keep them as opaque or simple arrays if we must
                break;
        }
        return f;
    }

    public static byte @NonNull [] jsonToRemoteCompose(@NonNull String jsonStr)
            throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        JSONObject rc = root.getJSONObject("rc");
        JSONArray ops = rc.getJSONArray("ops");

        WireBuffer buffer = new WireBuffer();

        // Ensure WireBuffer allows all opcodes
        try {
            java.lang.reflect.Field validOpsField = WireBuffer.class.getDeclaredField(
                    "mValidOperations");
            validOpsField.setAccessible(true);
            boolean[] validOps = (boolean[]) validOpsField.get(buffer);
            for (int i = 0; i < 256; i++) {
                validOps[i] = true;
            }
        } catch (Exception e) {
            // ignore
        }

        for (int i = 0; i < ops.length(); i++) {
            JSONObject opJson = ops.getJSONObject(i);
            int opcode = opJson.getInt("opcode");

            buffer.start(opcode);

            if (opJson.optBoolean("reconstructFromFields", false)) {
                JSONArray fields = opJson.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject fJson = fields.getJSONObject(j);
                    OpcodeRegistry.FieldType type = OpcodeRegistry.FieldType.valueOf(
                            fJson.getString("type"));
                    writeField(buffer, type, fJson, opJson);
                }
            } else {
                byte[] payload = Base64.getDecoder().decode(opJson.getString("payloadBase64"));
                for (byte b : payload) {
                    buffer.writeByte(b & 0xFF);
                }
            }
        }

        byte[] result = new byte[buffer.getSize()];
        System.arraycopy(buffer.getBuffer(), 0, result, 0, buffer.getSize());
        return result;
    }

    private static void writeField(WireBuffer buffer, OpcodeRegistry.FieldType type,
            JSONObject fJson, JSONObject opJson) throws JSONException {
        switch (type) {
            case BYTE:
                buffer.writeByte(Integer.parseInt(fJson.getString("value")));
                break;
            case SHORT:
                buffer.writeShort(Integer.parseInt(fJson.getString("value")));
                break;
            case INT:
                if ("packedLen".equals(fJson.getString("name"))) {
                    JSONArray expr = findFieldArray(opJson.getJSONArray("fields"), "expression");
                    String anim = findFieldValue(opJson.getJSONArray("fields"), "animation");
                    int eLen = expr.length();
                    int aLen = (anim != null) ? Base64.getDecoder().decode(anim).length / 4 : 0;
                    buffer.writeInt((aLen << 16) | (eLen & 0xFFFF));
                } else {
                    buffer.writeInt(Integer.parseInt(fJson.getString("value")));
                }
                break;
            case LONG:
                buffer.writeLong(Long.parseLong(fJson.getString("value")));
                break;
            case FLOAT: {
                String s = fJson.getString("value");
                if (s.startsWith("NaN(")) {
                    int id = Integer.parseInt(s.substring(4, s.length() - 1));
                    buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                } else {
                    buffer.writeFloat(Float.parseFloat(s));
                }
                break;
            }
            case DOUBLE: {
                String s = fJson.getString("value");
                if (s.startsWith("NaN(")) {
                    long id = Long.parseLong(s.substring(4, s.length() - 1));
                    buffer.writeDouble(Double.longBitsToDouble(id | 0x7FF8000000000000L));
                } else {
                    buffer.writeDouble(Double.parseDouble(s));
                }
                break;
            }
            case UTF8:
                buffer.writeUTF8(fJson.getString("value"));
                break;
            case BOOLEAN:
                buffer.writeBoolean(Boolean.parseBoolean(fJson.getString("value")));
                break;
            case BUFFER:
                buffer.writeBuffer(Base64.getDecoder().decode(fJson.getString("value")));
                break;
            case HEADER_BODY: {
                String format = fJson.getString("format");
                if ("modern".equals(format)) {
                    JSONArray props = fJson.getJSONArray("value");
                    buffer.writeInt(props.length());
                    for (int i = 0; i < props.length(); i++) {
                        JSONObject p = props.getJSONObject(i);
                        int tag = p.getInt("tag");
                        String pType = p.getString("type");
                        switch (pType) {
                            case "INT":
                                buffer.writeShort((short) (tag | (0 << 10)));
                                buffer.writeShort(4);
                                buffer.writeInt(p.getInt("value"));
                                break;
                            case "FLOAT": {
                                buffer.writeShort((short) (tag | (1 << 10)));
                                buffer.writeShort(4);
                                String fv = p.getString("value");
                                if (fv.startsWith("NaN(")) {
                                    int id = Integer.parseInt(fv.substring(4, fv.length() - 1));
                                    buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                                } else {
                                    buffer.writeFloat(Float.parseFloat(fv));
                                }
                                break;
                            }
                            case "LONG":
                                buffer.writeShort((short) (tag | (2 << 10)));
                                buffer.writeShort(8);
                                buffer.writeLong(p.getLong("value"));
                                break;
                            case "STRING": {
                                buffer.writeShort((short) (tag | (3 << 10)));
                                String sv = p.getString("value");
                                byte[] data = sv.getBytes();
                                buffer.writeShort((short) (data.length + 4));
                                buffer.writeBuffer(data);
                                break;
                            }
                        }
                    }
                } else {
                    JSONObject legacy = fJson.getJSONObject("value");
                    buffer.writeInt(legacy.getInt("width"));
                    buffer.writeInt(legacy.getInt("height"));
                    buffer.writeLong(legacy.getLong("capabilities"));
                }
                break;
            }
            case FLOAT_ARRAY: {
                JSONArray arr = fJson.getJSONArray("value");
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.getString(i);
                    if (s.startsWith("NaN(")) {
                        int id = Integer.parseInt(s.substring(4, s.length() - 1));
                        buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                    } else {
                        buffer.writeFloat(Float.parseFloat(s));
                    }
                }
                break;
            }
            case FLOAT_RPN: {
                JSONArray arr = fJson.getJSONArray("value");
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.getString(i);
                    Integer op = sFloatRpnMap.get(s);
                    if (op != null) {
                        buffer.writeFloat(AnimatedFloatExpression.asNan(op));
                    } else if (s.startsWith("NaN(")) {
                        int id = Integer.parseInt(s.substring(4, s.length() - 1));
                        buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                    } else {
                        buffer.writeFloat(Float.parseFloat(s));
                    }
                }
                break;
            }
            case INT_ARRAY: {
                JSONArray arr = fJson.getJSONArray("value");
                for (int i = 0; i < arr.length(); i++) {
                    buffer.writeInt(arr.getInt(i));
                }
                break;
            }
            case INT_RPN: {
                JSONArray arr = fJson.getJSONArray("value");
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.getString(i);
                    Integer op = sIntRpnMap.get(s);
                    if (op != null) {
                        buffer.writeInt(op);
                    } else {
                        buffer.writeInt(Integer.parseInt(s));
                    }
                }
                break;
            }
            case FLOAT_ARRAY_BASE64: {
                byte[] data = Base64.getDecoder().decode(fJson.getString("value"));
                for (byte b : data) {
                    buffer.writeByte(b & 0xFF);
                }
                break;
            }
            case GLYPH_ARRAY:
            case KERNING_TABLE:
                break;
        }
    }

    private static @Nullable JSONArray findFieldArray(JSONArray fields, String name)
            throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (name.equals(f.getString("name"))) {
                return f.getJSONArray("value");
            }
        }
        return null;
    }

    private static @Nullable String findFieldValue(JSONArray fields, String name)
            throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (name.equals(f.getString("name"))) {
                return f.optString("value", null);
            }
        }
        return null;
    }
}
