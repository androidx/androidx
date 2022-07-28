/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion.parse;

import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.parser.CLElement;
import androidx.constraintlayout.core.parser.CLKey;
import androidx.constraintlayout.core.parser.CLObject;
import androidx.constraintlayout.core.parser.CLParser;
import androidx.constraintlayout.core.parser.CLParsingException;

import java.util.Arrays;

public class KeyParser {

    private interface Ids {
        int get(String str);
    }

    private interface DataType {
        int get(int str);
    }

    private static TypedBundle parse(String str, Ids table, DataType dtype) {
        TypedBundle bundle = new TypedBundle();

        try {
            CLObject parsedContent = CLParser.parse(str);
            int n = parsedContent.size();
            for (int i = 0; i < n; i++) {
                CLKey clkey = ((CLKey) parsedContent.get(i));
                String type = clkey.content();
                CLElement value = clkey.getValue();
                int id = table.get(type);
                if (id == -1) {
                    System.err.println("unknown type " + type);
                    continue;
                }
                switch (dtype.get(id)) {
                    case TypedValues.FLOAT_MASK:
                        bundle.add(id, value.getFloat());
                        System.out.println("parse " + type + " FLOAT_MASK > " + value.getFloat());
                        break;
                    case TypedValues.STRING_MASK:
                        bundle.add(id, value.content());
                        System.out.println("parse " + type + " STRING_MASK > " + value.content());

                        break;
                    case TypedValues.INT_MASK:
                        bundle.add(id, value.getInt());
                        System.out.println("parse " + type + " INT_MASK > " + value.getInt());
                        break;
                    case TypedValues.BOOLEAN_MASK:
                        bundle.add(id, parsedContent.getBoolean(i));
                        break;
                }
            }
        } catch (CLParsingException e) {
            //TODO replace with something not equal to printStackTrace();
            System.err.println(e.toString()+"\n"+ Arrays.toString(e.getStackTrace())
                    .replace("[","   at ")
                    .replace(",","\n   at")
                    .replace("]",""));
        }
        return bundle;
    }

    // @TODO: add description
    public static TypedBundle parseAttributes(String str) {
        return parse(str, TypedValues.AttributesType::getId, TypedValues.AttributesType::getType);
    }

    // @TODO: add description
    public static void main(String[] args) {
        String str = "{"
                + "frame:22,\n"
                + "target:'widget1',\n"
                + "easing:'easeIn',\n"
                + "curveFit:'spline',\n"
                + "progress:0.3,\n"
                + "alpha:0.2,\n"
                + "elevation:0.7,\n"
                + "rotationZ:23,\n"
                + "rotationX:25.0,\n"
                + "rotationY:27.0,\n"
                + "pivotX:15,\n"
                + "pivotY:17,\n"
                + "pivotTarget:'32',\n"
                + "pathRotate:23,\n"
                + "scaleX:0.5,\n"
                + "scaleY:0.7,\n"
                + "translationX:5,\n"
                + "translationY:7,\n"
                + "translationZ:11,\n"
                + "}";
        parseAttributes(str);
    }
}
