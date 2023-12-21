package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Ancestor";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("note")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("receiver")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("sender")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.getNamespace(), document.getId(), SCHEMA_NAME);
    String mNoteCopy = document.getNote();
    if (mNoteCopy != null) {
      builder.setPropertyString("note", mNoteCopy);
    }
    String mReceiverCopy = document.getReceiver();
    if (mReceiverCopy != null) {
      builder.setPropertyString("receiver", mReceiverCopy);
    }
    String mSenderCopy = document.getSender();
    if (mSenderCopy != null) {
      builder.setPropertyString("sender", mSenderCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String mIdConv = genericDoc.getId();
    String mNamespaceConv = genericDoc.getNamespace();
    String[] mNoteCopy = genericDoc.getPropertyStringArray("note");
    String mNoteConv = null;
    if (mNoteCopy != null && mNoteCopy.length != 0) {
      mNoteConv = mNoteCopy[0];
    }
    String[] mReceiverCopy = genericDoc.getPropertyStringArray("receiver");
    String mReceiverConv = null;
    if (mReceiverCopy != null && mReceiverCopy.length != 0) {
      mReceiverConv = mReceiverCopy[0];
    }
    String[] mSenderCopy = genericDoc.getPropertyStringArray("sender");
    String mSenderConv = null;
    if (mSenderCopy != null && mSenderCopy.length != 0) {
      mSenderConv = mSenderCopy[0];
    }
    Gift document = new Gift(mIdConv, mNamespaceConv, mNoteConv, mReceiverConv, mSenderConv);
    return document;
  }
}
