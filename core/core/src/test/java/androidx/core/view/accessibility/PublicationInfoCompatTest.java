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

package androidx.core.view.accessibility;

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_ATTRIBUTES_KEY;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_TAG_KEY;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat.STRUCTURED_DATA_TYPE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.OptIn;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.MathInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.PublicationInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.StructuredDataInfoCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tests for {@link PublicationInfoCompat}.
 *
 * <p>{@link PublicationInfoCompat} doesn't wrap a platform object, so it's stored in a node's
 * extras on every SDK, including those where the platform {@code StructuredDataInfo} class is
 * available. Each test runs on the library's minimum SDK, on the newest SDK without the platform
 * {@code StructuredDataInfo} class, and on the first SDK with it, and expects the same behavior on
 * all of them.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = {
            Build.VERSION_CODES.N,
            Build.VERSION_CODES.BAKLAVA,
            Build.VERSION_CODES.CINNAMON_BUN
        })
@DoNotInstrument
@OptIn(markerClass = PreviewAccessibilityApi.class)
public final class PublicationInfoCompatTest {
    private static final String ATTRIBUTE = "attribute";

    @Test
    public void setStructuredDataInfo_roundTripsPublicationInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        PublicationInfoCompat chapter = createChapter();

        node.setStructuredDataInfo(chapter);

