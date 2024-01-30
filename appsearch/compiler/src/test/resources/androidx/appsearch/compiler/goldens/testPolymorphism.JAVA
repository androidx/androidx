package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
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
          .addParentType($$__AppSearch__Parent1.SCHEMA_NAME)
          .addParentType($$__AppSearch__Parent2.SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("note2")
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("note1")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Parent1.class);
    classSet.add(Parent2.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String note2Copy = document.note2;
    if (note2Copy != null) {
      builder.setPropertyString("note2", note2Copy);
    }
    String senderCopy = document.sender;
    if (senderCopy != null) {
      builder.setPropertyString("sender", senderCopy);
    }
    String note1Copy = document.note1;
    if (note1Copy != null) {
      builder.setPropertyString("note1", note1Copy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    String[] note2Copy = genericDoc.getPropertyStringArray("note2");
    String note2Conv = null;
    if (note2Copy != null && note2Copy.length != 0) {
      note2Conv = note2Copy[0];
    }
    String[] senderCopy = genericDoc.getPropertyStringArray("sender");
    String senderConv = null;
    if (senderCopy != null && senderCopy.length != 0) {
      senderConv = senderCopy[0];
    }
    String[] note1Copy = genericDoc.getPropertyStringArray("note1");
    String note1Conv = null;
    if (note1Copy != null && note1Copy.length != 0) {
      note1Conv = note1Copy[0];
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.note2 = note2Conv;
    document.sender = senderConv;
    document.note1 = note1Conv;
    return document;
  }
}
