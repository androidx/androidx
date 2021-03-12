package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .setVersion(42)
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.uri, SCHEMA_NAME);
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String namespaceConv = genericDoc.getNamespace();
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.uri = uriConv;
    return document;
  }
}
