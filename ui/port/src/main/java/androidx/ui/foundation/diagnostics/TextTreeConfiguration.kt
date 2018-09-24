package androidx.ui.foundation.diagnostics

/**
 * Configuration specifying how a particular [DiagnosticsTreeStyle] should be
 * rendered as text art.
 *
 * See also:
 *
 *  * [sparseTextConfiguration], which is a typical style.
 *  * [transitionTextConfiguration], which is an example of a complex tree style.
 *  * [DiagnosticsNode.toStringDeep], for code using [TextTreeConfiguration]
 *    to render text art for arbitrary trees of [DiagnosticsNode] objects.
 */
data class TextTreeConfiguration(
    /** Prefix to add to the first line to display a child with this style. */
    val prefixLineOne: String,
    /**
     * Prefix to add to other lines to display a child with this style.
     *
     * [prefixOtherLines] should typically be one character shorter than
     * [prefixLineOne] as
     */
    val prefixOtherLines: String,
    /**
     * Prefix to add to the first line to display the last child of a node with
     * this style.
     */
    val prefixLastChildLineOne: String,
    /**
     * Additional prefix to add to other lines of a node if this is the root node
     * of the tree.
     */
    val prefixOtherLinesRootNode: String,
    /**
     * Character to use to draw line linking parent to child.
     *
     * The first child does not require a line but all subsequent children do
     * with the line drawn immediately before the left edge of the previous
     * sibling.
     */
    val linkCharacter: String,
    /**
     * Prefix to add before each property if the node as children.
     *
     * Plays a similar role to [linkCharacter] except that some configurations
     * intentionally use a different line style than the [linkCharacter].
     */
    val propertyPrefixIfChildren: String,
    /**
     * Prefix to add before each property if the node does not have children.
     *
     * This string is typically a whitespace string the same length as
     * [propertyPrefixIfChildren] but can have a different length.
     */
    val propertyPrefixNoChildren: String,
    /**
     * Character(s) to use to separate lines.
     *
     * Typically leave set at the default value of '\n' unless this style needs
     * to treat lines differently as is the case for
     * [singleLineTextConfiguration].
     */
    val lineBreak: String = "\n",
    /**
     * Whether to place line breaks between properties or to leave all
     * properties on one line.
     */
    val lineBreakProperties: Boolean = true,
    /**
     * Text added immediately after the name of the node.
     *
     * See [transitionTextConfiguration] for an example of using a value other
     * than ':' to achieve a custom line art style.
     */
    val afterName: String = ":",
    /**
     * Text to add immediately after the description line of a node with
     * properties and/or children.
     */
    val afterDescriptionIfBody: String = "",
    /**
     * Optional string to add before the properties of a node.
     *
     * Only displayed if the node has properties.
     * See [singleLineTextConfiguration] for an example of using this field
     * to enclose the property list with parenthesis.
     */
    val beforeProperties: String = "",
    /**
     * Optional string to add after the properties of a node.
     *
     * See documentation for [beforeProperties].
     */
    val afterProperties: String = "",
    /**
     * Property separator to add between properties.
     *
     * See [singleLineTextConfiguration] for an example of using this field
     * to render properties as a comma separated list.
     */
    val propertySeparator: String = "",
    /**
     * Prefix to add to all lines of the body of the tree node.
     *
     * The body is all content in the node other than the name and description.
     */
    val bodyIndent: String = "",
    /**
     * Footer to add as its own line at the end of a non-root node.
     *
     * See [transitionTextConfiguration] for an example of using footer to draw a box
     * around the node. [footer] is indented the same amount as [prefixOtherLines].
     */
    val footer: String = "",
    /**
     * Whether the children of a node should be shown.
     *
     * See [singleLineTextConfiguration] for an example of using this field to
     * hide all children of a node.
     */
    val showChildren: Boolean = true,
    /**
     * Whether to add a blank line at the end of the output for a node if it has
     * no children.
     *
     * See [denseTextConfiguration] for an example of setting this to false.
     */
    val addBlankLineIfNoChildren: Boolean = true,
    /** Whether the name should be displayed on the same line as the description. */
    val isNameOnOwnLine: Boolean = false,
    /** Add a blank line between properties and children if both are present. */
    val isBlankLineBetweenPropertiesAndChildren: Boolean = true
) {

    /**
     * Whitespace to draw instead of the childLink character if this node is the
     * last child of its parent so no link line is required.
     */
    val childLinkSpace: String = " ".repeat(linkCharacter.length)
}

