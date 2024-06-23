package com.github.zastai.apiref.tests;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.internal.WellKnown;
import com.github.zastai.apiref.signatures.ClassSignature;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassSignatureTests {

  @Test
  public void case1() {
    // class C<T1, T2>
    final var signature = ClassSignature.decode("<T1:Ljava/lang/Object;T2:Ljava/lang/Object;>Ljava/lang/Object;");
    assertNotNull(signature);
    assertNotNull(signature.typeParameters);
    assertEquals(2, signature.typeParameters.length);
    {
      final var p = signature.typeParameters[0];
      assertEquals("T1", p.name);
      {
        final var t = p.classBound;
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(WellKnown.Descriptors.OBJECT, t.name);
        assertNull(t.qualifier);
        assertNull(t.typeArguments);
        assertNull(t.wildcard);
      }
      assertNull(p.interfaceBounds);
    }
    {
      final var p = signature.typeParameters[1];
      assertEquals("T2", p.name);
      {
        final var t = p.classBound;
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(WellKnown.Descriptors.OBJECT, t.name);
        assertNull(t.qualifier);
        assertNull(t.typeArguments);
        assertNull(t.wildcard);
      }
      assertNull(p.interfaceBounds);
    }
    {
      final var t = signature.baseClass;
      assertNotNull(t);
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(WellKnown.Descriptors.OBJECT, t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    assertNull(signature.baseInterfaces);
  }

  @Test
  public void case2() {
    // class C extends HashSet<String> implements AutoCloseable, Supplier<String>
    final var signature = ClassSignature.decode("Ljava/util/HashSet<Ljava/lang/String;>;Ljava/lang/AutoCloseable;Ljava/util/function/Supplier<Ljava/lang/String;>;");
    assertNotNull(signature);
    assertNull(signature.typeParameters);
    {
      final var t = signature.baseClass;
      assertNotNull(t);
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(HashSet.class), t.name);
      assertNull(t.qualifier);
      assertNotNull(t.typeArguments);
      assertEquals(1, t.typeArguments.length);
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
      assertNull(t.wildcard);
    }
    assertNotNull(signature.baseInterfaces);
    assertEquals(2, signature.baseInterfaces.length);
    {
      final var t = signature.baseInterfaces[0];
      assertNotNull(t);
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(AutoCloseable.class), t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    {
      final var t = signature.baseInterfaces[1];
      assertNotNull(t);
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(Type.getDescriptor(Supplier.class), t.name);
      assertNull(t.qualifier);
      assertNotNull(t.typeArguments);
      assertEquals(1, t.typeArguments.length);
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
      assertNull(t.wildcard);
    }
  }

  @Test
  public void case3() {
    // class C<T extends List<?> & AutoCloseable & C.Tag & C.GenericTag<String>>
    final var signature = ClassSignature.decode("<T::Ljava/util/List<*>;:Ljava/lang/AutoCloseable;:LC$Tag;:LC$GenericTag<Ljava/lang/String;>;>Ljava/lang/Object;");
    assertNotNull(signature);
    assertNotNull(signature.typeParameters);
    assertEquals(1, signature.typeParameters.length);
    {
      final var p = signature.typeParameters[0];
      assertEquals("T", p.name);
      assertNotNull(p.interfaceBounds);
      assertEquals(4, p.interfaceBounds.length);
      {
        final var t = p.interfaceBounds[0];
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(Type.getDescriptor(List.class), t.name);
        assertNull(t.qualifier);
        assertNotNull(t.typeArguments);
        assertEquals(1, t.typeArguments.length);
        assertNull(t.typeArguments[0]);
        assertNull(t.wildcard);
      }
      {
        final var t = p.interfaceBounds[1];
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(Type.getDescriptor(AutoCloseable.class), t.name);
        assertNull(t.qualifier);
        assertNull(t.typeArguments);
        assertNull(t.wildcard);
      }
      {
        final var t = p.interfaceBounds[2];
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(ASMUtil.descriptorForName("C$Tag"), t.name);
        assertNull(t.qualifier);
        assertNull(t.typeArguments);
        assertNull(t.wildcard);
      }
      {
        final var t = p.interfaceBounds[3];
        assertNotNull(t);
        assertEquals(0, t.arrayDimensions);
        assertFalse(t.isTypeVariable);
        assertEquals(ASMUtil.descriptorForName("C$GenericTag"), t.name);
        assertNull(t.qualifier);
        assertNotNull(t.typeArguments);
        assertEquals(1, t.typeArguments.length);
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
        assertNull(t.wildcard);
      }
    }
    {
      final var t = signature.baseClass;
      assertNotNull(t);
      assertEquals(0, t.arrayDimensions);
      assertFalse(t.isTypeVariable);
      assertEquals(WellKnown.Descriptors.OBJECT, t.name);
      assertNull(t.qualifier);
      assertNull(t.typeArguments);
      assertNull(t.wildcard);
    }
    assertNull(signature.baseInterfaces);
  }

}
