import androidx.ui.VoidCallback
import androidx.ui.bindings.EnginePhase
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.layout
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderProxyBox
import androidx.ui.rendering.pumpFrame
import androidx.ui.rendering.renderer
import androidx.ui.semantics.DebugSemanticsDumpOrder
import androidx.ui.semantics.MoveCursorHandler
import androidx.ui.semantics.OrdinalSortKey
import androidx.ui.semantics.SemanticsAction
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.semantics.SemanticsNode
import androidx.ui.semantics.SemanticsSortKey
import androidx.ui.semantics.SemanticsTag
import androidx.ui.semantics.debugResetSemanticsIdCounter
import androidx.ui.test.util.assertThrows
import androidx.ui.test.util.normalizeHashCodes
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SemanticsNodeTests {
    val tag1: SemanticsTag = SemanticsTag("Tag One")
    val tag2: SemanticsTag = SemanticsTag("Tag Two")
    val tag3: SemanticsTag = SemanticsTag("Tag Three")

    @Before
    fun setUp() {
        debugResetSemanticsIdCounter()
    }

    @Test
    fun tagging() {
        val node = SemanticsNode()

        assertThat(node.isTagged(tag1)).isFalse()
        assertThat(node.isTagged(tag2)).isFalse()

        node.tags = setOf(tag1)
        assertThat(node.isTagged(tag1)).isTrue()
        assertThat(node.isTagged(tag2)).isFalse()

        node.tags = setOf(tag1, tag2)
        assertThat(node.isTagged(tag1)).isTrue()
        assertThat(node.isTagged(tag2)).isTrue()
    }

    @Test
    fun `getSemanticsData includes tags`() {
        val tags = mutableSetOf(tag1, tag2)

        val node: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 10.0, 10.0)
            it.tags = tags
        }

        assertThat(tags).isEqualTo(node.getSemanticsData().tags)

        tags.add(tag3)

        val config: SemanticsConfiguration = SemanticsConfiguration().also {
            it.isSemanticBoundary = true
            it.isMergingSemanticsOfDescendants = true
        }

        node.updateWith(
            config = config,
            childrenInInversePaintOrder = listOf(
                SemanticsNode().also {
                    it.isMergedIntoParent = true
                    it.rect = Rect.fromLTRB(5.0, 5.0, 10.0, 10.0)
                    it.tags = tags
                }
            )
        )

        assertThat(tags).isEqualTo(node.getSemanticsData().tags)
    }

    /**
     * after markNeedsSemanticsUpdate() all render objects between two semantic boundaries are
     * asked for annotations
     */
    @Test
    @Ignore("TODO(Migration/ryanmentley): Needs native code implemented")
    fun `after markNeedsSemanticsUpdate() queries annotations for objects between boundaries`() {
        renderer.pipelineOwner!!.ensureSemantics()

        var middle: TestRender? = null
        val root = TestRender(
            hasTapAction = true,
            isSemanticBoundary = true,
            child = TestRender(
                hasLongPressAction = true,
                isSemanticBoundary = false,
                child = TestRender(
                    hasScrollLeftAction = true,
                    isSemanticBoundary = false,
                    child = TestRender(
                        hasScrollRightAction = true,
                        isSemanticBoundary = false,
                        child = TestRender(
                            hasScrollUpAction = true,
                            isSemanticBoundary = true
                        )
                    )
                ).also { middle = it }
            )
        )

        layout(root)
        pumpFrame(phase = EnginePhase.flushSemantics)

        var expectedActions = SemanticsAction.tap.index or
                SemanticsAction.longPress.index or
                SemanticsAction.scrollLeft.index or
                SemanticsAction.scrollRight.index

        assertThat(expectedActions).isEqualTo(root.debugSemantics!!.getSemanticsData().actions)

        // Guaranteed non-null by the also block above
        middle!!.apply {
            hasScrollLeftAction = false
            hasScrollDownAction = true
        }

        middle!!.markNeedsSemanticsUpdate()

        pumpFrame(phase = EnginePhase.flushSemantics)

        expectedActions = SemanticsAction.tap.index or
                SemanticsAction.longPress.index or
                SemanticsAction.scrollDown.index or
                SemanticsAction.scrollRight.index
        assertThat(expectedActions).isEqualTo(root.debugSemantics!!.getSemanticsData().actions)
    }

    @Test
    fun `toStringDeep() does not throw with transform == null`() {
        val child1: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 5.0, 5.0)
        }
        val child2: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(5.0, 0.0, 10.0, 5.0)
        }
        val root: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 10.0, 5.0)
        }
        root.updateWith(
            config = null,
            childrenInInversePaintOrder = listOf(child1, child2)
        )

        assertThat(root.transform).isNull()
        assertThat(child1.transform).isNull()
        assertThat(child2.transform).isNull()

        assertThat(root.toStringDeep(childOrder = DebugSemanticsDumpOrder.traversalOrder))
            .isEqualTo(
                "SemanticsNode#3\n" +
                        " │ STALE\n" +
                        " │ owner: null\n" +
                        " │ Rect.fromLTRB(0.0, 0.0, 10.0, 5.0)\n" +
                        " │\n" +
                        " ├─SemanticsNode#1\n" +
                        " │   STALE\n" +
                        " │   owner: null\n" +
                        " │   Rect.fromLTRB(0.0, 0.0, 5.0, 5.0)\n" +
                        " │\n" +
                        " └─SemanticsNode#2\n" +
                        "     STALE\n" +
                        "     owner: null\n" +
                        "     Rect.fromLTRB(5.0, 0.0, 10.0, 5.0)\n"
            )
    }

    @Test
    fun `Incompatible OrdinalSortKey throw AssertionError when compared`() {
        // Different types.
        assertThrows(AssertionError::class) {
            OrdinalSortKey(0.0).compareTo(CustomSortKey(0.0))
        }

        // Different names.
        assertThrows(AssertionError::class) {
            OrdinalSortKey(0.0, name = "a").compareTo(OrdinalSortKey(0.0, name = "b"))
        }
    }

    @Test
    fun `OrdinalSortKey compares correctly`() {
        val tests: List<List<SemanticsSortKey>> = listOf(
            listOf(OrdinalSortKey(0.0), OrdinalSortKey(0.0)),
            listOf(OrdinalSortKey(0.0), OrdinalSortKey(1.0)),
            listOf(OrdinalSortKey(1.0), OrdinalSortKey(0.0)),
            listOf(OrdinalSortKey(1.0), OrdinalSortKey(1.0))
        )
        val expectedResults: List<Int> = listOf(0, -1, 1, 0)

        val results: MutableList<Int> = mutableListOf()
        for (tuple in tests) {
            results.add(tuple[0].compareTo(tuple[1]))
        }
        assertThat(results).containsExactlyElementsIn(expectedResults)
    }

    @Test
    fun `toStringDeep respects childOrder parameter`() {
        val child1: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(15.0, 0.0, 20.0, 5.0)
        }
        val child2: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(10.0, 0.0, 15.0, 5.0)
        }
        val root: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 20.0, 5.0)
        }
        root.updateWith(
            config = null,
            childrenInInversePaintOrder = listOf(child1, child2)
        )
        assertThat(
            root.toStringDeep(childOrder = DebugSemanticsDumpOrder.traversalOrder)
        ).isEqualTo(
            "SemanticsNode#3\n" +
                    " │ STALE\n" +
                    " │ owner: null\n" +
                    " │ Rect.fromLTRB(0.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#1\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(15.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " └─SemanticsNode#2\n" +
                    "     STALE\n" +
                    "     owner: null\n" +
                    "     Rect.fromLTRB(10.0, 0.0, 15.0, 5.0)\n"
        )
        assertThat(
            root.toStringDeep(childOrder = DebugSemanticsDumpOrder.inverseHitTest)
        ).isEqualTo(
            "SemanticsNode#3\n" +
                    " │ STALE\n" +
                    " │ owner: null\n" +
                    " │ Rect.fromLTRB(0.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#1\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(15.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " └─SemanticsNode#2\n" +
                    "     STALE\n" +
                    "     owner: null\n" +
                    "     Rect.fromLTRB(10.0, 0.0, 15.0, 5.0)\n"
        )

        val child3: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 10.0, 5.0)
        }
        child3.updateWith(
            config = null,
            childrenInInversePaintOrder = listOf(
                SemanticsNode().also {
                    it.rect = Rect.fromLTRB(5.0, 0.0, 10.0, 5.0)
                },
                SemanticsNode().also {
                    it.rect = Rect.fromLTRB(0.0, 0.0, 5.0, 5.0)
                }
            )
        )

        val rootComplex: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTRB(0.0, 0.0, 25.0, 5.0)
        }
        rootComplex.updateWith(
            config = null,
            childrenInInversePaintOrder = listOf(child1, child2, child3)
        )

        assertThat(
            rootComplex.toStringDeep(childOrder = DebugSemanticsDumpOrder.traversalOrder)
        ).isEqualTo(
            "SemanticsNode#7\n" +
                    " │ STALE\n" +
                    " │ owner: null\n" +
                    " │ Rect.fromLTRB(0.0, 0.0, 25.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#1\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(15.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#2\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(10.0, 0.0, 15.0, 5.0)\n" +
                    " │\n" +
                    " └─SemanticsNode#4\n" +
                    "   │ STALE\n" +
                    "   │ owner: null\n" +
                    "   │ Rect.fromLTRB(0.0, 0.0, 10.0, 5.0)\n" +
                    "   │\n" +
                    "   ├─SemanticsNode#5\n" +
                    "   │   STALE\n" +
                    "   │   owner: null\n" +
                    "   │   Rect.fromLTRB(5.0, 0.0, 10.0, 5.0)\n" +
                    "   │\n" +
                    "   └─SemanticsNode#6\n" +
                    "       STALE\n" +
                    "       owner: null\n" +
                    "       Rect.fromLTRB(0.0, 0.0, 5.0, 5.0)\n"
        )

        assertThat(
            rootComplex.toStringDeep(childOrder = DebugSemanticsDumpOrder.inverseHitTest)
        ).isEqualTo(
            "SemanticsNode#7\n" +
                    " │ STALE\n" +
                    " │ owner: null\n" +
                    " │ Rect.fromLTRB(0.0, 0.0, 25.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#1\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(15.0, 0.0, 20.0, 5.0)\n" +
                    " │\n" +
                    " ├─SemanticsNode#2\n" +
                    " │   STALE\n" +
                    " │   owner: null\n" +
                    " │   Rect.fromLTRB(10.0, 0.0, 15.0, 5.0)\n" +
                    " │\n" +
                    " └─SemanticsNode#4\n" +
                    "   │ STALE\n" +
                    "   │ owner: null\n" +
                    "   │ Rect.fromLTRB(0.0, 0.0, 10.0, 5.0)\n" +
                    "   │\n" +
                    "   ├─SemanticsNode#5\n" +
                    "   │   STALE\n" +
                    "   │   owner: null\n" +
                    "   │   Rect.fromLTRB(5.0, 0.0, 10.0, 5.0)\n" +
                    "   │\n" +
                    "   └─SemanticsNode#6\n" +
                    "       STALE\n" +
                    "       owner: null\n" +
                    "       Rect.fromLTRB(0.0, 0.0, 5.0, 5.0)\n"
        )
    }

    @Test
    fun `debug properties`() {
        val minimalProperties: SemanticsNode = SemanticsNode()
        assertThat(
            minimalProperties.toStringDeep()
        ).isEqualTo(
            "SemanticsNode#1\n" +
                    "   Rect.fromLTRB(0.0, 0.0, 0.0, 0.0)\n" +
                    "   invisible\n"
        )

        assertThat(
            minimalProperties.toStringDeep(minLevel = DiagnosticLevel.hidden)
        ).isEqualTo(
            "SemanticsNode#1\n" +
                    "   owner: null\n" +
                    "   isMergedIntoParent: false\n" +
                    "   mergeAllDescendantsIntoThisNode: false\n" +
                    "   Rect.fromLTRB(0.0, 0.0, 0.0, 0.0)\n" +
                    "   actions: []\n" +
                    "   flags: []\n" +
                    "   invisible\n" +
                    "   isHidden: false\n" +
                    "   label: \"\"\n" +
                    "   value: \"\"\n" +
                    "   increasedValue: \"\"\n" +
                    "   decreasedValue: \"\"\n" +
                    "   hint: \"\"\n" +
                    "   textDirection: null\n" +
                    "   sortKey: null\n" +
                    "   scrollExtentMin: null\n" +
                    "   scrollPosition: null\n" +
                    "   scrollExtentMax: null\n"
        )

        val config: SemanticsConfiguration = SemanticsConfiguration().also {
            it.isSemanticBoundary = true
            it.isMergingSemanticsOfDescendants = true
            it.onScrollUp = { }
            it.onLongPress = { }
            it.onShowOnScreen = { }
            it.isChecked = false
            it.isSelected = true
            it.isButton = true
            it.label = "Use all the properties"
            it.textDirection = TextDirection.RTL
            it.sortKey = OrdinalSortKey(1.0)
        }
        val allProperties: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTWH(50.0, 10.0, 20.0, 30.0)
            it.transform = Matrix4.translation(Vector3(10.0, 10.0, 0.0))
            it.updateWith(config = config, childrenInInversePaintOrder = null)
        }

        assertThat(
            allProperties.toStringDeep().normalizeHashCodes()
        ).isEqualTo(
            ("SemanticsNode#2\n" +
                    "   STALE\n" +
                    "   owner: null\n" +
                    "   merge boundary ⛔️\n" +
                    "   Rect.fromLTRB(60.0, 20.0, 80.0, 50.0)\n" +
                    "   actions: longPress, scrollUp, showOnScreen\n" +
                    "   flags: hasCheckedState, isSelected, isButton\n" +
                    "   label: \"Use all the properties\"\n" +
                    "   textDirection: RTL\n" +
                    "   sortKey: OrdinalSortKey#19df5(order: 1.0)\n").normalizeHashCodes()
        )

        assertThat(
            allProperties.getSemanticsData().toString()
        ).isEqualTo(
            "SemanticsData(Rect.fromLTRB(50.0, 10.0, 70.0, 40.0), " +
                    "[1.0,0.0,0.0,10.0; 0.0,1.0,0.0,10.0; 0.0,0.0,1.0,0.0; 0.0,0.0,0.0,1.0], " +
                    "actions: [longPress, scrollUp, showOnScreen], " +
                    "flags: [hasCheckedState, isSelected, isButton], " +
                    "label: \"Use all the properties\", textDirection: RTL)"
        )

        val scaled: SemanticsNode = SemanticsNode().also {
            it.rect = Rect.fromLTWH(50.0, 10.0, 20.0, 30.0)
            it.transform = Matrix4.diagonal3(Vector3(10.0, 10.0, 1.0))
        }
        assertThat(
            scaled.toStringDeep()
        ).isEqualTo(
            "SemanticsNode#3\n" +
                    "   STALE\n" +
                    "   owner: null\n" +
                    "   Rect.fromLTRB(50.0, 10.0, 70.0, 40.0) scaled by 10.0x\n"
        )
        assertThat(
            scaled.getSemanticsData().toString()
        ).isEqualTo(
            "SemanticsData(Rect.fromLTRB(50.0, 10.0, 70.0, 40.0), " +
                    "[10.0,0.0,0.0,0.0; 0.0,10.0,0.0,0.0; 0.0,0.0,1.0,0.0; 0.0,0.0,0.0,1.0])"
        )
    }

    @Test
    fun `SemanticsConfiguration getter and setter`() {
        val config = SemanticsConfiguration()

        assertThat(config.isSemanticBoundary).isFalse()
        assertThat(config.isButton).isFalse()
        assertThat(config.isMergingSemanticsOfDescendants).isFalse()
        assertThat(config.isEnabled).isNull()
        assertThat(config.isChecked).isNull()
        assertThat(config.isSelected).isFalse()
        assertThat(config.isBlockingSemanticsOfPreviouslyPaintedNodes).isFalse()
        assertThat(config.isFocused).isFalse()
        assertThat(config.isTextField).isFalse()

        assertThat(config.onShowOnScreen).isNull()
        assertThat(config.onScrollDown).isNull()
        assertThat(config.onScrollUp).isNull()
        assertThat(config.onScrollLeft).isNull()
        assertThat(config.onScrollRight).isNull()
        assertThat(config.onLongPress).isNull()
        assertThat(config.onDecrease).isNull()
        assertThat(config.onIncrease).isNull()
        assertThat(config.onMoveCursorForwardByCharacter).isNull()
        assertThat(config.onMoveCursorBackwardByCharacter).isNull()
        assertThat(config.onTap).isNull()

        config.isSemanticBoundary = true
        config.isButton = true
        config.isMergingSemanticsOfDescendants = true
        config.isEnabled = true
        config.isChecked = true
        config.isSelected = true
        config.isBlockingSemanticsOfPreviouslyPaintedNodes = true
        config.isFocused = true
        config.isTextField = true

        val onShowOnScreen: VoidCallback = { }
        val onScrollDown: VoidCallback = { }
        val onScrollUp: VoidCallback = { }
        val onScrollLeft: VoidCallback = { }
        val onScrollRight: VoidCallback = { }
        val onLongPress: VoidCallback = { }
        val onDecrease: VoidCallback = { }
        val onIncrease: VoidCallback = { }
        val onMoveCursorForwardByCharacter: MoveCursorHandler = { }
        val onMoveCursorBackwardByCharacter: MoveCursorHandler = { }
        val onTap: VoidCallback = { }

        config.onShowOnScreen = onShowOnScreen
        config.onScrollDown = onScrollDown
        config.onScrollUp = onScrollUp
        config.onScrollLeft = onScrollLeft
        config.onScrollRight = onScrollRight
        config.onLongPress = onLongPress
        config.onDecrease = onDecrease
        config.onIncrease = onIncrease
        config.onMoveCursorForwardByCharacter = onMoveCursorForwardByCharacter
        config.onMoveCursorBackwardByCharacter = onMoveCursorBackwardByCharacter
        config.onTap = onTap

        assertThat(config.isSemanticBoundary).isTrue()
        assertThat(config.isButton).isTrue()
        assertThat(config.isMergingSemanticsOfDescendants).isTrue()
        assertThat(config.isEnabled).isTrue()
        assertThat(config.isChecked).isTrue()
        assertThat(config.isSelected).isTrue()
        assertThat(config.isBlockingSemanticsOfPreviouslyPaintedNodes).isTrue()
        assertThat(config.isFocused).isTrue()
        assertThat(config.isTextField).isTrue()

        assertThat(onShowOnScreen).isSameAs(config.onShowOnScreen)
        assertThat(onScrollDown).isSameAs(config.onScrollDown)
        assertThat(onScrollUp).isSameAs(config.onScrollUp)
        assertThat(onScrollLeft).isSameAs(config.onScrollLeft)
        assertThat(onScrollRight).isSameAs(config.onScrollRight)
        assertThat(onLongPress).isSameAs(config.onLongPress)
        assertThat(onDecrease).isSameAs(config.onDecrease)
        assertThat(onIncrease).isSameAs(config.onIncrease)
        assertThat(onMoveCursorForwardByCharacter).isSameAs(config.onMoveCursorForwardByCharacter)
        assertThat(onMoveCursorBackwardByCharacter).isSameAs(config.onMoveCursorBackwardByCharacter)
        assertThat(onTap).isSameAs(config.onTap)
    }
}

private class TestRender(
    var hasTapAction: Boolean = false,
    var hasLongPressAction: Boolean = false,
    var hasScrollLeftAction: Boolean = false,
    var hasScrollRightAction: Boolean = false,
    var hasScrollUpAction: Boolean = false,
    var hasScrollDownAction: Boolean = false,
    var isSemanticBoundary: Boolean,
    // TODO(Migration/ryanmentley): How does this compile in Dart?!
    child: RenderObject? = null
) : RenderProxyBox(child as RenderBox?) {

    override fun describeSemanticsConfiguration(config: SemanticsConfiguration) {
        super.describeSemanticsConfiguration(config)

        config.isSemanticBoundary = isSemanticBoundary
        if (hasTapAction) {
            config.onTap = { }
        }
        if (hasLongPressAction) {
            config.onLongPress = { }
        }
        if (hasScrollLeftAction) {
            config.onScrollLeft = { }
        }
        if (hasScrollRightAction) {
            config.onScrollRight = { }
        }
        if (hasScrollUpAction) {
            config.onScrollUp = { }
        }
        if (hasScrollDownAction) {
            config.onScrollDown = { }
        }
    }
}

private class CustomSortKey(
    order: Double,
    name: String? = null
) : OrdinalSortKey(order, name)
