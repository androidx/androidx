/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.serialization.yaml;

import androidx.compose.remote.serialization.AbstractArraySerializer;
import androidx.compose.remote.serialization.AbstractMapSerializer;
import androidx.compose.remote.serialization.AbstractSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;

public class YAMLSerializer extends AbstractSerializer {

    @Override
    public AbstractArraySerializer newArraySerializer() {
        return new YAMLArraySerializer();
    }

    @Override
    public AbstractMapSerializer newMapSerializer() {
        return new YAMLMapSerializer();
    }

    @Override
    public AbstractSerializer newSerializer() {
        return new YAMLSerializer();
    }

    @Nullable Object toObject() {
        switch (getValueType()) {
            case ARRAY:
                YAMLArraySerializer arraySerializer = (YAMLArraySerializer) mValue;
                if (arraySerializer == null) {
                    throw new RuntimeException("Not a YAMLArraySerializer");
                }
                return arraySerializer.toList();
            case MAP:
                YAMLMapSerializer mapSerializer = (YAMLMapSerializer) mValue;
                if (mapSerializer == null) {
                    throw new RuntimeException("Not a YAMLMapSerializer");
                }
                return mapSerializer.toMap();
            case STRING:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case BOOLEAN:
                return mValue;
            case NULL:
                return null;
            default:
                throw new RuntimeException("YAML ValueType not recognized");
        }
    }

    /**
     * return simple string
     *
     * @return
     */
    public String toSimpleString() {
        return yamlToString(toString());
    }

    /**
     * return flat string
     *
     * @return
     */
    public String toFlatString() {
        return yamlToFlatString(toString());
    }

    @NonNull
    @Override
    public String toString() {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        return yaml.dump(toObject());
    }

    /**
     * Visits each node of a YAML structure and converts it into a formatted string.
     *
     * @param yamlContent The YAML content as a String.
     * @return A formatted string representation of the YAML structure.
     */
    private String yamlToString(String yamlContent) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Node node = yaml.compose(new StringReader(yamlContent));
        if (node == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        visitNode(node, 0, stringBuilder);
        return stringBuilder.toString();
    }

    HashSet<String> mSkipList = new HashSet<>(Arrays.asList("type", "value", "operations"));
    HashSet<String> mCommandList = new HashSet<>(Arrays.asList("TextData", "value"));

    private void visitNode(Node node, int indent, StringBuilder stringBuilder) {
        boolean typeFlag = false;
        String indentation = "  ".repeat(indent);
        if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple tuple : mappingNode.getValue()) {
                Node keyNode = tuple.getKeyNode();
                Node valueNode = tuple.getValueNode();

                // Append the key
                stringBuilder.append(indentation);
                // We assume keys are scalars for this basic formatting
                if (keyNode instanceof ScalarNode) {
                    String t = ((ScalarNode) keyNode).getValue();
                    typeFlag = (mSkipList.contains(t));
                    if (!typeFlag) {
                        stringBuilder.append(t).append(": ");
                    }
                }

                // If the value is a scalar, print it on the same line.
                // Otherwise, start a new line before visiting the next node.
                if (valueNode instanceof ScalarNode) {
                    visitNode(valueNode, 0, stringBuilder); // No extra indent for same-line scalar
                    stringBuilder.append("\n");
                } else {
                    // stringBuilder.append("\n");
                    visitNode(valueNode, indent + 1, stringBuilder);
                }
            }
        } else if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            for (Node itemNode : sequenceNode.getValue()) {
                // stringBuilder.append(indentation).append("- ");
                // If the item is a scalar, print it on the same line.
                // Otherwise, start a new line before visiting the next node.
                if (itemNode instanceof ScalarNode) {
                    visitNode(itemNode, 0, stringBuilder); // No extra indent for same-line scalar
                    stringBuilder.append("\n");
                } else {
                    // stringBuilder.append("\n");
                    visitNode(itemNode, indent + 1, stringBuilder);
                }
            }
        } else if (node instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) node;
            stringBuilder.append(scalarNode.getValue());
        }
    }

    /**
     * Visits each node of a YAML structure and converts it into a formatted string.
     *
     * @param yamlContent The YAML content as a String.
     * @return A formatted string representation of the YAML structure.
     */
    private String yamlToFlatString(String yamlContent) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Node node = yaml.compose(new StringReader(yamlContent));
        if (node == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        visitNodeFlat(node, 0, stringBuilder);
        return stringBuilder.toString();
    }

    private void visitNodeFlat(Node node, int indent, StringBuilder stringBuilder) {
        boolean typeFlag = false;

        String indentation = "  ".repeat(indent);
        if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple tuple : mappingNode.getValue()) {
                Node keyNode = tuple.getKeyNode();
                Node valueNode = tuple.getValueNode();
                if (keyNode instanceof ScalarNode) {
                    String key = ((ScalarNode) keyNode).getValue();
                    if ("type".equals(key)) {

                        if (valueNode instanceof ScalarNode) {
                            String value = ((ScalarNode) valueNode).getValue();
                            if ("Value".equals(value)) {
                                stringBuilder.append(" ");
                            } else {
                                stringBuilder.append("\n");
                            }
                        }
                    }
                }
                // Append the key
                stringBuilder.append(indentation);
                // We assume keys are scalars for this basic formatting
                if (keyNode instanceof ScalarNode) {
                    String t = ((ScalarNode) keyNode).getValue();
                    if (!"Value".equals(t)) {
                        typeFlag = (mSkipList.contains(t));
                        if (!typeFlag) {
                            stringBuilder.append(t).append("= ");
                        }
                    }
                }

                // If the value is a scalar, print it on the same line.
                // Otherwise, start a new line before visiting the next node.
                if (valueNode instanceof ScalarNode) {
                    visitNodeFlat(
                            valueNode, 0, stringBuilder); // No extra indent for same-line scalar
                    if (!"Value".equals(((ScalarNode) valueNode).getValue())) {
                        stringBuilder.append(" ");
                    }
                } else {
                    // stringBuilder.append("\n");
                    visitNodeFlat(valueNode, indent, stringBuilder);
                }
            }
        } else if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            for (Node itemNode : sequenceNode.getValue()) {
                // stringBuilder.append(indentation).append("- ");
                // If the item is a scalar, print it on the same line.
                // Otherwise, start a new line before visiting the next node.
                if (itemNode instanceof ScalarNode) {
                    visitNodeFlat(
                            itemNode, 0, stringBuilder); // No extra indent for same-line scalar
                    stringBuilder.append(" ");
                } else {
                    // stringBuilder.append("\n");
                    visitNodeFlat(itemNode, indent + 1, stringBuilder);
                }
            }
        } else if (node instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) node;
            if (!"Value".equals(scalarNode.getValue())) stringBuilder.append(scalarNode.getValue());
        }
    }
}
