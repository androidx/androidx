/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.managers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.param;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.TextInputSession;
import androidx.compose.remote.core.TextInputSupport;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.loom.LoomWireBuffer;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.touch.CommandParameters;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A layout whose content displays a document string the user can edit.
 *
 * <p>The layout is a box that declares a string ({@code textId}) as user-editable and carries the
 * text input attributes a platform editor needs: keyboard type, IME action, flags, maximum length,
 * input filter, hint and content type. Its content is free: typically a text component bound to
 * the same string, possibly decorated (label, icon, border...), but any content works. When the
 * layout is clicked
 * and the player's {@link PaintContext} has a {@link TextInputSupport} installed, a {@link
 * TextInputSession} is started so the host can present a platform editor (IME) over the layout;
 * the host writes edits back into the string data, which updates every binding in the document.
 * Without text input support the layout behaves like a box.
 *
 * <p>When the content contains a {@link CoreText} bound to {@code textId} (looked up once at
 * inflation), that text component is used to place and style the editor, it is not painted while
 * the editor is presented over it, and the hint is painted in its place when the string is empty.
 * Otherwise the editor covers the whole layout and the hint is painted at the layout's content
 * origin.
 *
 * <p>The keyboard type and IME action values mirror Compose's {@code KeyboardType} and {@code
 * ImeAction} so players can map them directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextInputLayout extends BoxLayout implements VariableSupport {

    // Keyboard types (same values as Compose KeyboardType)
    public static final int KEYBOARD_UNSPECIFIED = 0;
    public static final int KEYBOARD_TEXT = 1;
    public static final int KEYBOARD_ASCII = 2;
    public static final int KEYBOARD_NUMBER = 3;
    public static final int KEYBOARD_PHONE = 4;
    public static final int KEYBOARD_URI = 5;
    public static final int KEYBOARD_EMAIL = 6;
    public static final int KEYBOARD_PASSWORD = 7;
    public static final int KEYBOARD_NUMBER_PASSWORD = 8;
    public static final int KEYBOARD_DECIMAL = 9;

    // IME actions (same values as Compose ImeAction)
    public static final int IME_ACTION_NONE = 0;
    public static final int IME_ACTION_DEFAULT = 1;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_PREVIOUS = 5;
    public static final int IME_ACTION_NEXT = 6;
    public static final int IME_ACTION_DONE = 7;

    // Input flags
    /** The editor is a single line; the IME action key replaces the newline key. */
    public static final int FLAG_SINGLE_LINE = 1;

    /**
     * Only write the string back when the IME action is triggered or the editor loses focus,
     * instead of on every keystroke.
     */
    public static final int FLAG_COMMIT_ON_DONE = 1 << 1;

    /** Enable the IME auto-correction. */
    public static final int FLAG_AUTO_CORRECT = 1 << 2;

    /** Capitalize every character. */
    public static final int FLAG_CAPITALIZE_CHARACTERS = 1 << 3;

    /** Capitalize the first character of every word. */
    public static final int FLAG_CAPITALIZE_WORDS = 1 << 4;

    /** Capitalize the first character of every sentence. */
    public static final int FLAG_CAPITALIZE_SENTENCES = 1 << 5;

    // Input filters, applied by the editor before a keystroke reaches the string
    public static final int FILTER_NONE = 0;
    public static final int FILTER_DIGITS = 1;
    public static final int FILTER_DECIMAL = 2;
    public static final int FILTER_ALPHANUMERIC = 3;
    public static final int FILTER_HEXADECIMAL = 4;

    // Content types, hinting what the string holds: drive autofill and keyboard suggestions
    // (Android autofill hints, iOS textContentType, web autocomplete).
    public static final int CONTENT_TYPE_NONE = 0;
    public static final int CONTENT_TYPE_PERSON_NAME = 1;
    public static final int CONTENT_TYPE_PERSON_FIRST_NAME = 2;
    public static final int CONTENT_TYPE_PERSON_LAST_NAME = 3;
    public static final int CONTENT_TYPE_EMAIL_ADDRESS = 4;
    public static final int CONTENT_TYPE_PHONE_NUMBER = 5;
    public static final int CONTENT_TYPE_USERNAME = 6;
    public static final int CONTENT_TYPE_PASSWORD = 7;
    public static final int CONTENT_TYPE_NEW_PASSWORD = 8;
    public static final int CONTENT_TYPE_ONE_TIME_CODE = 9;
    public static final int CONTENT_TYPE_POSTAL_ADDRESS = 10;
    public static final int CONTENT_TYPE_POSTAL_CODE = 11;
    public static final int CONTENT_TYPE_CREDIT_CARD_NUMBER = 12;
    public static final int CONTENT_TYPE_CREDIT_CARD_EXPIRATION_DATE = 13;
    public static final int CONTENT_TYPE_CREDIT_CARD_SECURITY_CODE = 14;

    // Sparse attributes, written after the ids as (count, [id, value]*) so that new attributes
    // can be added without changing the wire format (see TextStyle / CoreText).
    public static final byte P_KEYBOARD_TYPE = 1;
    public static final byte P_IME_ACTION = 2;
    public static final byte P_INPUT_FLAGS = 3;
    public static final byte P_MAX_LENGTH = 4;
    public static final byte P_FILTER = 5;
    public static final byte P_HINT_ID = 6;
    public static final byte P_CONTENT_TYPE = 7;

    public static final CommandParameters PARAMETERS =
            new CommandParameters(
                    param("keyboardType", P_KEYBOARD_TYPE, KEYBOARD_UNSPECIFIED),
                    param("imeAction", P_IME_ACTION, IME_ACTION_NONE),
                    param("inputFlags", P_INPUT_FLAGS, 0),
                    param("maxLength", P_MAX_LENGTH, 0),
                    param("filter", P_FILTER, FILTER_NONE),
                    param("hintId", P_HINT_ID, 0),
                    param("contentType", P_CONTENT_TYPE, CONTENT_TYPE_NONE));

    /** Opacity of the hint relative to the text color, in percent. */
    private static final int HINT_ALPHA_PERCENT = 45;

    private final int mTextId;
    private final int mKeyboardType;
    private final int mImeAction;
    private final int mInputFlags;
    private final int mMaxLength;
    private final int mFilter;
    private final int mHintId;
    private final int mContentType;

    private boolean mEditing = false;
    private @Nullable TextInputSession mSession;
    private @Nullable CoreText mTextComponent;
    private final @NonNull PaintBundle mHintPaint = new PaintBundle();
    private final float @NonNull [] mHintBounds = new float[4];

    public TextInputLayout(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int textId,
            int keyboardType,
            int imeAction,
            int inputFlags,
            int maxLength,
            int filter,
            int hintId,
            int contentType) {
        super(parent, componentId, animationId, BoxLayout.START, BoxLayout.TOP);
        mTextId = textId;
        mKeyboardType = keyboardType;
        mImeAction = imeAction;
        mInputFlags = inputFlags;
        mMaxLength = maxLength;
        mFilter = filter;
        mHintId = hintId;
        mContentType = contentType;
    }

    // ---------------------------------------------------------------------------------------- //
    // Attributes
    // ---------------------------------------------------------------------------------------- //

    /** Returns the id of the edited string. */
    public int getTextId() {
        return mTextId;
    }

    /** Returns the keyboard type, one of the {@code KEYBOARD_*} constants. */
    public int getKeyboardType() {
        return mKeyboardType;
    }

    /** Returns the IME action, one of the {@code IME_ACTION_*} constants. */
    public int getImeAction() {
        return mImeAction;
    }

    /** Returns the bitmask of {@code FLAG_*} constants. */
    public int getInputFlags() {
        return mInputFlags;
    }

    /** Returns true if the given {@code FLAG_*} is set. */
    public boolean hasInputFlag(int flag) {
        return (mInputFlags & flag) != 0;
    }

    /** Returns the maximum length of the text, or 0 when unlimited. */
    public int getMaxLength() {
        return mMaxLength;
    }

    /** Returns the input filter, one of the {@code FILTER_*} constants. */
    public int getFilter() {
        return mFilter;
    }

    /** Returns the id of the hint string, or 0 when none was declared. */
    public int getHintId() {
        return mHintId;
    }

    /** Returns the content type, one of the {@code CONTENT_TYPE_*} constants. */
    public int getContentType() {
        return mContentType;
    }

    @Override
    public void inflate() {
        super.inflate();
        // The content is inflated before this layout, so the lookup is complete here.
        mTextComponent = findTextComponent(this, mTextId);
    }

    /**
     * Returns the text component of the content displaying the edited string, if any: the first
     * {@link CoreText} bound to {@link #getTextId()} found in the content, depth first. Resolved
     * once at inflation. Hosts use it to place and style their editor.
     */
    public @Nullable CoreText getTextComponent() {
        return mTextComponent;
    }

    private static @Nullable CoreText findTextComponent(
            @NonNull LayoutComponent layout, int textId) {
        ArrayList<Component> children = layout.getChildrenComponents();
        for (int i = 0; i < children.size(); i++) {
            Component child = children.get(i);
            if (child instanceof CoreText) {
                Integer childTextId = ((CoreText) child).getTextId();
                if (childTextId != null && childTextId == textId) {
                    return (CoreText) child;
                }
            }
            if (child instanceof LayoutComponent && !(child instanceof TextInputLayout)) {
                CoreText found = findTextComponent((LayoutComponent) child, textId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------- //
    // Editing lifecycle
    // ---------------------------------------------------------------------------------------- //

    /** Returns true while a host editor is presented over this layout. */
    public boolean isEditing() {
        return mEditing;
    }

    /** Returns the active editing session, or null when the layout is not being edited. */
    public @Nullable TextInputSession getSession() {
        return mSession;
    }

    @Override
    public boolean onClick(
            @NonNull RemoteContext context, @NonNull CoreDocument document, float x, float y) {
        // Let the content handle the click first (e.g. a clear button in the field).
        if (super.onClick(context, document, x, y)) {
            return true;
        }
        boolean isUnconditional = x == -1 && y == -1;
        if (!isUnconditional && !contains(context, x, y)) {
            return false;
        }
        TextInputSupport support = textInputSupport(context);
        if (support == null) {
            return false;
        }
        startTextInput(context, support);
        return true;
    }

    /**
     * Starts an editing session with the given host support, unless one is already active.
     *
     * @param context the current context
     * @param support the host text input support
     */
    public void startTextInput(@NonNull RemoteContext context, @NonNull TextInputSupport support) {
        if (mSession != null && mSession.isActive()) {
            return;
        }
        mSession = new TextInputSession(context, this);
        mEditing = true;
        support.startTextInput(mSession);
        context.needsRepaint();
    }

    /**
     * Ends the active session from the document side, telling the host to dismiss its editor.
     *
     * @param context the current context
     */
    public void stopTextInput(@NonNull RemoteContext context) {
        TextInputSession session = mSession;
        if (session == null || !session.isActive()) {
            return;
        }
        TextInputSupport support = textInputSupport(context);
        if (support != null) {
            support.stopTextInput(session);
        }
        session.end();
    }

    /**
     * Called by {@link TextInputSession#end()}; the content renders its text again.
     *
     * @param context the current context
     * @param session the session that ended
     */
    public void onSessionEnded(@NonNull RemoteContext context, @NonNull TextInputSession session) {
        if (mSession != session) {
            return;
        }
        mSession = null;
        mEditing = false;
        context.needsRepaint();
    }

    private static @Nullable TextInputSupport textInputSupport(@NonNull RemoteContext context) {
        PaintContext paintContext = context.getPaintContext();
        return paintContext != null ? paintContext.getTextInputSupport() : null;
    }

    // ---------------------------------------------------------------------------------------- //
    // Variables
    // ---------------------------------------------------------------------------------------- //

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mTextId, this);
        if (mHintId != 0) {
            context.listensTo(mHintId, this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        // The text is read live from the context; nothing to cache.
    }

    // ---------------------------------------------------------------------------------------- //
    // Painting
    // ---------------------------------------------------------------------------------------- //

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        TextInputSession session = mSession;
        if (session != null && session.isActive()) {
            // Keep the host editor aligned with the component
            if (session.updateBounds()) {
                TextInputSupport support = textInputSupport(context.getContext());
                if (support != null) {
                    support.updateTextInputBounds(session);
                }
            }
        }
        CoreText text = mTextComponent;
        if (mEditing && text != null) {
            // The host editor displays the text: paint everything but the text component, which
            // may be nested anywhere in the content (e.g. in a row with an icon).
            Component parent = text.getParent();
            ArrayList<Component> siblings =
                    parent instanceof LayoutComponent
                            ? ((LayoutComponent) parent).getChildrenComponents()
                            : null;
            int index = siblings != null ? siblings.indexOf(text) : -1;
            if (index < 0) {
                super.paintingComponent(context);
                return;
            }
            siblings.remove(index);
            try {
                super.paintingComponent(context);
            } finally {
                siblings.add(index, text);
            }
            return;
        }
        super.paintingComponent(context);
        if (!mEditing && mHintId != 0) {
            String value = context.getContext().getText(mTextId);
            if (value == null || value.isEmpty()) {
                paintHint(context, text);
            }
        }
    }

    /**
     * Paints the hint in place of the empty text: at the text component's content origin with its
     * style at reduced opacity when there is one, else at the layout's content origin.
     */
    private void paintHint(@NonNull PaintContext context, @Nullable CoreText text) {
        String hint = context.getContext().getText(mHintId);
        if (hint == null || hint.isEmpty()) {
            return;
        }
        int length = hint.length();
        float x = mX + mPaddingLeft;
        float y = mY + mPaddingTop;
        float contentW = mWidth - mPaddingLeft - mPaddingRight;
        int textColor = 0xFF000000;
        float fontSize = TextStyle.DEFAULT_FONT_SIZE;
        int weight = (int) TextStyle.DEFAULT_FONT_WEIGHT;
        boolean italic = false;
        int align = CoreText.TEXT_ALIGN_START;
        if (text != null) {
            x += text.getX() + text.getPaddingLeft();
            y += text.getY() + text.getPaddingTop();
            contentW = text.getWidth() - text.getPaddingLeft() - text.getPaddingRight();
            textColor = text.getColorValue();
            fontSize = text.getFontSizeValue();
            weight = (int) text.getFontWeightValue();
            italic = text.getFontStyle() == 1;
            align = text.getTextAlignValue();
        }
        context.save();
        context.translate(x, y);
        context.savePaint();
        int alpha = (textColor >>> 24) * HINT_ALPHA_PERCENT / 100;
        mHintPaint.reset();
        mHintPaint.setStyle(PaintBundle.STYLE_FILL);
        mHintPaint.setColor((alpha << 24) | (textColor & 0x00FFFFFF));
        mHintPaint.setTextSize(fontSize);
        mHintPaint.setTextStyle(0, weight, italic);
        context.replacePaint(mHintPaint);
        context.getTextBounds(mHintId, 0, length, 0, mHintBounds);
        float hintWidth = mHintBounds[2] - mHintBounds[0];
        float px;
        switch (align) {
            case CoreText.TEXT_ALIGN_CENTER:
                px = (contentW - hintWidth) / 2f;
                break;
            case CoreText.TEXT_ALIGN_RIGHT:
            case CoreText.TEXT_ALIGN_END:
                px = contentW - hintWidth;
                break;
            default:
                px = 0f;
        }
        context.drawTextRun(mHintId, 0, length, 0, 0, px - mHintBounds[0], -mHintBounds[1], false);
        context.restorePaint();
        context.restore();
    }

    // ---------------------------------------------------------------------------------------- //
    // Serialization
    // ---------------------------------------------------------------------------------------- //

    @Override
    public @NonNull String toString() {
        return "TEXT_INPUT ["
                + mComponentId
                + ":"
                + mAnimationId
                + "] ("
                + mX
                + ", "
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") text ["
                + mTextId
                + "]";
    }

    @Override
    protected @NonNull String getSerializedName() {
        return "TEXT_INPUT";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                getSerializedName()
                        + " ["
                        + mComponentId
                        + ":"
                        + mAnimationId
                        + "] text ["
                        + mTextId
                        + "] keyboard "
                        + mKeyboardType
                        + " ime "
                        + mImeAction
                        + " flags "
                        + mInputFlags
                        + " maxLength "
                        + mMaxLength
                        + " filter "
                        + mFilter
                        + " hint ["
                        + mHintId
                        + "] content "
                        + mContentType);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer
                .add("textId", mTextId)
                .add("keyboardType", mKeyboardType)
                .add("imeAction", mImeAction)
                .add("inputFlags", mInputFlags)
                .add("maxLength", mMaxLength)
                .add("filter", mFilter)
                .add("hintId", mHintId)
                .add("contentType", mContentType)
                .add("editing", mEditing);
    }

    /** The name of the operation */
    public static @NonNull String name() {
        return "TextInputLayout";
    }

    /** The id of the operation */
    public static int id() {
        return Operations.LAYOUT_TEXT_INPUT;
    }

    /**
     * Write the operation to the buffer: the component, animation and text ids, then the sparse
     * text input attributes. Attributes at their default value are omitted.
     *
     * @param buffer a WireBuffer
     * @param componentId the component id
     * @param animationId the animation id
     * @param textId id of the edited string
     * @param keyboardType keyboard type ({@code KEYBOARD_*})
     * @param imeAction IME action ({@code IME_ACTION_*})
     * @param inputFlags bitmask of {@code FLAG_*}
     * @param maxLength maximum text length, 0 for unlimited
     * @param filter input filter ({@code FILTER_*})
     * @param hintId id of the hint string, 0 for none
     * @param contentType content type ({@code CONTENT_TYPE_*})
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int textId,
            int keyboardType,
            int imeAction,
            int inputFlags,
            int maxLength,
            int filter,
            int hintId,
            int contentType) {
        buffer.start(id());
        buffer.writeInt(componentId);
        buffer.writeInt(animationId);
        buffer.writeInt(textId);
        int count = 0;
        count += PARAMETERS.countIfNotDefault(P_KEYBOARD_TYPE, keyboardType);
        count += PARAMETERS.countIfNotDefault(P_IME_ACTION, imeAction);
        count += PARAMETERS.countIfNotDefault(P_INPUT_FLAGS, inputFlags);
        count += PARAMETERS.countIfNotDefault(P_MAX_LENGTH, maxLength);
        count += PARAMETERS.countIfNotDefault(P_FILTER, filter);
        count += PARAMETERS.countIfNotDefault(P_HINT_ID, hintId);
        count += PARAMETERS.countIfNotDefault(P_CONTENT_TYPE, contentType);
        buffer.writeShort(count);
        PARAMETERS.write(buffer, P_KEYBOARD_TYPE, keyboardType);
        PARAMETERS.write(buffer, P_IME_ACTION, imeAction);
        PARAMETERS.write(buffer, P_INPUT_FLAGS, inputFlags);
        PARAMETERS.write(buffer, P_MAX_LENGTH, maxLength);
        PARAMETERS.write(buffer, P_FILTER, filter);
        PARAMETERS.write(buffer, P_HINT_ID, hintId);
        PARAMETERS.write(buffer, P_CONTENT_TYPE, contentType);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int componentId = buffer.declareId();
        int animationId = buffer.declareId();
        int textId = buffer.readId();
        int paramsLength = buffer.readShort();
        final int[] attributes = {
            KEYBOARD_UNSPECIFIED, IME_ACTION_NONE, 0, 0, FILTER_NONE, 0, CONTENT_TYPE_NONE,
        };
        for (int i = 0; i < paramsLength; i++) {
            PARAMETERS.read(
                    buffer,
                    new CommandParameters.AbstractCallback() {
                        @Override
                        public void value(int id, int value) {
                            switch (id) {
                                case P_KEYBOARD_TYPE:
                                    attributes[0] = value;
                                    break;
                                case P_IME_ACTION:
                                    attributes[1] = value;
                                    break;
                                case P_INPUT_FLAGS:
                                    attributes[2] = value;
                                    break;
                                case P_MAX_LENGTH:
                                    attributes[3] = value;
                                    break;
                                case P_FILTER:
                                    attributes[4] = value;
                                    break;
                                case P_HINT_ID:
                                    if (buffer instanceof LoomWireBuffer) {
                                        attributes[5] =
                                                ((LoomWireBuffer) buffer)
                                                        .getRemapContext()
                                                        .resolveId(value);
                                    } else {
                                        attributes[5] = value;
                                    }
                                    break;
                                case P_CONTENT_TYPE:
                                    attributes[6] = value;
                                    break;
                            }
                        }
                    });
        }
        operations.add(
                new TextInputLayout(
                        null,
                        componentId,
                        animationId,
                        textId,
                        attributes[0],
                        attributes[1],
                        attributes[2],
                        attributes[3],
                        attributes[4],
                        attributes[5],
                        attributes[6]));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Managers", id(), name())
                .addedVersion(7)
                .experimental(true)
                .additionalDocumentation("layout_text_input")
                .description(
                        "A box whose content displays a string the user can edit. On click,"
                            + " players with text input support open a platform editor over the"
                            + " content (aligned with the text component bound to the string when"
                            + " there is one) and write edits back into the string.")
                .field(INT, "componentId", "Unique ID for this component")
                .field(INT, "animationId", "ID for animation purposes")
                .field(INT, "textId", "Id of the edited string data")
                .field(SHORT, "count", "Number of attributes that follow")
                .field(
                        INT,
                        "keyboardType",
                        "Keyboard type: 0 unspecified, 1 text, 2 ascii, 3 number, 4 phone, 5 uri,"
                                + " 6 email, 7 password, 8 number password, 9 decimal")
                .field(
                        INT,
                        "imeAction",
                        "IME action: 0 none, 1 default, 2 go, 3 search, 4 send, 5 previous,"
                                + " 6 next, 7 done")
                .field(
                        INT,
                        "inputFlags",
                        "Bitmask: 1 single line, 2 commit on done, 4 auto correct,"
                                + " 8 capitalize characters, 16 capitalize words,"
                                + " 32 capitalize sentences")
                .field(INT, "maxLength", "Maximum text length, 0 for unlimited")
                .field(
                        INT,
                        "filter",
                        "Input filter: 0 none, 1 digits, 2 decimal, 3 alphanumeric,"
                                + " 4 hexadecimal")
                .field(INT, "hintId", "Id of the hint string, 0 for none")
                .field(
                        INT,
                        "contentType",
                        "What the string holds, for autofill and keyboard suggestions: 0 none,"
                                + " 1 person name, 2 first name, 3 last name, 4 email address,"
                                + " 5 phone number, 6 username, 7 password, 8 new password,"
                                + " 9 one-time code, 10 postal address, 11 postal code,"
                                + " 12 credit card number, 13 credit card expiration date,"
                                + " 14 credit card security code");
    }
}
