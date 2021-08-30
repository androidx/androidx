/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.security.identity;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import co.nstant.in.cbor.CborException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CreateItemsRequestTest {
    @Test
    public void basicRequest() throws CborException {
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.test.ns", Arrays.asList("xyz", "abc"));

        String docType = "org.test.ns";
        assertEquals("{\n"
                        + "  'docType' : 'org.test.ns',\n"
                        + "  'nameSpaces' : {\n"
                        + "    'org.test.ns' : {\n"
                        + "      'abc' : false,\n"
                        + "      'xyz' : false\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(Util.createItemsRequest(entriesToRequest, docType)));
    }

    @Test
    public void multipleNamespaces() throws CborException {
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.test.ns1", Arrays.asList("foo", "bar"));
        entriesToRequest.put("org.test.ns2", Arrays.asList("xyz", "abc"));
        String docType = "org.test.ns";
        assertEquals("{\n"
                        + "  'docType' : 'org.test.ns',\n"
                        + "  'nameSpaces' : {\n"
                        + "    'org.test.ns1' : {\n"
                        + "      'bar' : false,\n"
                        + "      'foo' : false\n"
                        + "    },\n"
                        + "    'org.test.ns2' : {\n"
                        + "      'abc' : false,\n"
                        + "      'xyz' : false\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(Util.createItemsRequest(entriesToRequest, docType)));
    }

    @Test
    public void noDocType() throws CborException {
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        entriesToRequest.put("org.test.ns1", Arrays.asList("foo", "bar"));
        assertEquals("{\n"
                        + "  'nameSpaces' : {\n"
                        + "    'org.test.ns1' : {\n"
                        + "      'bar' : false,\n"
                        + "      'foo' : false\n"
                        + "    }\n"
                        + "  }\n"
                        + "}",
                Util.cborPrettyPrint(Util.createItemsRequest(entriesToRequest, null)));
    }

    @Test
    public void empty() throws CborException {
        Map<String, Collection<String>> entriesToRequest = new LinkedHashMap<>();
        assertEquals("{\n"
                        + "  'nameSpaces' : {}\n"
                        + "}",
                Util.cborPrettyPrint(Util.createItemsRequest(entriesToRequest, null)));
    }
}
