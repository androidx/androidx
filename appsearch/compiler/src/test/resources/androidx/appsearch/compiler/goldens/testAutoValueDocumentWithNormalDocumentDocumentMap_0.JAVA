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
public final class $$__AppSearch__DocumentClassMap_8bbdfb6f96b48bfad89bc598cab2960300d30e5388d5aaa970ab2db67993d889_0 extends AppSearchDocumentClassMap {
  @NonNull
  @Override
  protected Map<String, List<String>> getMap() {
    Map<String, List<String>> result = new HashMap<>();
    result.put("Person", Arrays.asList("com.example.appsearch.Person"));
    return result;
  }
}
