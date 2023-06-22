/*
 * Copyright 2010 the original author or authors.
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

package androidx.build.testutils;

import org.gradle.api.Action;
import org.gradle.api.XmlProvider;
import org.gradle.api.internal.DomNode;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.UncheckedException;
import org.gradle.util.internal.GUtil;
import org.gradle.util.internal.TextUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import groovy.util.IndentPrinter;
import groovy.util.Node;
import groovy.xml.XmlNodePrinter;
import groovy.xml.XmlParser;

/**
 * Test fixture for Gradle's XmlProvider.
 * <p>
 * Adapted from org.gradle.internal.xml.XmlTransformer.java in the Android Studio repo.
 */
@SuppressWarnings({"NullableProblems", "unused"})
public class XmlProviderImpl implements XmlProvider {
    private final String indentation = "  ";

    private StringBuilder builder;
    private Node node;
    private String stringValue;
    private Element element;
    private String publicId;
    private String systemId;

    public XmlProviderImpl(String original) {
        this.stringValue = original;
    }

    public XmlProviderImpl(Node original) {
        this.node = original;
    }

    public XmlProviderImpl(DomNode original) {
        this.node = original;
        publicId = original.getPublicId();
        systemId = original.getSystemId();
    }

    public void apply(Iterable<Action<? super XmlProvider>> actions) {
        for (Action<? super XmlProvider> action : actions) {
            action.execute(this);
        }
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        writeTo(writer);
        return writer.toString();
    }

    public void writeTo(Writer writer) {
        doWriteTo(writer, null);
    }

    public void writeTo(Writer writer, String encoding) {
        doWriteTo(writer, encoding);
    }

    public void writeTo(File file) {
        try (OutputStream outputStream = new BufferedOutputStream(
                Files.newOutputStream(file.toPath()))) {
            writeTo(outputStream);
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    public void writeTo(OutputStream stream) {
        try(Writer writer = new BufferedWriter(new OutputStreamWriter(
                stream, StandardCharsets.UTF_8))) {
            doWriteTo(writer, "UTF-8");
            writer.flush();
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    @Override
    public StringBuilder asString() {
        if (builder == null) {
            builder = new StringBuilder(toString());
            node = null;
            element = null;
        }
        return builder;
    }

    @Override
    public Node asNode() {
        if (node == null) {
            try {
                node = new XmlParser().parseText(toString());
            } catch (Exception e) {
                throw UncheckedException.throwAsUncheckedException(e);
            }
            builder = null;
            element = null;
        }
        return node;
    }

    @Override
    public Element asElement() {
        if (element == null) {
            Document document;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new InputSource(new StringReader(toString())));
            } catch (Exception e) {
                throw UncheckedException.throwAsUncheckedException(e);
            }
            element = document.getDocumentElement();
            builder = null;
            node = null;
        }
        return element;
    }

    private void doWriteTo(Writer writer, String encoding) {
        writeXmlDeclaration(writer, encoding);

        try {
            if (node != null) {
                printNode(node, writer);
            } else if (element != null) {
                printDomNode(element, writer);
            } else if (builder != null) {
                writer.append(TextUtil.toPlatformLineSeparators(stripXmlDeclaration(builder)));
            } else {
                writer.append(TextUtil.toPlatformLineSeparators(stripXmlDeclaration(stringValue)));
            }
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private void printNode(Node node, Writer writer) {
        final PrintWriter printWriter = new PrintWriter(writer);
        if (GUtil.isTrue(publicId)) {
            printWriter.format("<!DOCTYPE %s PUBLIC \"%s\" \"%s\">%n", node.name(), publicId,
                    systemId);
        }
        IndentPrinter indentPrinter = new IndentPrinter(printWriter, indentation) {
            @Override
            public void println() {
                printWriter.println();
            }

            @Override
            public void flush() {
                // for performance, ignore flushes
            }
        };
        XmlNodePrinter nodePrinter = new XmlNodePrinter(indentPrinter);
        nodePrinter.setPreserveWhitespace(true);
        nodePrinter.print(node);
        printWriter.flush();
    }

    private void printDomNode(org.w3c.dom.Node node, Writer destination) {
        removeEmptyTextNodes(node); // empty text nodes hinder subsequent formatting
        int indentAmount = determineIndentAmount();

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                factory.setAttribute("indent-number", indentAmount);
            } catch (IllegalArgumentException ignored) {
                /* unsupported by this transformer */
            }

            javax.xml.transform.Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            if (GUtil.isTrue(publicId)) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
            }
            try {
                // some impls support this but not factory.setAttribute("indent-number")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                        String.valueOf(indentAmount));
            } catch (IllegalArgumentException ignored) {
                /* unsupported by this transformer */
            }

            transformer.transform(new DOMSource(node), new StreamResult(destination));
        } catch (TransformerException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private int determineIndentAmount() {
        return indentation.length(); // assume indentation uses spaces
    }

    private void removeEmptyTextNodes(org.w3c.dom.Node node) {
        org.w3c.dom.NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE
                    && child.getNodeValue().trim().length() == 0) {
                node.removeChild(child);
                i--;
            } else {
                removeEmptyTextNodes(child);
            }
        }
    }

    private void writeXmlDeclaration(Writer writer, String encoding) {
        try {
            writer.write("<?xml version=\"1.0\"");
            if (encoding != null) {
                writer.write(" encoding=\"");
                writer.write(encoding);
                writer.write("\"");
            }
            writer.write("?>");
            writer.write(SystemProperties.getInstance().getLineSeparator());
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }
    private boolean hasXmlDeclaration(String xml) {
        // XML declarations must be located at first position of first line
        return xml.startsWith("<?xml");
    }

    private String stripXmlDeclaration(CharSequence sequence) {
        String str = sequence.toString();
        if (hasXmlDeclaration(str)) {
            str = str.substring(str.indexOf("?>") + 2);
            str = str.stripLeading();
        }
        return str;
    }
}