        StructuredDataInfoCompat retrieved = node.getStructuredDataInfo();
        assertThat(retrieved).isInstanceOf(PublicationInfoCompat.class);
        assertThat(retrieved.getTag()).isEqualTo(PublicationInfoCompat.PUBLICATION_TAG_CHAPTER);
        assertThat(retrieved.getAttributes()).containsExactly(ATTRIBUTE, "value");
        assertThat(retrieved).isEqualTo(chapter);
    }

    @Test
    public void setStructuredDataInfo_storesPublicationInfoInExtras() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();

        node.setStructuredDataInfo(createChapter());

        Bundle extras = node.getExtras();
        assertThat(extras.getString(STRUCTURED_DATA_TYPE_KEY)).isEqualTo("publication");
        assertThat(extras.getString(STRUCTURED_DATA_TAG_KEY))
                .isEqualTo(PublicationInfoCompat.PUBLICATION_TAG_CHAPTER);
        assertThat(extras.getBundle(STRUCTURED_DATA_ATTRIBUTES_KEY).getString(ATTRIBUTE))
                .isEqualTo("value");
    }

    @Test
    public void setStructuredDataInfo_null_clearsPublicationInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createChapter());

        node.setStructuredDataInfo(null);

        assertThat(node.getStructuredDataInfo()).isNull();
        assertThat(node.getExtras().containsKey(STRUCTURED_DATA_TYPE_KEY)).isFalse();
    }

    @Test
    public void setStructuredDataInfo_publicationInfoReplacesMathInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION));

        node.setStructuredDataInfo(createChapter());

        assertThat(node.getStructuredDataInfo()).isEqualTo(createChapter());
        assertThat(parcelAndUnparcel(node).getStructuredDataInfo()).isEqualTo(createChapter());
    }

    @Test
    public void setStructuredDataInfo_mathInfoReplacesPublicationInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createChapter());
        MathInfoCompat fraction = new MathInfoCompat(MathInfoCompat.MATH_TAG_FRACTION);

        node.setStructuredDataInfo(fraction);

        assertThat(node.getStructuredDataInfo()).isEqualTo(fraction);
        assertThat(parcelAndUnparcel(node).getStructuredDataInfo()).isEqualTo(fraction);
    }

    @Test
    public void parceling_preservesPublicationInfo() {
        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain();
        node.setStructuredDataInfo(createChapter());

        StructuredDataInfoCompat retrieved = parcelAndUnparcel(node).getStructuredDataInfo();

        assertThat(retrieved).isInstanceOf(PublicationInfoCompat.class);
        assertThat(retrieved).isEqualTo(createChapter());
    }

    @Test
    public void equals_publicationAndMathInfoWithSameTag_areNotEqual() {
        String tag = PublicationInfoCompat.PUBLICATION_TAG_CHAPTER;
        PublicationInfoCompat publicationInfo = new PublicationInfoCompat(tag);
        //noinspection WrongConstant
        MathInfoCompat mathInfo = new MathInfoCompat(tag);

        assertThat(mathInfo.getTag()).isEqualTo(publicationInfo.getTag());
        assertThat(publicationInfo).isNotEqualTo(mathInfo);
        assertThat(mathInfo).isNotEqualTo(publicationInfo);
    }

    @Test
    public void equalsAndHashCode_compareTagAndAttributes() {
        PublicationInfoCompat differentTag =
                new PublicationInfoCompat(PublicationInfoCompat.PUBLICATION_TAG_PART);
        differentTag.putAttribute(ATTRIBUTE, "value");

        assertThat(createChapter()).isEqualTo(createChapter());
        assertThat(createChapter().hashCode()).isEqualTo(createChapter().hashCode());
        assertThat(createChapter()).isNotEqualTo(differentTag);
        assertThat(createChapter())
                .isNotEqualTo(
                        new PublicationInfoCompat(PublicationInfoCompat.PUBLICATION_TAG_CHAPTER));
    }

    @Test
    public void constructor_emptyTag_throwsIllegalArgumentException() {
        //noinspection WrongConstant
        assertThrows(IllegalArgumentException.class, () -> new PublicationInfoCompat(""));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void constructor_nullTag_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new PublicationInfoCompat(null));
    }

    @Test
    public void tags_haveExpectedValues() {
        // Tag values are part of the API and must not change.
        assertThat(publicationTags()).containsExactlyEntriesIn(expectedPublicationTags()).inOrder();
    }

    private static PublicationInfoCompat createChapter() {
        PublicationInfoCompat chapter =
                new PublicationInfoCompat(PublicationInfoCompat.PUBLICATION_TAG_CHAPTER);
        chapter.putAttribute(ATTRIBUTE, "value");
        return chapter;
    }

    private static Map<String, String> publicationTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("ABSTRACT", PublicationInfoCompat.PUBLICATION_TAG_ABSTRACT);
        tags.put("ACKNOWLEDGMENTS", PublicationInfoCompat.PUBLICATION_TAG_ACKNOWLEDGMENTS);
        tags.put("AFTERWORD", PublicationInfoCompat.PUBLICATION_TAG_AFTERWORD);
        tags.put("APPENDIX", PublicationInfoCompat.PUBLICATION_TAG_APPENDIX);
        tags.put("BACK_LINK", PublicationInfoCompat.PUBLICATION_TAG_BACK_LINK);
        tags.put("BIBLIOGRAPHY", PublicationInfoCompat.PUBLICATION_TAG_BIBLIOGRAPHY);
        tags.put(
                "BIBLIOGRAPHY_REFERENCE",
                PublicationInfoCompat.PUBLICATION_TAG_BIBLIOGRAPHY_REFERENCE);
        tags.put("CHAPTER", PublicationInfoCompat.PUBLICATION_TAG_CHAPTER);
        tags.put("COLOPHON", PublicationInfoCompat.PUBLICATION_TAG_COLOPHON);
        tags.put("CONCLUSION", PublicationInfoCompat.PUBLICATION_TAG_CONCLUSION);
        tags.put("COVER", PublicationInfoCompat.PUBLICATION_TAG_COVER);
        tags.put("CREDIT", PublicationInfoCompat.PUBLICATION_TAG_CREDIT);
        tags.put("CREDITS", PublicationInfoCompat.PUBLICATION_TAG_CREDITS);
        tags.put("DEDICATION", PublicationInfoCompat.PUBLICATION_TAG_DEDICATION);
        tags.put("ENDNOTES", PublicationInfoCompat.PUBLICATION_TAG_ENDNOTES);
        tags.put("EPIGRAPH", PublicationInfoCompat.PUBLICATION_TAG_EPIGRAPH);
        tags.put("EPILOGUE", PublicationInfoCompat.PUBLICATION_TAG_EPILOGUE);
        tags.put("ERRATA", PublicationInfoCompat.PUBLICATION_TAG_ERRATA);
        tags.put("EXAMPLE", PublicationInfoCompat.PUBLICATION_TAG_EXAMPLE);
        tags.put("FOOTNOTE", PublicationInfoCompat.PUBLICATION_TAG_FOOTNOTE);
        tags.put("FOREWORD", PublicationInfoCompat.PUBLICATION_TAG_FOREWORD);
        tags.put("GLOSSARY", PublicationInfoCompat.PUBLICATION_TAG_GLOSSARY);
        tags.put("GLOSSARY_REFERENCE", PublicationInfoCompat.PUBLICATION_TAG_GLOSSARY_REFERENCE);
        tags.put("INDEX", PublicationInfoCompat.PUBLICATION_TAG_INDEX);
        tags.put("INTRODUCTION", PublicationInfoCompat.PUBLICATION_TAG_INTRODUCTION);
        tags.put("NOTE_REFERENCE", PublicationInfoCompat.PUBLICATION_TAG_NOTE_REFERENCE);
        tags.put("NOTICE", PublicationInfoCompat.PUBLICATION_TAG_NOTICE);
        tags.put("PAGE_BREAK", PublicationInfoCompat.PUBLICATION_TAG_PAGE_BREAK);
        tags.put("PAGE_FOOTER", PublicationInfoCompat.PUBLICATION_TAG_PAGE_FOOTER);
        tags.put("PAGE_HEADER", PublicationInfoCompat.PUBLICATION_TAG_PAGE_HEADER);
        tags.put("PAGE_LIST", PublicationInfoCompat.PUBLICATION_TAG_PAGE_LIST);
        tags.put("PART", PublicationInfoCompat.PUBLICATION_TAG_PART);
        tags.put("PREFACE", PublicationInfoCompat.PUBLICATION_TAG_PREFACE);
        tags.put("PROLOGUE", PublicationInfoCompat.PUBLICATION_TAG_PROLOGUE);
        tags.put("PULL_QUOTE", PublicationInfoCompat.PUBLICATION_TAG_PULL_QUOTE);
        tags.put(
                "QUESTIONS_AND_ANSWERS",
                PublicationInfoCompat.PUBLICATION_TAG_QUESTIONS_AND_ANSWERS);
        tags.put("SUBTITLE", PublicationInfoCompat.PUBLICATION_TAG_SUBTITLE);
        tags.put("TABLE_OF_CONTENTS", PublicationInfoCompat.PUBLICATION_TAG_TABLE_OF_CONTENTS);
        tags.put("TIP", PublicationInfoCompat.PUBLICATION_TAG_TIP);
        return tags;
    }

    private static Map<String, String> expectedPublicationTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("ABSTRACT", "doc-abstract");
        tags.put("ACKNOWLEDGMENTS", "doc-acknowledgments");
        tags.put("AFTERWORD", "doc-afterword");
        tags.put("APPENDIX", "doc-appendix");
        tags.put("BACK_LINK", "doc-backlink");
        tags.put("BIBLIOGRAPHY", "doc-bibliography");
        tags.put("BIBLIOGRAPHY_REFERENCE", "doc-biblioref");
        tags.put("CHAPTER", "doc-chapter");
        tags.put("COLOPHON", "doc-colophon");
        tags.put("CONCLUSION", "doc-conclusion");
        tags.put("COVER", "doc-cover");
        tags.put("CREDIT", "doc-credit");
        tags.put("CREDITS", "doc-credits");
        tags.put("DEDICATION", "doc-dedication");
        tags.put("ENDNOTES", "doc-endnotes");
        tags.put("EPIGRAPH", "doc-epigraph");
        tags.put("EPILOGUE", "doc-epilogue");
        tags.put("ERRATA", "doc-errata");
        tags.put("EXAMPLE", "doc-example");
        tags.put("FOOTNOTE", "doc-footnote");
        tags.put("FOREWORD", "doc-foreword");
        tags.put("GLOSSARY", "doc-glossary");
        tags.put("GLOSSARY_REFERENCE", "doc-glossref");
        tags.put("INDEX", "doc-index");
        tags.put("INTRODUCTION", "doc-introduction");
        tags.put("NOTE_REFERENCE", "doc-noteref");
        tags.put("NOTICE", "doc-notice");
        tags.put("PAGE_BREAK", "doc-pagebreak");
        tags.put("PAGE_FOOTER", "doc-pagefooter");
        tags.put("PAGE_HEADER", "doc-pageheader");
        tags.put("PAGE_LIST", "doc-pagelist");
        tags.put("PART", "doc-part");
        tags.put("PREFACE", "doc-preface");
        tags.put("PROLOGUE", "doc-prologue");
        tags.put("PULL_QUOTE", "doc-pullquote");
        tags.put("QUESTIONS_AND_ANSWERS", "doc-qna");
        tags.put("SUBTITLE", "doc-subtitle");
        tags.put("TABLE_OF_CONTENTS", "doc-toc");
        tags.put("TIP", "doc-tip");
        return tags;
    }

    /**
     * Returns a copy of {@code node} made by parceling it, as when it's sent to an accessibility
     * service. On older SDKs, this recycles {@code node}, so it mustn't be used afterwards.
     */
    private static AccessibilityNodeInfoCompat parcelAndUnparcel(AccessibilityNodeInfoCompat node) {
        Parcel parcel = Parcel.obtain();
        try {
            node.unwrap().writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return AccessibilityNodeInfoCompat.wrap(
                    AccessibilityNodeInfo.CREATOR.createFromParcel(parcel));
        } finally {
            parcel.recycle();
        }
    }
}
