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
public final class $$__AppSearch__FooGift implements DocumentClassFactory<FooGift> {
  public static final String SCHEMA_NAME = "MyGift";

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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("sender")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("foo")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(FooGift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String noteCopy = document.note;
    if (noteCopy != null) {
      builder.setPropertyString("note", noteCopy);
    }
    String senderCopy = document.sender;
    if (senderCopy != null) {
      builder.setPropertyString("sender", senderCopy);
    }
    builder.setPropertyBoolean("foo", document.foo);
    return builder.build();
  }

  @Override
  public FooGift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    String[] noteCopy = genericDoc.getPropertyStringArray("note");
    String noteConv = null;
    if (noteCopy != null && noteCopy.length != 0) {
      noteConv = noteCopy[0];
    }
    String[] senderCopy = genericDoc.getPropertyStringArray("sender");
    String senderConv = null;
    if (senderCopy != null && senderCopy.length != 0) {
      senderConv = senderCopy[0];
    }
    boolean fooConv = genericDoc.getPropertyBoolean("foo");
    FooGift document = new FooGift(idConv, namespaceConv, noteConv, fooConv);
    document.sender = senderConv;
    return document;
  }
}
