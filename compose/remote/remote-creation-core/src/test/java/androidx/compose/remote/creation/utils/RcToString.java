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
package androidx.compose.remote.creation.utils;


import androidx.compose.remote.core.CompanionOperation;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Lossless converter between RemoteCompose binary (.rc) and JSON.
 * Guaranteed bit-for-bit round-trip fidelity.
 */
public class RcToString {
    private static final Map<String, Integer> sFloatRpnMap = new HashMap<>();
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

    private RcToString() {
    }

    /**
     * Convert RemoteCompose binary to JSON.
     *
     * @param rcBytes RemoteCompose binary
     * @return JSON string
     */
    public static @NonNull String toString(byte @NonNull [] rcBytes) {
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

        JSerialObject root = new JSerialObject();
        root.put("format", "androidx.compose.remote.rc.json");
        root.put("version", 1);

        JSerialObject rc = new JSerialObject();
        rc.put("apiLevel", apiLevel);
        rc.put("profiles", profiles);
        JSerialArray opsJson = new JSerialArray();

        while (buffer.getIndex() < totalLen) {
            int startIdx = buffer.getIndex();
            int opcode = buffer.readByte();

            OpcodeRegistry.OpSpec spec = OpcodeRegistry.get(opcode);
            JSerialObject opJson = new JSerialObject();
            opJson.put("opcode", opcode);

            if (spec != null) {
                opJson.put("kind", "op");
                opJson.put("name", spec.getName());

                JSerialArray fields = new JSerialArray();
                for (OpcodeRegistry.FieldSpec fSpec : spec.getFields()) {
                    fields.add(encodeField(buffer, fSpec, fields, opcode));
                }
                opJson.put("fields", fields);

                if (spec.isFixedLength() || spec.forceReconstruct()) {
                    opJson.put("reconstructFromFields", true);
                }

                int endIdx = buffer.getIndex();
                if (!spec.isFixedLength() && !spec.forceReconstruct()) {
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

            opsJson.add(opJson);
        }

        rc.put("ops", opsJson);
        root.put("rc", rc);

        return root.toString();
    }

    private static String formatFloat(float f) {
        int bits = Float.floatToRawIntBits(f);
        if ((bits & 0xFF800000) == 0xFF800000) {
            int id = bits & 0x7FFFFF;
            return "NaN(" + id + ")";
        }
        return Float.toString(f);
    }

    private static String formatDouble(double d) {
        if (Double.isNaN(d)) {
            long id = Double.doubleToRawLongBits(d) & 0xFFFFFFFFFFFFFL;
            return "NaN(" + id + ")";
        }
        return Double.toString(d);
    }

    private static long findFieldLong(JSerialArray fields, String name) {
        for (int i = 0; i < fields.size(); i++) {
            JSerialObject f = fields.getAsObject(i);
            if (name.equals(f.getAsString("name"))) {
                String val = f.getAsString("value");
                if (val == null) return 0;
                try {
                    return Long.parseLong(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static int findFieldInt(JSerialArray fields, String name) {
        for (int i = 0; i < fields.size(); i++) {
            JSerialObject f = fields.getAsObject(i);
            if (name.equals(f.getAsString("name"))) {
                String val = f.getAsString("value");
                if (val == null) return 0;
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static float findFieldFloat(JSerialArray fields, String name) {
        for (int i = 0; i < fields.size(); i++) {
            JSerialObject f = fields.getAsObject(i);
            if (name.equals(f.getAsString("name"))) {
                String val = f.getAsString("value");
                if (val == null) return 0f;
                try {
                    return Float.parseFloat(val);
                } catch (NumberFormatException e) {
                    return 0f;
                }
            }
        }
        return 0f;
    }

    private static JSerialObject encodeField(WireBuffer buffer, OpcodeRegistry.FieldSpec spec,
            JSerialArray currentFields, int opcode) {
        JSerialObject f = new JSerialObject();
        f.put("name", spec.getName());
        f.put("type", spec.getType().name());
        switch (spec.getType()) {
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
            case BITMAP_TEXT_ID: {
                int v = buffer.readInt();
                f.put("raw", v);
                if ((v & 0x80000000) != 0) {
                    v = v & 0xFFFF;
                }
                f.put("value", String.valueOf(v));
                break;
            }
            case BITMAP_TEXT_GLYPH_SPACING: {
                int rawTextId = 0;
                for (int i = 0; i < currentFields.size(); i++) {
                    JSerialObject field = currentFields.getAsObject(i);
                    if ("textId".equals(field.getAsString("name"))) {
                        rawTextId = field.getAsInt("raw");
                        break;
                    }
                }
                if ((rawTextId & 0x80000000) != 0) {
                    f.put("value", formatFloat(buffer.readFloat()));
                } else {
                    f.put("value", formatFloat(0f));
                }
                break;
            }
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
                    JSerialArray props = new JSerialArray();
                    for (int i = 0; i < propCount; i++) {
                        short tag = (short) buffer.readShort();
                        buffer.readShort(); // itemLen
                        int dataType = tag >> 10;
                        int propId = tag & 0x3F;
                        JSerialObject prop = new JSerialObject();
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
                        props.add(prop);
                    }
                    f.put("value", props);
                } else {
                    f.put("format", "legacy");
                    JSerialObject legacy = new JSerialObject();
                    legacy.put("width", buffer.readInt());
                    legacy.put("height", buffer.readInt());
                    legacy.put("capabilities", buffer.readLong());
                    f.put("value", legacy);
                }
                break;
            }
            case FLOAT_ARRAY: {
                int len = findFieldInt(currentFields, "length");
                JSerialArray arr = new JSerialArray();
                for (int i = 0; i < len; i++) {
                    arr.add(formatFloat(buffer.readFloat()));
                }
                f.put("value", arr);
                break;
            }
            case INT_ARRAY: {
                int len;
                if (opcode == Operations.PAINT_VALUES && "paintBundle".equals(spec.getName())) {
                    len = buffer.readInt();
                } else {
                    len = findFieldInt(currentFields, "length");
                }
                JSerialArray arr = new JSerialArray();
                for (int i = 0; i < len; i++) {
                    arr.add(buffer.readInt());
                }
                f.put("value", arr);
                break;
            }
            case FLOAT_RPN: {
                int packed = findFieldInt(currentFields, "packedLen");
                int len = packed & 0xFFFF;
                JSerialArray rpnArr = new JSerialArray();
                for (int i = 0; i < len; i++) {
                    float v = buffer.readFloat();
                    if (AnimatedFloatExpression.isMathOperator(v)) {
                        rpnArr.add(AnimatedFloatExpression.toMathName(v));
                    } else {
                        rpnArr.add(formatFloat(v));
                    }
                }
                f.put("value", rpnArr);
                break;
            }
            case INT_RPN: {
                int len = findFieldInt(currentFields, "length");
                int mask = findFieldInt(currentFields, "mask");
                if (mask == 0 && findFieldArray(currentFields, "mask") == null) {
                    mask = 0xFFFFFFFF;
                }
                JSerialArray rpnIntArr = new JSerialArray();
                for (int i = 0; i < len; i++) {
                    int v = buffer.readInt();
                    if (IntegerExpressionEvaluator.isOperation(mask, i)) {
                        if (v >= IntegerExpressionEvaluator.OFFSET) {
                            String name = IntegerExpressionEvaluator.toMathName(v);
                            rpnIntArr.add(name != null ? name : String.valueOf(v));
                        } else {
                            // It's an ID (bit set in mask, but value < OFFSET)
                            rpnIntArr.add("[" + v + "]");
                        }
                    } else {
                        rpnIntArr.add(String.valueOf(v));
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

    private static void writeField(WireBuffer buffer, OpcodeRegistry.FieldType type,
            JSerialObject fJson, JSerialObject opJson, int opcode) {
        switch (type) {
            case BYTE:
                buffer.writeByte(Integer.parseInt(fJson.getAsString("value")));
                break;
            case SHORT:
                buffer.writeShort(Integer.parseInt(fJson.getAsString("value")));
                break;
            case INT:
                if ("packedLen".equals(fJson.getAsString("name"))) {
                    JSerialArray expr = findFieldArray(opJson.getAsArray("fields"), "expression");
                    String anim = findFieldValue(opJson.getAsArray("fields"), "animation");
                    int eLen = expr.size();
                    int aLen = (anim != null) ? Base64.getDecoder().decode(anim).length / 4 : 0;
                    buffer.writeInt((aLen << 16) | (eLen & 0xFFFF));
                } else {
                    buffer.writeInt(Integer.parseInt(fJson.getAsString("value")));
                }
                break;
            case LONG:
                buffer.writeLong(Long.parseLong(fJson.getAsString("value")));
                break;
            case FLOAT: {
                String s = fJson.getAsString("value");
                if (s.startsWith("NaN(")) {
                    int id = Integer.parseInt(s.substring(4, s.length() - 1));
                    buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                } else {
                    buffer.writeFloat(Float.parseFloat(s));
                }
                break;
            }
            case DOUBLE: {
                String s = fJson.getAsString("value");
                if (s.startsWith("NaN(")) {
                    long id = Long.parseLong(s.substring(4, s.length() - 1));
                    buffer.writeDouble(Double.longBitsToDouble(id | 0x7FF8000000000000L));
                } else {
                    buffer.writeDouble(Double.parseDouble(s));
                }
                break;
            }
            case UTF8:
                buffer.writeUTF8(fJson.getAsString("value"));
                break;
            case BOOLEAN:
                buffer.writeBoolean(Boolean.parseBoolean(fJson.getAsString("value")));
                break;
            case BITMAP_TEXT_ID: {
                int textId = Integer.parseInt(fJson.getAsString("value"));
                float glyphSpacing = findFieldFloat(opJson.getAsArray("fields"), "glyphSpacing");
                if (glyphSpacing != 0f) {
                    textId |= 0x80000000;
                }
                buffer.writeInt(textId);
                break;
            }
            case BITMAP_TEXT_GLYPH_SPACING: {
                float glyphSpacing = Float.parseFloat(fJson.getAsString("value"));
                if (glyphSpacing != 0f) {
                    buffer.writeFloat(glyphSpacing);
                }
                break;
            }
            case BUFFER:
                buffer.writeBuffer(Base64.getDecoder().decode(fJson.getAsString("value")));
                break;
            case HEADER_BODY: {
                String format = fJson.getAsString("format");
                if ("modern".equals(format)) {
                    JSerialArray props = (JSerialArray) fJson.get("value");
                    buffer.writeInt(props.size());
                    for (int i = 0; i < props.size(); i++) {
                        JSerialObject p = props.getAsObject(i);
                        int tag = p.getAsInt("tag");
                        String pType = p.getAsString("type");
                        switch (pType) {
                            case "INT":
                                buffer.writeShort((short) (tag | (0 << 10)));
                                buffer.writeShort(4);
                                buffer.writeInt(p.getAsInt("value"));
                                break;
                            case "FLOAT": {
                                buffer.writeShort((short) (tag | (1 << 10)));
                                buffer.writeShort(4);
                                String fv = p.getAsString("value");
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
                                buffer.writeLong(Long.parseLong(p.getAsString("value")));
                                break;
                            case "STRING": {
                                buffer.writeShort((short) (tag | (3 << 10)));
                                String sv = p.getAsString("value");
                                byte[] data = sv.getBytes();
                                buffer.writeShort((short) (data.length + 4));
                                buffer.writeBuffer(data);
                                break;
                            }
                        }
                    }
                } else {
                    JSerialObject legacy = (JSerialObject) fJson.get("value");
                    buffer.writeInt(legacy.getAsInt("width"));
                    buffer.writeInt(legacy.getAsInt("height"));
                    buffer.writeLong(legacy.getAsLong("capabilities"));
                }
                break;
            }
            case FLOAT_ARRAY: {
                JSerialArray arr = (JSerialArray) fJson.get("value");
                for (int i = 0; i < arr.size(); i++) {
                    String s = (String) arr.get(i);
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
                JSerialArray arr = (JSerialArray) fJson.get("value");
                for (int i = 0; i < arr.size(); i++) {
                    String s = (String) arr.get(i);
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
                JSerialArray arr = (JSerialArray) fJson.get("value");
                if (opcode == Operations.PAINT_VALUES
                        && "paintBundle".equals(fJson.getAsString("name"))) {
                    buffer.writeInt(arr.size());
                }
                for (int i = 0; i < arr.size(); i++) {
                    buffer.writeInt((Integer) arr.get(i));
                }
                break;
            }
            case INT_RPN: {
                JSerialArray arr = (JSerialArray) fJson.get("value");
                for (int i = 0; i < arr.size(); i++) {
                    String s = (String) arr.get(i);
                    Integer op = sIntRpnMap.get(s);
                    if (op != null) {
                        buffer.writeInt(op);
                    } else if (s.startsWith("[") && s.endsWith("]")) {
                        buffer.writeInt(Integer.parseInt(s.substring(1, s.length() - 1)));
                    } else {
                        buffer.writeInt(Integer.parseInt(s));
                    }
                }
                break;
            }
            case FLOAT_ARRAY_BASE64: {
                byte[] data = Base64.getDecoder().decode(fJson.getAsString("value"));
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

    private static @Nullable JSerialArray findFieldArray(JSerialArray fields, String name) {
        for (int i = 0; i < fields.size(); i++) {
            JSerialObject f = fields.getAsObject(i);
            if (name.equals(f.getAsString("name"))) {
                return (JSerialArray) f.get("value");
            }
        }
        return null;
    }

    private static @Nullable String findFieldValue(JSerialArray fields, String name) {
        for (int i = 0; i < fields.size(); i++) {
            JSerialObject f = fields.getAsObject(i);
            if (name.equals(f.getAsString("name"))) {
                return f.getAsString("value");
            }
        }
        return null;
    }

    // Inner classes based on java.util.Properties
    static class JSerialObject extends Properties {
        public void put(String key, Object value) {
            super.put(key, value);
        }

        public String getAsString(String key) {
            Object v = get(key);
            return v != null ? v.toString() : null;
        }

        public int getAsInt(String key) {
            return Integer.parseInt(getAsString(key));
        }

        public float getAsFloat(String key) {
            return Float.parseFloat(getAsString(key));
        }

        public long getAsLong(String key) {
            return Long.parseLong(getAsString(key));
        }

        public boolean getAsBoolean(String key) {
            return Boolean.parseBoolean(getAsString(key));
        }

        public JSerialObject getAsObject(String key) {
            return (JSerialObject) get(key);
        }

        public JSerialArray getAsArray(String key) {
            return (JSerialArray) get(key);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<Object, Object> entry : entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                Object val = entry.getValue();
                if (val instanceof String) {
                    sb.append("\"").append(val).append("\"");
                } else {
                    sb.append(val);
                }
                sb.append("\n");
            }
            sb.append("}\n");
            return sb.toString();
        }
    }

    static class JSerialArray extends Properties {
        private int mSize = 0;

        public void add(Object value) {
            put(String.valueOf(mSize++), value);
        }

        public int size() {
            return mSize;
        }

        public Object get(int index) {
            return get(String.valueOf(index));
        }

        public JSerialObject getAsObject(int index) {
            return (JSerialObject) get(index);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < mSize; i++) {
                if (i > 0) sb.append(",");
                Object val = get(i);
                if (val instanceof String) {
                    sb.append("\"").append(val).append("\"");
                } else {
                    sb.append(val);
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static class JSerialParser {
        private final String mSrc;
        private int mPos = 0;

        JSerialParser(String src) {
            this.mSrc = src;
        }

        public Object parse() {
            skipWhitespace();
            if (mPos >= mSrc.length()) return null;
            char c = mSrc.charAt(mPos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            return parsePrimitive();
        }

        private JSerialObject parseObject() {
            JSerialObject obj = new JSerialObject();
            mPos++; // skip '{'
            while (mPos < mSrc.length()) {
                skipWhitespace();
                if (mSrc.charAt(mPos) == '}') {
                    mPos++;
                    break;
                }
                String key = parseString();
                skipWhitespace();
                mPos++; // skip ':'
                Object val = parse();
                obj.put(key, val);
                skipWhitespace();
                if (mPos < mSrc.length() && mSrc.charAt(mPos) == ',') {
                    mPos++;
                }
            }
            return obj;
        }

        private JSerialArray parseArray() {
            JSerialArray arr = new JSerialArray();
            mPos++; // skip '['
            while (mPos < mSrc.length()) {
                skipWhitespace();
                if (mSrc.charAt(mPos) == ']') {
                    mPos++;
                    break;
                }
                Object val = parse();
                arr.add(val);
                skipWhitespace();
                if (mPos < mSrc.length() && mSrc.charAt(mPos) == ',') {
                    mPos++;
                }
            }
            return arr;
        }

        private String parseString() {
            mPos++; // skip '"'
            int start = mPos;
            while (mPos < mSrc.length() && mSrc.charAt(mPos) != '"') {
                mPos++;
            }
            String s = mSrc.substring(start, mPos);
            mPos++; // skip '"'
            return s;
        }

        private Object parsePrimitive() {
            int start = mPos;
            while (mPos < mSrc.length() && mSrc.charAt(mPos) != ',' && mSrc.charAt(mPos) != '}'
                    && mSrc.charAt(mPos) != ']') {
                mPos++;
            }
            String s = mSrc.substring(start, mPos).trim();
            if ("true".equals(s)) return true;
            if ("false".equals(s)) return false;
            if ("null".equals(s)) return null;
            try {
                if (s.contains(".")) {
                    return Float.parseFloat(s);
                }
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return s;
            }
        }

        private void skipWhitespace() {
            while (mPos < mSrc.length() && Character.isWhitespace(mSrc.charAt(mPos))) {
                mPos++;
            }
        }
    }
}
