package androidx.ui.foundation.diagnostics

import androidx.annotation.CallSuper

/// Defines diagnostics data for a [value].
///
/// [DiagnosticsNode] provides a high quality multi-line string dump via
/// [toStringDeep]. The core members are the [name], [toDescription],
/// [getProperties], [value], and [getChildren]. All other members exist
/// typically to provide hints for how [toStringDeep] and debugging tools should
/// format output.
abstract class DiagnosticsNode(
        /// Label describing the [DiagnosticsNode], typically shown before a separator
        /// (see [showSeparator]).
        ///
        /// The name will be omitted if the [showName] property is false.
        val name: String?,
        /// Hint for how the node should be displaye
        private val style: DiagnosticsTreeStyle?,
        /// Whether the name of the property should be shown when showing the default
        /// view of the tree.
        ///
        /// This could be set to false (hiding the name) if the value's description
        /// will make the name self-evident.
        private val showName: Boolean = true,
        /// Whether to show a separator between [name] and description.
        ///
        /// If false, name and description should be shown with no separation.
        /// `:` is typically used as a separator when displaying as text.
        val showSeparator: Boolean = true
) {

    companion object {
        /// Diagnostics containing just a string `message` and not a concrete name or
        /// value.
        ///
        /// The [style] and [level] arguments must not be null.
        ///
        /// See also:
        ///
        ///  * [MessageProperty], which is better suited to messages that are to be
        ///    formatted like a property with a separate name and message.
        fun message(
                message: String,
                style: DiagnosticsTreeStyle = DiagnosticsTreeStyle.singleLine,
                level: DiagnosticLevel = DiagnosticLevel.info
        ): DiagnosticsProperty<Any> {
            return DiagnosticsProperty.create<Any>(
                    name = "",
                    value = null,
                    description = message,
                    style = style,
                    showName = false,
                    level = level
            )
        }
    }

    init {
        // A name ending with ':' indicates that the user forgot that the ':' will
        // be automatically added for them when generating descriptions of the
        // property.
        assert(name == null || !name.endsWith(':')) { "Names of diagnostic nodes must not end with colons." }
    }


    /// Returns a description with a short summary of the node itself not
    /// including children or properties.
    ///
    /// `parentConfiguration` specifies how the parent is rendered as text art.
    /// For example, if the parent does not line break between properties, the
    /// description of a property should also be a single line if possible.
    abstract fun toDescription(parentConfiguration: TextTreeConfiguration? = null): String


    /// Whether the diagnostic should be filtered due to its [level] being lower
    /// than `minLevel`.
    ///
    /// If `minLevel` is [DiagnosticLevel.hidden] no diagnostics will be filtered.
    /// If `minLevel` is [DiagnosticsLevel.off] all diagnostics will be filtered.
    fun isFiltered(minLevel: DiagnosticLevel): Boolean = getLevel() < minLevel

    /// Priority level of the diagnostic used to control which diagnostics should
    /// be shown and filtered.
    ///
    /// Typically this only makes sense to set to a different value than
    /// [DiagnosticLevel.info] for diagnostics representing properties. Some
    /// subclasses have a `level` argument to their constructor which influences
    /// the value returned here but other factors also influence it. For example,
    /// whether an exception is thrown computing a property value
    /// [DiagnosticLevel.error] is returned.
    open fun getLevel(): DiagnosticLevel = DiagnosticLevel.info

    open fun getShowName(): Boolean = showName

    open fun getStyle(): DiagnosticsTreeStyle? = style

    /// Description to show if the node has no displayed properties or children.
    open fun getEmptyBodyDescription(): String? = null

    /// The actual object this is diagnostics data for.

    abstract fun getValue(): Any?


    /// Properties of this [DiagnosticsNode].
    ///
    /// Properties and children are kept distinct even though they are both
    /// [List<DiagnosticsNode>] because they should be grouped differently.
    abstract fun getProperties(): List<DiagnosticsNode>;

    /// Children of this [DiagnosticsNode].
    ///
    /// See also:
    ///
    ///  * [getProperties]
    abstract fun getChildren(): List<DiagnosticsNode>;

    private val separator: String = if (showSeparator)  ":" else ""

    /// Serialize the node excluding its descendents to a JSON map.
    ///
    /// Subclasses should override if they have additional properties that are
    /// useful for the GUI tools that consume this JSON.
    ///
    /// See also:
    ///
    ///  * [WidgetInspectorService], which forms the bridge between JSON returned
    ///    by this method and interactive tree views in the Flutter IntelliJ
    ///    plugin.
    @CallSuper
    open fun toJsonMap(): Map<String, Any> {
        // // TODO(Migration/Filip): Added several .orEmpty to satisfy non-nullability
        return mapOf(
            "name" to name.orEmpty(),
            "showSeparator" to showSeparator,
            "description" to toDescription(),
            "level" to getLevel().toString(),
            "showName" to showName,
            "emptyBodyDescription" to getEmptyBodyDescription().orEmpty(),
            "style" to style.toString(),
            "valueToString" to getValue().toString(),
            "type" to this::class.java.toString(), // TODO(Migration/Filip): Not sure if this is it
            "hasChildren" to getChildren().isNotEmpty()
        );
    }

    /// Returns a string representation of this diagnostic that is compatible with
    /// the style of the parent if the node is not the root.
    ///
    /// `parentConfiguration` specifies how the parent is rendered as text art.
    /// For example, if the parent places all properties on one line, the
    /// [toString] for each property should avoid line breaks if possible.
    ///
    /// `minLevel` specifies the minimum [DiagnosticLevel] for properties included
    /// in the output.
    open fun toStringParametrized(
            parentConfiguration: TextTreeConfiguration? = null,
            minLevel: DiagnosticLevel = DiagnosticLevel.info
    ): String {
        assert(style != null);
        assert(minLevel != null);
        if (style == DiagnosticsTreeStyle.singleLine)
            return toStringDeep(parentConfiguration = parentConfiguration, minLevel = minLevel);

        val description = toDescription(parentConfiguration = parentConfiguration);

        if (name == null || name.isEmpty() || !showName)
            return description;

        return if (description.contains("\n")) "$name$separator\n$description" else "$name$separator $description";
    }

    override fun toString(): String {
        return toStringParametrized()
    }

    /// Returns a configuration specifying how this object should be rendered
    /// as text art.
    protected fun getTextTreeConfiguration(): TextTreeConfiguration? {
        assert(style != null)

        when (style) {
            DiagnosticsTreeStyle.dense -> denseTextConfiguration;
            DiagnosticsTreeStyle.sparse -> sparseTextConfiguration;
            DiagnosticsTreeStyle.offstage -> dashedTextConfiguration;
            DiagnosticsTreeStyle.whitespace -> whitespaceTextConfiguration;
            DiagnosticsTreeStyle.transition -> transitionTextConfiguration;
            DiagnosticsTreeStyle.singleLine -> singleLineTextConfiguration;
        }
        return null
    }

    /// Text configuration to use to connect this node to a `child`.
    ///
    /// The singleLine style is special cased because the connection from the
    /// parent to the child should be consistent with the parent's style as the
    /// single line style does not provide any meaningful style for how children
    /// should be connected to their parents.
    private fun _childTextConfiguration(
            child: DiagnosticsNode?,
            textStyle: TextTreeConfiguration
    ): TextTreeConfiguration? {
        return if (child != null && child.style != DiagnosticsTreeStyle.singleLine) {
            child.getTextTreeConfiguration()
        } else {
            textStyle
        }
    }

    /// Returns a string representation of this node and its descendants.
    ///
    /// `prefixLineOne` will be added to the front of the first line of the
    /// output. `prefixOtherLines` will be added to the front of each other line.
    /// If `prefixOtherLines` is null, the `prefixLineOne` is used for every line.
    /// By default, there is no prefix.
    ///
    /// `minLevel` specifies the minimum [DiagnosticLevel] for properties included
    /// in the output.
    ///
    /// The [toStringDeep] method takes other arguments, but those are intended
    /// for internal use when recursing to the descendants, and so can be ignored.
    ///
    /// See also:
    ///
    ///  * [toString], for a brief description of the [value] but not its children.
    ///  * [toStringShallow], for a detailed description of the [value] but not its
    ///    children.
    fun toStringDeep(
            prefixLineOne: String = "",
            prefixOtherLines: String? = null,
            parentConfiguration: TextTreeConfiguration? = null,
            minLevel: DiagnosticLevel = DiagnosticLevel.debug
    ): String {
        assert(minLevel != null)
        var prefixOtherLines2 = prefixOtherLines ?: prefixLineOne;

        val children = getChildren();
        val config = getTextTreeConfiguration()!!;
        if (prefixOtherLines2.isEmpty())
            prefixOtherLines2 += config.prefixOtherLinesRootNode;

        val builder = _PrefixedStringBuilder(
                prefixLineOne,
                prefixOtherLines2
        );

        val description = toDescription(parentConfiguration = parentConfiguration);
        if (description == null || description.isEmpty()) {
            if (showName && name != null)
                builder.write(name);
        } else {
            if (name != null && name.isNotEmpty() && showName) {
                builder.write(name);
                if (showSeparator)
                    builder.write(config.afterName);

                builder.write(
                        if(config.isNameOnOwnLine ||description.contains("\n")) "\n" else " ");
                if (description.contains('\n') && style == DiagnosticsTreeStyle.singleLine)
                    builder.prefixOtherLines += "  ";
            }
            builder.prefixOtherLines += if (children.isEmpty()) {
                config.propertyPrefixNoChildren
            }
           else  {
                config.propertyPrefixIfChildren;
            }
            builder.write(description);
        }

        val properties = getProperties().filter { !it.isFiltered(minLevel) }.toList()

        if (properties.isNotEmpty() || children.isNotEmpty() || getEmptyBodyDescription() != null)
            builder.write(config.afterDescriptionIfBody);

        if (config.lineBreakProperties)
            builder.write(config.lineBreak);

        if (properties.isNotEmpty())
            builder.write(config.beforeProperties);

        builder.prefixOtherLines += config.bodyIndent;

        if (getEmptyBodyDescription() != null &&
                properties.isEmpty() &&
                children.isEmpty() &&
                prefixLineOne.isNotEmpty()) {
            builder.write(getEmptyBodyDescription().orEmpty());
            if (config.lineBreakProperties)
                builder.write(config.lineBreak);
        }

        for ( i in 0..properties.size) {
            val property = properties[i];
            if (i > 0)
                builder.write(config.propertySeparator);

            val kWrapWidth = 65;
            if (property.style != DiagnosticsTreeStyle.singleLine) {
                val propertyStyle = property.getTextTreeConfiguration();
                builder.writeRaw(
                        property.toStringDeep(
                                prefixLineOne = "${builder.prefixOtherLines}${propertyStyle!!.prefixLineOne}",
                                prefixOtherLines = "${builder.prefixOtherLines}${propertyStyle.linkCharacter}${propertyStyle.prefixOtherLines}",
                                parentConfiguration =  config,
                                minLevel =  minLevel
                ))
                continue
            }
            assert(property.style == DiagnosticsTreeStyle.singleLine);
            val message = property.toStringParametrized(parentConfiguration = config, minLevel = minLevel);
            if (!config.lineBreakProperties || message.length < kWrapWidth) {
                builder.write(message);
            } else {
                // debugWordWrap doesn't handle line breaks within the text being
                // wrapped so we must call it on each line.
                val lines = message.split('\n');
                for (j in 0..lines.size) {
                    val line = lines[j];
                    if (j > 0)
                        builder.write(config.lineBreak);
                    builder.write(debugWordWrap(line, kWrapWidth, wrapIndent = "  ").joinToString(separator = "\n"));
                }

                for (j in 0..lines.size) {
                    val line = lines[j];
                    if (j > 0)
                        builder.write(config.lineBreak);
                    builder.write(debugWordWrap(line, kWrapWidth, wrapIndent = "  ").joinToString(separator = "\n"));
                }
            }
            if (config.lineBreakProperties)
                builder.write(config.lineBreak);
        }
        if (properties.isNotEmpty())
            builder.write(config.afterProperties);

        if (!config.lineBreakProperties)
            builder.write(config.lineBreak);

        val prefixChildren = "$prefixOtherLines2${config.bodyIndent}";

        if (children.isEmpty() &&
                config.addBlankLineIfNoChildren &&
                builder.hasMultipleLines) {
            val prefix = prefixChildren.trimEnd();
            if (prefix.isNotEmpty())
                builder.writeRaw("$prefix${config.lineBreak}");
        }

        if (children.isNotEmpty() && config.showChildren) {
            if (config.isBlankLineBetweenPropertiesAndChildren &&
                    properties.isNotEmpty() &&
                    children.first().getTextTreeConfiguration()!!.isBlankLineBetweenPropertiesAndChildren) {
                builder.write(config.lineBreak);
            }

            for (i in 0..children.size) {
                val child = children[i];
                assert(child != null);
                val childConfig = _childTextConfiguration(child, config)!!;
                if (i == children.size - 1) {
                    val lastChildPrefixLineOne = "$prefixChildren${childConfig.prefixLastChildLineOne}";
                    builder.writeRawLine(child.toStringDeep(
                            prefixLineOne = lastChildPrefixLineOne,
                            prefixOtherLines = "$prefixChildren${childConfig.childLinkSpace}${childConfig.prefixOtherLines}",
                            parentConfiguration = config,
                            minLevel = minLevel
                    ));
                    if (childConfig.footer.isNotEmpty())
                        builder.writeRaw("$prefixChildren${childConfig.childLinkSpace}${childConfig.footer}");
                } else {
                    val nextChildStyle = _childTextConfiguration(children[i + 1], config)!!;
                    val childPrefixLineOne = "$prefixChildren${childConfig.prefixLineOne}";
                    val childPrefixOtherLines = "$prefixChildren${nextChildStyle.linkCharacter}${childConfig.prefixOtherLines}";
                    builder.writeRawLine(child.toStringDeep(
                            prefixLineOne = childPrefixLineOne,
                            prefixOtherLines = childPrefixOtherLines,
                            parentConfiguration = config,
                            minLevel = minLevel
                    ));
                    if (childConfig.footer.isNotEmpty())
                        builder.writeRaw("$prefixChildren${nextChildStyle.linkCharacter}${childConfig.footer}");
                }
            }
        }
        return builder.toString();
    }
}