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
package androidx.constraintlayout.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CLParserBenchmarkTest {

    String mSimpleFromWiki2 =
            "{\n"
                    + "  firstName: 'John',\n"
                    + "  lastName: 'Smith',\n"
                    + "  isAlive: true,\n"
                    + "  age: 27,\n"
                    + "  address: {\n"
                    + "    streetAddress: '21 2nd Street',\n"
                    + "    city: 'New York',\n"
                    + "    state: 'NY',\n"
                    + "    postalCode: '10021-3100'\n"
                    + "  },\n"
                    + "  phoneNumbers: [\n"
                    + "    {\n"
                    + "      type: 'home',\n"
                    + "      number: '212 555-1234'\n"
                    + "    },\n"
                    + "    {\n"
                    + "      type: 'office',\n"
                    + "      number: '646 555-4567'\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  children: [],\n"
                    + "  spouse: null\n"
                    + "}";

    @Test
    public void parseAndCheck1000x() {
        try {
            for (int i = 0; i < 1000; i++) {
                parseAndeCheck();
            }
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void parse1000x() {
        try {
            for (int i = 0; i < 1000; i++) {
                parseOnce();
            }
            parseAndeCheck();
        } catch (CLParsingException e) {
            System.err.println("Exception " + e.reason());
            e.printStackTrace();
            assertTrue(false);
        }
    }

    private void parseOnce() throws CLParsingException {
        String test = mSimpleFromWiki2;
        CLObject parsedContent = CLParser.parse(test);
        CLObject o;

        assertEquals("John", parsedContent.getString("firstName"));
    }

    private void parseAndeCheck() throws CLParsingException {
        String test = mSimpleFromWiki2;
        CLObject parsedContent = CLParser.parse(test);
        CLObject o;

        assertEquals("John", parsedContent.getString("firstName"));
        assertEquals("Smith", parsedContent.getString("lastName"));
        assertEquals(true, parsedContent.getBoolean("isAlive"));
        assertEquals(27, parsedContent.getInt("age"));
        assertEquals(
                "{ streetAddress: '21 2nd Street', city: 'New York'"
                        + ", state: 'NY', postalCode: '10021-3100' }",
                (o = parsedContent.getObject("address")).toJSON());
        assertEquals("21 2nd Street", o.getString("streetAddress"));
        assertEquals("New York", o.getString("city"));
        assertEquals("NY", o.getString("state"));
        assertEquals("NY", o.getString("state"));
        assertEquals("NY", o.getString("state"));
        assertEquals("NY", o.getString("state"));
        assertEquals("10021-3100", o.getString("postalCode"));
        assertEquals("{ type: 'home', number: '212 555-1234' }",
                (o = parsedContent.getArray("phoneNumbers").getObject(0)).toJSON());
        assertEquals("home", o.getString("type"));
        assertEquals("212 555-1234", o.getString("number"));
        assertEquals("{ type: 'office', number: '646 555-4567' }",
                (o = parsedContent.getArray("phoneNumbers").getObject(1)).toJSON());
        assertEquals("office", o.getString("type"));
        assertEquals("646 555-4567", o.getString("number"));
        assertEquals(0, parsedContent.getArray("children").mElements.size());
        CLElement element = parsedContent.get("spouse");
        if (element instanceof CLToken) {
            CLToken token = (CLToken) element;
            assertEquals(CLToken.Type.NULL, token.mType);
        }
    }
}