/**
 * Default text tree configuration.
 *
 * Example:
 * ```
 * <root_name>: <root_description>
 *  │ <property1>
 *  │ <property2>
 *  │ ...
 *  │ <propertyN>
 *  ├─<child_name>: <child_description>
 *  │ │ <property1>
 *  │ │ <property2>
 *  │ │ ...
 *  │ │ <propertyN>
 *  │ │
 *  │ └─<child_name>: <child_description>
 *  │     <property1>
 *  │     <property2>
 *  │     ...
 *  │     <propertyN>
 *  │
 *  └─<child_name>: <child_description>"
 *    <property1>
 *    <property2>
 *    ...
 *    <propertyN>
 * ```
 *
 * See also:
 *
 *  * [DiagnosticsTreeStyle.sparse]
 */
val sparseTextConfiguration = TextTreeConfiguration(
        prefixLineOne = "├─",
        prefixOtherLines = " ",
        prefixLastChildLineOne = "└─",
        linkCharacter = "│",
        propertyPrefixIfChildren = "│ ",
        propertyPrefixNoChildren = "  ",
        prefixOtherLinesRootNode = " "
)

/**
 * Identical to [sparseTextConfiguration] except that the lines connecting
 * parent to children are dashed.
 *
 * Example:
 * ```
 * <root_name>: <root_description>
 *  │ <property1>
 *  │ <property2>
 *  │ ...
 *  │ <propertyN>
 *  ├─<normal_child_name>: <child_description>
 *  ╎ │ <property1>
 *  ╎ │ <property2>
 *  ╎ │ ...
 *  ╎ │ <propertyN>
 *  ╎ │
 *  ╎ └─<child_name>: <child_description>
 *  ╎     <property1>
 *  ╎     <property2>
 *  ╎     ...
 *  ╎     <propertyN>
 *  ╎
 *  ╎╌<dashed_child_name>: <child_description>
 *  ╎ │ <property1>
 *  ╎ │ <property2>
 *  ╎ │ ...
 *  ╎ │ <propertyN>
 *  ╎ │
 *  ╎ └─<child_name>: <child_description>
 *  ╎     <property1>
 *  ╎     <property2>
 *  ╎     ...
 *  ╎     <propertyN>
 *  ╎
 *  └╌<dashed_child_name>: <child_description>"
 *    <property1>
 *    <property2>
 *    ...
 *    <propertyN>
 * ```
 *
 * See also:
 *
 *  * [DiagnosticsTreeStyle.offstage], uses this style for ASCII art display.
 */
val dashedTextConfiguration = TextTreeConfiguration(
        prefixLineOne = "╎╌",
        prefixLastChildLineOne = "└╌",
        prefixOtherLines = " ",
        linkCharacter = "╎",
        // Intentionally not set as a dashed line as that would make the properties
        // look like they were disabled.
        propertyPrefixIfChildren = "│ ",
        propertyPrefixNoChildren = "  ",
        prefixOtherLinesRootNode = " "
)

/**
 * Dense text tree configuration that minimizes horizontal whitespace.
 *
 * Example:
 * ```
 * <root_name>: <root_description>(<property1>; <property2> <propertyN>)
 * ├<child_name>: <child_description>(<property1>, <property2>, <propertyN>)
 * └<child_name>: <child_description>(<property1>, <property2>, <propertyN>)
 * ```
 *
 * See also:
 *
 *  * [DiagnosticsTreeStyle.dense]
 */
