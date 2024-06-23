package com.github.zastai.apiref.tests;

import com.github.zastai.apiref.internal.WellKnown;
import com.github.zastai.apiref.signatures.MethodSignature;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodSignatureTests {

  @Test
  public void case1() {
    // <T> T method()
    final var signature = MethodSignature.decode("<T:Ljava/lang/Object;>()TT;");
    assertNotNull(signature);
    assertNotNull(signature.typeParameters);
    assertEquals(1, signature.typeParameters.length);
    {
      final var p = signature.typeParameters[0];
      assertEquals("T", p.name);
      assertNotNull(p.classBound);
      {
        final var b = p.classBound;
        assertEquals(0, b.arrayDimensions);
        assertFalse(b.isTypeVariable);
        assertEquals(WellKnown.Descriptors.OBJECT, b.name);
        assertNull(b.qualifier);
        assertNull(b.typeArguments);
        assertNull(b.wildcard);
      }
      assertNull(p.interfaceBounds);
    }
    {
      final var t = signature.returnType;
      assertEquals(0, t.arrayDimensions);
      assertTrue(t.isTypeVariable);
      assertEquals("T", t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    assertNull(signature.parameterTypes);
    assertNull(signature.thrownTypes);
  }

  @Test
  public void case2() {
    // int[] method(List<?> list, Set<String> set)
    final var signature = MethodSignature.decode("(Ljava/util/List<*>;Ljava/util/Set<Ljava/lang/String;>;)[I");
    assertNotNull(signature);
    assertNull(signature.typeParameters);
    {
      final var t = signature.returnType;
      assertEquals(1, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals("I", t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    assertNotNull(signature.parameterTypes);
    assertNull(signature.thrownTypes);
  }

  @Test
  public void case3() {
    // <E extends Throwable> void method() throws IOException, E
    final var signature = MethodSignature.decode("<E:Ljava/lang/Throwable;>()V^Ljava/io/IOException;^TE;");
    assertNotNull(signature);
    assertNotNull(signature.typeParameters);
    assertEquals(1, signature.typeParameters.length);
    {
      final var p = signature.typeParameters[0];
      assertEquals("E", p.name);
      assertNotNull(p.classBound);
      {
        final var b = p.classBound;
        assertEquals(0, b.arrayDimensions);
        assertFalse(b.isTypeVariable);
        assertEquals(Type.getDescriptor(Throwable.class), b.name);
        assertNull(b.qualifier);
        assertNull(b.typeArguments);
        assertNull(b.wildcard);
      }
      assertNull(p.interfaceBounds);
    }
    {
      final var t = signature.returnType;
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals("V", t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    assertNull(signature.parameterTypes);
    assertNotNull(signature.thrownTypes);
    assertEquals(2, signature.thrownTypes.length);
    {
      final var t = signature.thrownTypes[0];
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(IOException.class), t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    {
      final var t = signature.thrownTypes[1];
      assertEquals(0, t.arrayDimensions);
      assertTrue(t.isTypeVariable);
      assertEquals("E", t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
  }

}
