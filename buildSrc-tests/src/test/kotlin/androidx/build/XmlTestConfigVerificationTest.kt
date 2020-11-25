/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

/**
 * Simple check that the test config templates are able to be parsed as valid xml.
 */
@RunWith(JUnit4::class)
class XmlTestConfigVerificationTest {

    @Test
    fun testValidTestConfigXml_TEMPLATE() {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        parser.parse(
            InputSource(StringReader(TEMPLATE.replace("TEST_BLOCK", FULL_TEST))),
            DefaultHandler()
        )
    }

    @Test
    fun testValidTestConfigXml_SELF_INSTRUMENTING_TEMPLATE() {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        parser.parse(
            InputSource(
                StringReader(
                    SELF_INSTRUMENTING_TEMPLATE.replace(
                        "TEST_BLOCK",
                        DEPENDENT_TESTS
                    )
                )
            ),
            DefaultHandler()
        )
    }

    @Test
    fun testValidTestConfigXml_MEDIA_TEMPLATE() {
        val parser = SAXParserFactory.newInstance().newSAXParser()
        parser.parse(
            InputSource(
                StringReader(
                    MEDIA_TEMPLATE.replace(
                        "INSTRUMENTATION_ARGS",
                        CLIENT_PREVIOUS + SERVICE_PREVIOUS
                    )
                )
            ),
            DefaultHandler()
        )
    }
}