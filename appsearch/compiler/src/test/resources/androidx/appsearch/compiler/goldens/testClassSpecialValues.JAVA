package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("string")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.mNamespace, document.mId, SCHEMA_NAME);
    Long mCreationTimestampMillisCopy = document.mCreationTimestampMillis;
    if (mCreationTimestampMillisCopy != null) {
      builder.setCreationTimestampMillis(mCreationTimestampMillisCopy);
    }
    Integer mScoreCopy = document.mScore;
    if (mScoreCopy != null) {
      builder.setScore(mScoreCopy);
    }
    Long mTtlMillisCopy = document.getTtlMillis();
    if (mTtlMillisCopy != null) {
      builder.setTtlMillis(mTtlMillisCopy);
    }
    String mStringCopy = document.mString;
    if (mStringCopy != null) {
      builder.setPropertyString("string", mStringCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String mNamespaceConv = genericDoc.getNamespace();
    String mIdConv = genericDoc.getId();
    long mCreationTimestampMillisConv = genericDoc.getCreationTimestampMillis();
    long mTtlMillisConv = genericDoc.getTtlMillis();
    int mScoreConv = genericDoc.getScore();
    String[] mStringCopy = genericDoc.getPropertyStringArray("string");
    String mStringConv = null;
    if (mStringCopy != null && mStringCopy.length != 0) {
      mStringConv = mStringCopy[0];
    }
    Gift document = new Gift();
    document.mNamespace = mNamespaceConv;
    document.mId = mIdConv;
    document.mCreationTimestampMillis = mCreationTimestampMillisConv;
    document.mScore = mScoreConv;
    document.setTtlMillis(mTtlMillisConv);
    document.mString = mStringConv;
    return document;
  }
}
