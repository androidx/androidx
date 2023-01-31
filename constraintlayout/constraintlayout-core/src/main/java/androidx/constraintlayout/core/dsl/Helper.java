/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.HashMap;
import java.util.Map;

public class Helper {
    public enum Type {
        VERTICAL_GUIDELINE,
        HORIZONTAL_GUIDELINE,
        VERTICAL_CHAIN,
        HORIZONTAL_CHAIN,
        BARRIER
    }

    protected final String name;
    protected HelperType type = null;
    protected String config;
    protected Map<String, String> configMap = new HashMap<>();
    final static protected Map<Constraint.Side, String> sideMap = new HashMap<>();
    static {
        sideMap.put(Constraint.Side.LEFT, "'left'");
        sideMap.put(Constraint.Side.RIGHT, "'right'");
        sideMap.put(Constraint.Side.TOP, "'top'");
        sideMap.put(Constraint.Side.BOTTOM, "'bottom'");
        sideMap.put(Constraint.Side.START, "'start'");
        sideMap.put(Constraint.Side.END, "'end'");
        sideMap.put(Constraint.Side.BASELINE, "'baseline'");
    }

    final static protected Map<Helper.Type, String> typeMap = new HashMap<>();
    static {
        typeMap.put(Type.VERTICAL_GUIDELINE, "vGuideline");
        typeMap.put(Type.HORIZONTAL_GUIDELINE, "hGuideline");
        typeMap.put(Type.VERTICAL_CHAIN, "vChain");
        typeMap.put(Type.HORIZONTAL_CHAIN, "hChain");
        typeMap.put(Type.BARRIER, "barrier");
    }

    public Helper(String name, HelperType type) {
        this.name = name;
        this.type = type;
    }

    public Helper(String name, HelperType type, String config) {
        this.name = name;
        this.type = type;
        this.config = config;
        configMap = convertConfigToMap();
    }

    public String getId() {
        return name;
    }

    public HelperType getType() {
        return type;
    }

    public String getConfig() {
        return config;
    }

    public Map<String, String> convertConfigToMap() {
        if (config == null || config.length() == 0) {
            return null;
        }

        Map<String, String> map = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        String key = "";
        String value = "";
        int squareBrackets = 0;
        int curlyBrackets = 0;
        char ch;
        for (int i = 0; i < config.length(); i++) {
            ch = config.charAt(i);
            if (ch == ':') {
                key = builder.toString();
                builder.setLength(0);
            } else if (ch == ',' && squareBrackets == 0 && curlyBrackets == 0) {
                value = builder.toString();
                map.put(key, value);
                key = value = "";
                builder.setLength(0);
            } else if (ch != ' ') {
                switch (ch) {
                    case '[':
                        squareBrackets++;
                        break;
                    case '{':
                        curlyBrackets++;
                        break;
                    case ']':
                        squareBrackets--;
                        break;
                    case '}':
                        curlyBrackets--;
                        break;
                }

                builder.append(ch);
            }
        }

        map.put(key, builder.toString());
        return map;
    }

    public void append(Map<String, String> map, StringBuilder ret) {
        if (map.isEmpty()) {
            return;
        }
        for (String key : map.keySet()) {
            ret.append(key).append(":").append(map.get(key)).append(",\n");
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(name + ":{\n");

        if (type != null) {
            ret.append("type:'").append(type.toString()).append("',\n");
        }

        if (configMap != null) {
            append(configMap, ret);
        }

        ret.append("},\n");
        return ret.toString();
    }

    public static void main(String[] args) {
        Barrier b = new Barrier("abc", "['a1', 'b2']");
        System.out.println(b.toString());
    }

    public static final class HelperType {
        final String mName;

        public HelperType(String str) {
            mName = str;
        }

        @Override
        public String toString() { return mName; }
    }
}
