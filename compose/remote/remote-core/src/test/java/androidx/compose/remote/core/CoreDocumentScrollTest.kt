/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.RootContentBehavior
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests how the [Header.DOC_SCROLL] header property affects the scroll queries of documents. */
@RunWith(JUnit4::class)
class CoreDocumentScrollTest {

    @Test
    fun noDocScrollAndNoScrollContainer_reportsNoScroll() {
        val document = load(createDocument())

        assertThat(document.hasHorizontalScroll()).isFalse()
        assertThat(document.hasVerticalScroll()).isFalse()
    }

    @Test
    fun docScrollVertical_reportsVerticalScroll() {
        val document = load(createDocument(scroll = Header.SCROLL_VERTICAL))

        assertThat(document.hasHorizontalScroll()).isFalse()
        assertThat(document.hasVerticalScroll()).isTrue()
    }

    @Test
    fun docScrollHorizontal_reportsHorizontalScroll() {
        val document = load(createDocument(scroll = Header.SCROLL_HORIZONTAL))

        assertThat(document.hasHorizontalScroll()).isTrue()
        assertThat(document.hasVerticalScroll()).isFalse()
    }

    @Test
    fun docScrollBothDirections_reportsBothDirections() {
        val document =
            load(createDocument(scroll = Header.SCROLL_HORIZONTAL or Header.SCROLL_VERTICAL))

        assertThat(document.hasHorizontalScroll()).isTrue()
        assertThat(document.hasVerticalScroll()).isTrue()
    }

    @Test
    fun docScroll_addsToTheScrollContainersFound() {
        val document =
            load(
                createDocument(
                    scroll = Header.SCROLL_HORIZONTAL,
                    scrollContainer = Header.SCROLL_VERTICAL,
                )
            )

        assertThat(document.hasHorizontalScroll()).isTrue()
        assertThat(document.hasVerticalScroll()).isTrue()
    }

    @Test
    fun docScroll_cannotHideScrollContainers() {
        for (scroll in listOf(0, Header.SCROLL_HORIZONTAL)) {
            val vertical =
                load(createDocument(scroll = scroll, scrollContainer = Header.SCROLL_VERTICAL))
            assertThat(vertical.hasVerticalScroll()).isTrue()
        }
        for (scroll in listOf(0, Header.SCROLL_VERTICAL)) {
            val horizontal =
                load(createDocument(scroll = scroll, scrollContainer = Header.SCROLL_HORIZONTAL))
            assertThat(horizontal.hasHorizontalScroll()).isTrue()
        }
    }

    @Test
    fun docScroll_cannotHideRootContentScroll() {
        val document = load(createDocument(scroll = Header.SCROLL_HORIZONTAL))

        // What playing a RootContentBehavior operation does.
        document.setRootContentBehavior(
            RootContentBehavior.SCROLL_VERTICAL,
            RootContentBehavior.ALIGNMENT_CENTER,
            RootContentBehavior.NONE,
            RootContentBehavior.NONE,
        )

        assertThat(document.hasHorizontalScroll()).isTrue()
        assertThat(document.hasVerticalScroll()).isTrue()
    }

    @Test
    fun docScroll_ignoresValuesWritersReject() {
        // Other producers or newer versions may write values that writers here reject: players
        // ignore unknown flags, and values that aren't INTs.
        val unknownFlag = load(headerOnlyDocument(Header.SCROLL_VERTICAL or 4))
        assertThat(unknownFlag.hasHorizontalScroll()).isFalse()
        assertThat(unknownFlag.hasVerticalScroll()).isTrue()

        for (value in listOf<Any>("vertical", 2f, 2L)) {
            val document = load(headerOnlyDocument(value))
            assertThat(document.hasHorizontalScroll()).isFalse()
            assertThat(document.hasVerticalScroll()).isFalse()
        }
    }

    @Test
    fun docScroll_canBePeekedFromTheHeaderOfCompressedDocuments() {
        val plain = createDocument(scroll = Header.SCROLL_VERTICAL)
        val compressed = Header.compressDocument(plain, plain.size)

        // Hosts can read the header without inflating the operations.
        val header = Header.readDirect(ByteArrayInputStream(compressed))

        assertThat(header.get(Header.DOC_SCROLL)).isEqualTo(Header.SCROLL_VERTICAL)
        assertThat(header.toString()).contains("DOC_SCROLL 2")
        assertThat(load(compressed).hasVerticalScroll()).isTrue()
    }

    /**
     * Returns a document with a column that scrolls in the [scrollContainer] directions, and a
     * [Header.DOC_SCROLL] property set to [scroll], or no such property if [scroll] is null.
     */
    private fun createDocument(scroll: Int? = null, scrollContainer: Int = 0): ByteArray {
        val tags =
            mutableListOf(
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 300),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 300),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        if (scroll != null) {
            tags += RemoteComposeWriter.hTag(Header.DOC_SCROLL, scroll)
        }
        val writer = RemoteComposeWriter(RcPlatformServices.None, *tags.toTypedArray())
        var modifier = RecordingModifier().fillMaxSize()
        if (scrollContainer and Header.SCROLL_HORIZONTAL != 0) {
            modifier = modifier.horizontalScroll(writer.addNamedFloat("scrollX", 0f))
        }
        if (scrollContainer and Header.SCROLL_VERTICAL != 0) {
            modifier = modifier.verticalScroll(writer.addNamedFloat("scrollY", 0f))
        }
        writer.root { writer.column(modifier, ColumnLayout.START, ColumnLayout.TOP) {} }
        return writer.encodeToByteArray()
    }

    /**
     * Returns a document made of a header whose [Header.DOC_SCROLL] property is [scroll], written
     * without the writer's checks.
     */
    private fun headerOnlyDocument(scroll: Any): ByteArray {
        val buffer = WireBuffer()
        Header.apply(
            buffer,
            CoreDocument.DOCUMENT_API_LEVEL,
            shortArrayOf(Header.DOC_SCROLL),
            arrayOf(scroll),
        )
        return buffer.cloneBytes()
    }

    /** Loads [document] without initializing a context: the header is read at load time. */
    private fun load(document: ByteArray): CoreDocument =
        CoreDocument().apply {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document)))
        }
}
