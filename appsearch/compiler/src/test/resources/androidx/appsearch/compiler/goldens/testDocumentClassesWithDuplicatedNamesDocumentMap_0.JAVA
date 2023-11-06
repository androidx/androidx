package com.example.appsearch;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchDocumentClassMap;
import com.google.auto.service.AutoService;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@AutoService(AppSearchDocumentClassMap.class)
@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__DocumentClassMap_be3cf5fe3ee0964354144ba564257ea30a4560a4f416181f226daea685c5fde5_0 extends AppSearchDocumentClassMap {
  @NonNull
  @Override
  protected Map<String, List<String>> getMap() {
    Map<String, List<String>> result = new HashMap<>();
    result.put("A", Arrays.asList("com.example.appsearch.MyClass1", "com.example.appsearch.MyClass2"));
    result.put("B", Arrays.asList("com.example.appsearch.MyClass3"));
    return result;
  }
}
