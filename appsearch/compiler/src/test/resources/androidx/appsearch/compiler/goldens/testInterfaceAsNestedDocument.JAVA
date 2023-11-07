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
import java.util.Map;
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
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("thing", $$__AppSearch__Thing.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Thing.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    Thing thingCopy = document.thing;
    if (thingCopy != null) {
      GenericDocument thingConv = GenericDocument.fromDocumentClass(thingCopy);
      builder.setPropertyDocument("thing", thingConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      Map<String, List<String>> documentClassMap) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    GenericDocument thingCopy = genericDoc.getPropertyDocument("thing");
    Thing thingConv = null;
    if (thingCopy != null) {
      thingConv = thingCopy.toDocumentClass(Thing.class, documentClassMap);
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.thing = thingConv;
    return document;
  }
}
