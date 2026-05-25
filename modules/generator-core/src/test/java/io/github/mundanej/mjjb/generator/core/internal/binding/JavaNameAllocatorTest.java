package io.github.mundanej.mjjb.generator.core.internal.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mundanej.mjjb.schema.model.JsonPointer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class JavaNameAllocatorTest {
  @Test
  void mapsJsonPropertyNamesToJavaFieldNames() {
    assertEquals("userId", JavaNameAllocator.fieldName("user-id"));
    assertEquals("displayName", JavaNameAllocator.fieldName("displayName"));
    assertEquals("userName", JavaNameAllocator.fieldName("USER_NAME"));
    assertEquals("classValue", JavaNameAllocator.fieldName("class"));
    assertEquals("value123Name", JavaNameAllocator.fieldName("123-name"));
    assertEquals("value", JavaNameAllocator.fieldName("!!!"));
  }

  @Test
  void allocatesUniqueFieldNamesWithoutChangingFirstUse() {
    Map<String, JsonPointer> names = new HashMap<>();

    assertEquals("userId", JavaNameAllocator.uniqueFieldName("user-id", names, pointer("a")));
    assertEquals("userId2", JavaNameAllocator.uniqueFieldName("user_id", names, pointer("b")));
    assertEquals("userId3", JavaNameAllocator.uniqueFieldName("user id", names, pointer("c")));
    assertEquals(Set.of("userId", "userId2", "userId3"), names.keySet());
  }

  @Test
  void mapsTaggedBranchValuesToJavaTypeNames() {
    assertEquals("CardPayment", JavaNameAllocator.branchTypeName("card-payment", 0));
    assertEquals("Variant123Name", JavaNameAllocator.branchTypeName("123-name", 0));
    assertEquals("Variant2", JavaNameAllocator.branchTypeName("!!!", 1));
    assertEquals("ClassVariant", JavaNameAllocator.branchTypeName("class", 0));
  }

  @Test
  void allocatesUniqueNestedTypeNames() {
    Set<String> names = new HashSet<>();
    names.add("Root");

    assertEquals("RootProfile", JavaNameAllocator.uniqueNestedTypeName("Root", "profile", names));
    assertEquals("RootProfile2", JavaNameAllocator.uniqueNestedTypeName("Root", "profile", names));
  }

  private static JsonPointer pointer(String propertyName) {
    return JsonPointer.ROOT.property(propertyName);
  }
}