val denseTextConfiguration = TextTreeConfiguration(
        propertySeparator = ", ",
        beforeProperties = "(",
        afterProperties = ")",
        lineBreakProperties = false,
        prefixLineOne = "├",
        prefixOtherLines = "",
        prefixLastChildLineOne = "└",
        linkCharacter = "│",
        propertyPrefixIfChildren = "│",
        propertyPrefixNoChildren = " ",
        prefixOtherLinesRootNode = "",
        addBlankLineIfNoChildren = false,
        isBlankLineBetweenPropertiesAndChildren = false
)

/**
 * Configuration that draws a box around a leaf node.
 *
 * Used by leaf nodes such as [TextSpan] to draw a clear border around the
 * contents of a node.
 *
 * Example:
 * ```
 *  <parent_node>
 *  ╞═╦══ <name> ═══
 *  │ ║  <description>:
 *  │ ║    <body>
 *  │ ║    ...
 *  │ ╚═══════════
 *  ╘═╦══ <name> ═══
 *    ║  <description>:
 *    ║    <body>
 *    ║    ...
 *    ╚═══════════
 * ```
 *
 * /// See also:
 *
 *  * [DiagnosticsTreeStyle.transition]
 */
val transitionTextConfiguration = TextTreeConfiguration(
        prefixLineOne = "╞═╦══ ",
        prefixLastChildLineOne = "╘═╦══ ",
        prefixOtherLines = " ║ ",
        footer = " ╚═══════════\n",
        linkCharacter = "│",
// Subtree boundaries are clear due to the border around the node so omit the
// property prefix.
        propertyPrefixIfChildren = "",
        propertyPrefixNoChildren = "",
        prefixOtherLinesRootNode = "",
        afterName = " ═══",
// Add a colon after the description if the node has a body to make the
// connection between the description and the body clearer.
        afterDescriptionIfBody = ":",
// Members are indented an extra two spaces to disambiguate as the description
// is placed within the box instead of along side the name as is the case for
// other styles.
        bodyIndent = "  ",
        isNameOnOwnLine = true,
// No need to add a blank line as the footer makes the boundary of this
// subtree unambiguous.
        addBlankLineIfNoChildren = false,
        isBlankLineBetweenPropertiesAndChildren = false
)

/**
 * Whitespace only configuration where children are consistently indented
 * two spaces.
 *
 * Use this style for displaying properties with structured values or for
 * displaying children within a [transitionTextConfiguration] as using a style that
 * draws line art would be visually distracting for those cases.
 *
 * Example:
 * ```
 * <parent_node>
 *   <name>: <description>:
 *     <properties>
 *     <children>
 *   <name>: <description>:
 *     <properties>
 *     <children>
 *```
 *
 * See also:
 *
 *  * [DiagnosticsTreeStyle.whitespace]
 */
val whitespaceTextConfiguration = TextTreeConfiguration(
        prefixLineOne = "",
        prefixLastChildLineOne = "",
        prefixOtherLines = " ",
        prefixOtherLinesRootNode = "  ",
        bodyIndent = "",
        propertyPrefixIfChildren = "",
        propertyPrefixNoChildren = "",
        linkCharacter = " ",
        addBlankLineIfNoChildren = false,
        // Add a colon after the description and before the properties to link the
        // properties to the description line.
        afterDescriptionIfBody = ":",
        isBlankLineBetweenPropertiesAndChildren = false
)

/**
 * Render a node as a single line omitting children.
 *
 * Example:
 * `<name>: <description>(<property1>, <property2>, ..., <propertyN>)`
 *
 * See also:
 *
 *   * [DiagnosticsTreeStyle.singleLine]
 */
val singleLineTextConfiguration = TextTreeConfiguration(
        propertySeparator = ", ",
        beforeProperties = "(",
        afterProperties = ")",
        prefixLineOne = "",
        prefixOtherLines = "",
        prefixLastChildLineOne = "",
        lineBreak = "",
        lineBreakProperties = false,
        addBlankLineIfNoChildren = false,
        showChildren = false,
        propertyPrefixIfChildren = "",
        propertyPrefixNoChildren = "",
        linkCharacter = "",
        prefixOtherLinesRootNode = ""
)