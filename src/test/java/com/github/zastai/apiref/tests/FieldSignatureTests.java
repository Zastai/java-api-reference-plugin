package com.github.zastai.apiref.tests;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.signatures.FieldSignature;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSignatureTests {

  @Test
  public void case1() {
    // T field;
    final var signature = FieldSignature.decode("TT;");
    assertNotNull(signature);
    {
      final var t = signature.type;
      assertEquals(0, t.arrayDimensions);
      assertTrue(t.isTypeVariable);
      assertEquals("T", t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
  }

  @Test
  public void case2() {
    // List<?> field;
    final var signature = FieldSignature.decode("Ljava/util/List<*>;");
    assertNotNull(signature);
    {
      final var t = signature.type;
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(List.class), t.name);
      assertNull(t.qualifier);
      assertNotNull(t.typeArguments);
      assertEquals(1, t.typeArguments.length);
      assertNull(t.typeArguments[0]);
      assertNull(t.wildcard);
    }
  }

  @Test
  public void case3() {
    // Map<String,Set<Integer>> field;
    final var signature = FieldSignature.decode("Ljava/util/Map<Ljava/lang/String;Ljava/util/Set<Ljava/lang/Integer;>;>;");
    assertNotNull(signature);
    {
      final var t = signature.type;
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(Map.class), t.name);
      assertNull(t.qualifier);
      assertNotNull(t.typeArguments);
      assertEquals(2, t.typeArguments.length);
      {
        final var a = t.typeArguments[0];
        assertNotNull(a);
        assertEquals(0, a.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(Type.getDescriptor(String.class), a.name);
        assertNull(a.qualifier);
        assertNull(a.typeArguments);
        assertNull(a.wildcard);
      }
      {
        final var a = t.typeArguments[1];
        assertNotNull(a);
        assertEquals(0, a.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(Type.getDescriptor(Set.class), a.name);
        assertNull(a.qualifier);
        assertNotNull(a.typeArguments);
        assertEquals(1, a.typeArguments.length);
        {
          final var a2 = a.typeArguments[0];
          assertNotNull(a2);
          assertEquals(0, a2.arrayDimensions);
          assertFalse(t.isTypeVariable);
          assertEquals(Type.getDescriptor(Integer.class), a2.name);
          assertNull(a2.qualifier);
          assertNull(a2.typeArguments);
          assertNull(a2.wildcard);
        }
        assertNull(a.wildcard);
      }
      assertNull(t.wildcard);
    }
  }

  @Test
  public void case4() {
    // Foo<String>.Bar<Integer>.Xyzzy<Set<StringBuilder>> field;
    final var signature = FieldSignature.decode("LFoo<Ljava/lang/String;>.Bar<Ljava/lang/Integer;>.Xyzzy<Ljava/util/Set<Ljava/lang/StringBuilder;>;>;");
    assertNotNull(signature);
    {
      final var t = signature.type;
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals("Xyzzy", t.name);
      {
        final var q = t.qualifier;
        assertNotNull(q);
        assertEquals(0, q.arrayDimensions);
        assertFalse(q.isTypeVariable);
        assertEquals("Bar", q.name);
        {
          final var q2 = q.qualifier;
          assertNotNull(q2);
          assertEquals(0, q2.arrayDimensions);
          assertFalse(q2.isTypeVariable);
          assertEquals(ASMUtil.descriptorForName("Foo"), q2.name);
          assertNotNull(q2.typeArguments);
          assertEquals(1, q2.typeArguments.length);
          {
            final var a = q2.typeArguments[0];
            assertNotNull(a);
            assertEquals(0, a.arrayDimensions);
            assertFalse(t.isTypeVariable);
            assertEquals(Type.getDescriptor(String.class), a.name);
            assertNull(a.qualifier);
            assertNull(a.typeArguments);
            assertNull(a.wildcard);
          }
          assertNull(q2.wildcard);
        }
        assertNotNull(q.typeArguments);
        assertEquals(1, q.typeArguments.length);
        {
          final var a = q.typeArguments[0];
          assertNotNull(a);
          assertEquals(0, a.arrayDimensions);
          assertFalse(t.isTypeVariable);
          assertEquals(Type.getDescriptor(Integer.class), a.name);
          assertNull(a.qualifier);
          assertNull(a.typeArguments);
          assertNull(a.wildcard);
        }
        assertNull(q.wildcard);
      }
      assertNotNull(t.typeArguments);
      assertEquals(1, t.typeArguments.length);
      {
        final var a = t.typeArguments[0];
        assertNotNull(a);
        assertEquals(0, a.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(Type.getDescriptor(Set.class), a.name);
        assertNull(a.qualifier);
        assertNotNull(a.typeArguments);
        assertEquals(1, a.typeArguments.length);
        {
          final var a2 = a.typeArguments[0];
          assertNotNull(a2);
          assertEquals(0, a2.arrayDimensions);
          assertFalse(t.isTypeVariable);
          assertEquals(Type.getDescriptor(StringBuilder.class), a2.name);
          assertNull(a2.qualifier);
          assertNull(a2.typeArguments);
          assertNull(a2.wildcard);
        }
        assertNull(a.wildcard);
      }
      assertNull(t.wildcard);
    }
  }

}
