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
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("middleContentA", $$__AppSearch__Middle.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("middleContentB", $$__AppSearch__Middle.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Middle.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    Middle middleContentACopy = document.middleContentA;
    if (middleContentACopy != null) {
      GenericDocument middleContentAConv = GenericDocument.fromDocumentClass(middleContentACopy);
      builder.setPropertyDocument("middleContentA", middleContentAConv);
    }
    Middle middleContentBCopy = document.middleContentB;
    if (middleContentBCopy != null) {
      GenericDocument middleContentBConv = GenericDocument.fromDocumentClass(middleContentBCopy);
      builder.setPropertyDocument("middleContentB", middleContentBConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    GenericDocument middleContentACopy = genericDoc.getPropertyDocument("middleContentA");
    Middle middleContentAConv = null;
    if (middleContentACopy != null) {
      middleContentAConv = middleContentACopy.toDocumentClass(Middle.class);
    }
    GenericDocument middleContentBCopy = genericDoc.getPropertyDocument("middleContentB");
    Middle middleContentBConv = null;
    if (middleContentBCopy != null) {
      middleContentBConv = middleContentBCopy.toDocumentClass(Middle.class);
    }
    Gift document = new Gift();
    document.id = idConv;
    document.namespace = namespaceConv;
    document.middleContentA = middleContentAConv;
    document.middleContentB = middleContentBConv;
    return document;
  }
}
