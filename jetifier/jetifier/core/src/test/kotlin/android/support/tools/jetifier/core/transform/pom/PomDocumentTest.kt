/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.pom

import android.support.tools.jetifier.core.archive.ArchiveFile
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class PomDocumentTest {

    @Test fun pom_noRules_noChange() {
        testRewriteToTheSame(
            givenAndExpectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "      <type>jar</type>\n" +
            "      <scope>test</scope>\n" +
            "      <optional>true</optional>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf()
        )
    }

    @Test fun pom_oneRule_shouldApply() {
        testRewrite(
            givenXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <systemPath>test/test</systemPath>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            expectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <systemPath>test/test</systemPath>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0")
                    )
                )
            )
        )
    }

    @Test fun pom_oneRule_shouldSkipTestScopedRule() {
        testRewriteToTheSame(
            givenAndExpectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "      <scope>test</scope>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0")
                    )
                )
            )
        )
    }

    @Test fun pom_oneRule_notApplicable() {
        testRewriteToTheSame(
            givenAndExpectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact2",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0")
                    )
                )
            )
        )
    }

    @Test fun pom_oneRule_appliedForEachType() {
        testRewrite(
            givenXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "      <type>test</type>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "      <type>compile</type>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            expectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "      <type>test</type>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "      <type>compile</type>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0")
                    )
                )
            )
        )
    }

    @Test fun pom_multipleTargets_shouldApplyAll() {
        testRewrite(
            givenXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            expectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup2</groupId>\n" +
            "      <artifactId>testArtifact2</artifactId>\n" +
            "      <version>2.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0"),
                        PomDependency(
                            groupId = "testGroup2", artifactId = "testArtifact2",
                            version = "2.0"))
                )
            )
        )
    }

    @Test fun pom_multipleRulesAndTargets_shouldApplyAll_distinct() {
        testRewrite(
            givenXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact2</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            expectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "    </dependency>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup2</groupId>\n" +
            "      <artifactId>testArtifact2</artifactId>\n" +
            "      <version>2.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0"),
                        PomDependency(
                            groupId = "testGroup2", artifactId = "testArtifact2",
                            version = "2.0")
                    )
                ),
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact2",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0"),
                        PomDependency(
                            groupId = "testGroup2", artifactId = "testArtifact2",
                            version = "2.0"))
                )
            )
        )
    }

    @Test fun pom_oneRule_hasToKeepExtraAttributesAndRewrite() {
        testRewrite(
            givenXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>supportArtifact</artifactId>\n" +
            "      <version>4.0</version>\n" +
            "      <classifier>hey</classifier>\n" +
            "      <type>jar</type>\n" +
            "      <scope>runtime</scope>\n" +
            "      <systemPath>somePath</systemPath>\n" +
            "      <optional>true</optional>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            expectedXml =
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>testGroup</groupId>\n" +
            "      <artifactId>testArtifact</artifactId>\n" +
            "      <version>1.0</version>\n" +
            "      <classifier>hey</classifier>\n" +
            "      <type>jar</type>\n" +
            "      <scope>runtime</scope>\n" +
            "      <systemPath>somePath</systemPath>\n" +
            "      <optional>true</optional>\n" +
            "    </dependency>\n" +
            "  </dependencies>",
            rules = listOf(
                PomRewriteRule(
                    PomDependency(
                        groupId = "supportGroup", artifactId = "supportArtifact",
                        version =  "4.0"),
                    listOf(
                        PomDependency(
                            groupId = "testGroup", artifactId = "testArtifact",
                            version = "1.0")
                    )
                )
            )
        )
    }

    @Test fun pom_usingEmptyProperties_shouldNotCrash() {
        val document = loadDocument(
            "  <properties/>\n" +
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>\${groupId.version.property}</artifactId>\n" +
            "      <version>\${groupId.version.property}</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>"
        )

        Truth.assertThat(document.dependencies).hasSize(1)
    }

    @Test fun pom_usingProperties_shouldResolve() {
        val document = loadDocument(
            "  <properties>\n" +
            "    <groupId.version.property>1.0.0</groupId.version.property>\n" +
            "    <groupId.artifactId.property>supportArtifact</groupId.artifactId.property>\n" +
            "  </properties>\n" +
            "  <dependencies>\n" +
            "    <dependency>\n" +
            "      <groupId>supportGroup</groupId>\n" +
            "      <artifactId>\${groupId.artifactId.property}</artifactId>\n" +
            "      <version>\${groupId.version.property}</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>"
        )

        Truth.assertThat(document.dependencies).hasSize(1)

        val dependency = document.dependencies.first()
        Truth.assertThat(dependency.version).isEqualTo("1.0.0")
        Truth.assertThat(dependency.artifactId).isEqualTo("supportArtifact")
    }


    private fun testRewriteToTheSame(givenAndExpectedXml: String, rules: List<PomRewriteRule>) {
        testRewrite(givenAndExpectedXml, givenAndExpectedXml, rules)
    }

    private fun testRewrite(givenXml: String, expectedXml : String, rules: List<PomRewriteRule>) {
        val given =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <!-- Some comment -->\n" +
            "  <groupId>test.group</groupId>\n" +
            "  <artifactId>test.artifact.id</artifactId>\n" +
            "  <version>1.0</version>\n" +
            "  $givenXml\n" +
            "</project>\n"

        var expected =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <!-- Some comment -->\n" +
            "  <groupId>test.group</groupId>\n" +
            "  <artifactId>test.artifact.id</artifactId>\n" +
            "  <version>1.0</version>\n" +
            "  $expectedXml\n" +
            "</project>\n"

        val file = ArchiveFile(Paths.get("pom.xml"), given.toByteArray())
        val pomDocument = PomDocument.loadFrom(file)
        pomDocument.applyRules(rules)
        pomDocument.saveBackToFileIfNeeded()
        var strResult = file.data.toString(StandardCharsets.UTF_8)

        // Remove spaces in front of '<' and the back of '>'
        expected = expected.replace(">[ ]+".toRegex(), ">")
        expected = expected.replace("[ ]+<".toRegex(), "<")

        strResult = strResult.replace(">[ ]+".toRegex(), ">")
        strResult = strResult.replace("[ ]+<".toRegex(), "<")

        // Replace newline characters to match the ones we are using in the expected string
        strResult = strResult.replace("\\r\\n".toRegex(), "\n")

        Truth.assertThat(strResult).isEqualTo(expected)
    }

    private fun loadDocument(givenXml : String) : PomDocument {
        val given =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "  <!-- Some comment -->\n" +
            "  <groupId>test.group</groupId>\n" +
            "  <artifactId>test.artifact.id</artifactId>\n" +
            "  <version>1.0</version>\n" +
            "  $givenXml\n" +
            "</project>\n"

        val file = ArchiveFile(Paths.get("pom.xml"), given.toByteArray())
        val pomDocument = PomDocument.loadFrom(file)
        return pomDocument
    }
}

