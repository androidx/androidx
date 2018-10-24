package androidx.ui.engine.text

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.painting.Canvas

/**
 * A paragraph of text.
 *
 * A paragraph retains the size and position of each glyph in the text and can
 * be efficiently resized and painted.
 *
 * To create a [Paragraph] object, use a [ParagraphBuilder].
 *
 * Paragraphs can be displayed on a [Canvas] using the [Canvas.drawParagraph]
 * method.
 *
 * This class is created by the engine, and should not be instantiated
 * or extended directly.
 *
 * To create a [Paragraph] object, use a [ParagraphBuilder].
 */
//
// TODO(Migration/siyamed): actually this goes into native, but for now added the constructor to
// include the styles.
class Paragraph internal constructor(
    val text: StringBuilder,
    val paragraphStyle: ParagraphStyle,
    val textStyles: List<ParagraphBuilder.TextStyleIndex>
) {
    private var needsLayout = true
    private val paragraphImpl: ParagraphAndroid

    /**
     * The amount of horizontal space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val width: Double
        get() = paragraphImpl.width

    /**
     * The amount of vertical space this paragraph occupies.
     *
     * Valid only after [layout] has been called.
     */
    val height: Double
        get() = paragraphImpl.height

    /**
     * The minimum width that this paragraph could be without failing to paint
     * its contents within itself.
     *
     * Valid only after [layout] has been called.
     */
    val minIntrinsicWidth
        get() = paragraphImpl.minIntrinsicWidth

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     *
     * Valid only after [layout] has been called.
     */
    val maxIntrinsicWidth: Double
        get() = paragraphImpl.maxIntrinsicWidth

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the first line, in logical pixels.
     */
    val alphabeticBaseline: Double
        get() = paragraphImpl.alphabeticBaseline

    /**
     * The distance from the top of the paragraph to the ideographic
     * baseline of the first line, in logical pixels.
     */
    val ideographicBaseline: Double
        get() = paragraphImpl.ideographicBaseline

    /**
     * True if there is more vertical content, but the text was truncated, either
     * because we reached `maxLines` lines of text or because the `maxLines` was
     * null, `ellipsis` was not null, and one of the lines exceeded the width
     * constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [new
     * ParagraphStyle].
     */
    val didExceedMaxLines: Boolean
        get() = paragraphImpl.didExceedMaxLines

    init {
        paragraphImpl = ParagraphAndroid(text, paragraphStyle, textStyles)
    }

    // void Paragraph::SetFontCollection(
    // std::shared_ptr<FontCollection> font_collection) {
    //    font_collection_ = std::move(font_collection);
    // }

    // void Paragraph::SetParagraphStyle(const ParagraphStyle& style) {
    //    needs_layout_ = true;
    //    paragraph_style_ = style;
    // }

    /**
     * Computes the size and position of each glyph in the paragraph.
     *
     * The [ParagraphConstraints] control how wide the text is allowed to be.
     */
    fun layout(constraints: ParagraphConstraints) {
        _layout(constraints.width)
    }

    private fun _layout(width: Double, force: Boolean = false) {
        // TODO(migration/siyamed) the comparison should be floor(width) since it is
        // floored in paragraphImpl, or the comparison should be moved to there.
        if (!needsLayout && this.width == width && !force) return
        needsLayout = false
        paragraphImpl.layout(width, force)
    }

    /** Returns a list of text boxes that enclose the given text range. */
    fun getBoxesForRange(start: Int, end: Int): List<TextBox> {
        TODO()
        // native 'Paragraph_getRectsForRange';
        //    std::vector<TextBox> ParagraphImplTxt::getRectsForRange(
        //        unsigned start,
        //        unsigned end,
        //        txt::Paragraph::RectStyle rect_style) {
        //        std::vector<TextBox> result;
        //        std::vector<txt::Paragraph::TextBox> boxes =
        //            m_paragraph->GetRectsForRange(start, end, rect_style);
        //        for (const txt::Paragraph::TextBox& box : boxes) {
        //        result.emplace_back(box.rect,
        //            static_cast<blink::TextDirection>(box.direction));
        //    }
        //        return result;
        //    }
    }

    /** Returns the text position closest to the given offset. */
    fun getPositionForOffset(offset: Offset): TextPosition {
        return paragraphImpl.getPositionForOffset(offset)
    }

    /**
     * Returns the [start, end] of the word at the given offset. Characters not
     * part of a word, such as spaces, symbols, and punctuation, have word breaks
     * on both sides. In such cases, this method will return [offset, offset+1].
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): List<Int> {
        TODO()
        // native 'Paragraph_getWordBoundary';
    }

    // Redirecting the paint function in this way solves some dependency problems
    // in the C++ code. If we straighten out the C++ dependencies, we can remove
    // this indirection.
    fun _paint(canvas: Canvas, x: Double, y: Double) {
        // native 'Paragraph_paint';
        TODO()
    }
}

// TODO(Migration/siyamed): native blink::paragraph_impl.h
// namespace blink {
//
//    class ParagraphImpl {
//        public:
//        virtual ~ParagraphImpl(){};
//
//        virtual double width() = 0;
//
//        virtual double height() = 0;
//
//        virtual double minIntrinsicWidth() = 0;
//
//        virtual double maxIntrinsicWidth() = 0;
//
//        virtual double alphabeticBaseline() = 0;
//
//        virtual double ideographicBaseline() = 0;
//
//        virtual bool didExceedMaxLines() = 0;
//
//        virtual void layout(double width) = 0;
//
//        virtual void paint(Canvas* canvas, double x, double y) = 0;
//
//        virtual std::vector<TextBox> getRectsForRange(
//        unsigned start,
//        unsigned end,
//        txt::Paragraph::RectStyle rect_style) = 0;
//
//        virtual Dart_Handle getPositionForOffset(double dx, double dy) = 0;
//
//        virtual Dart_Handle getWordBoundary(unsigned offset) = 0;
//    };
//
// }  // namespace blink
//
// #endif  // FLUTTER_LIB_UI_TEXT_PARAGRAPH_IMPL_H_

// TODO(Migration/siyamed): native blink::paragraph_impl_txt.h
// class ParagraphImplTxt : public ParagraphImpl {
//    public:
//    ~ParagraphImplTxt() override;
//
//    explicit ParagraphImplTxt(std::unique_ptr<txt::Paragraph> paragraph);
//
//    double width() override;
//    double height() override;
//    double minIntrinsicWidth() override;
//    double maxIntrinsicWidth() override;
//    double alphabeticBaseline() override;
//    double ideographicBaseline() override;
//    bool didExceedMaxLines() override;
//
//    void layout(double width) override;
//    void paint(Canvas* canvas, double x, double y) override;
//
//    std::vector<TextBox> getRectsForRange(
//        unsigned start,
//    unsigned end,
//    txt::Paragraph::RectStyle rect_style) override;
//    Dart_Handle getPositionForOffset(double dx, double dy) override;
//    Dart_Handle getWordBoundary(unsigned offset) override;
//
//    private:
//    std::unique_ptr<txt::Paragraph> m_paragraph;
//    double m_width = -1.0;
// };
//
// }  // namespace blink
//
// #endif  // FLUTTER_LIB_UI_TEXT_PARAGRAPH_IMPL_TXT_H_

// TODO(Migration/siyamed): native blink::paragraph_impl_txt.cc
// namespace blink {
//
//    ParagraphImplTxt::ParagraphImplTxt(std::unique_ptr<txt::Paragraph> paragraph)
//    : m_paragraph(std::move(paragraph)) {}
//
//    ParagraphImplTxt::~ParagraphImplTxt() {}
//
//    double ParagraphImplTxt::width() {
//        return m_paragraph->GetMaxWidth();
//    }
//
//    double ParagraphImplTxt::height() {
//        return m_paragraph->GetHeight();
//    }
//
//    double ParagraphImplTxt::minIntrinsicWidth() {
//        return m_paragraph->GetMinIntrinsicWidth();
//    }
//
//    double ParagraphImplTxt::maxIntrinsicWidth() {
//        return m_paragraph->GetMaxIntrinsicWidth();
//    }
//
//    double ParagraphImplTxt::alphabeticBaseline() {
//        return m_paragraph->GetAlphabeticBaseline();
//    }
//
//    double ParagraphImplTxt::ideographicBaseline() {
//        return m_paragraph->GetIdeographicBaseline();
//    }
//
//    bool ParagraphImplTxt::didExceedMaxLines() {
//        return m_paragraph->DidExceedMaxLines();
//    }
//
//    void ParagraphImplTxt::layout(double width) {
//        m_width = width;
//        m_paragraph->Layout(width);
//    }
//
//    void ParagraphImplTxt::paint(Canvas* canvas, double x, double y) {
//        SkCanvas* sk_canvas = canvas->canvas();
//        if (!sk_canvas)
//            return;
//        m_paragraph->Paint(sk_canvas, x, y);
//    }
//
//
//    Dart_Handle ParagraphImplTxt::getPositionForOffset(double dx, double dy) {
//        Dart_Handle result = Dart_NewListOf(Dart_CoreType_Int, 2);
//        txt::Paragraph::PositionWithAffinity pos =
//        m_paragraph->GetGlyphPositionAtCoordinate(dx, dy);
//        Dart_ListSetAt(result, 0, ToDart(pos.position));
//        Dart_ListSetAt(result, 1, ToDart(static_cast<int>(pos.affinity)));
//        return result;
//    }
//
//    Dart_Handle ParagraphImplTxt::getWordBoundary(unsigned offset) {
//        txt::Paragraph::Range<size_t> point = m_paragraph->GetWordBoundary(offset);
//        Dart_Handle result = Dart_NewListOf(Dart_CoreType_Int, 2);
//        Dart_ListSetAt(result, 0, ToDart(point.start));
//        Dart_ListSetAt(result, 1, ToDart(point.end));
//        return result;
//    }
//
// }  // namespace blink
