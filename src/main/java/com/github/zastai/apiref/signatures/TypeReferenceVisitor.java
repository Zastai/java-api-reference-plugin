package com.github.zastai.apiref.signatures;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.internal.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class TypeReferenceVisitor extends SignatureVisitor {

  public int arrayDimensions;

  public boolean isTypeVariable;

  public @Nullable String name;

  private final @NotNull Consumer<@NotNull TypeReference> processor;

  public @Nullable TypeReference qualifier;

  public @Nullable List<@Nullable TypeReference> typeArguments;

  public @Nullable Character wildcard;

  public TypeReferenceVisitor(@NotNull Consumer<TypeReference> processor) {
    super(Constants.API_VERSION);
    this.processor = processor;
  }

  public TypeReferenceVisitor(char wildcard, @NotNull Consumer<TypeReference> processor) {
    this(processor);
    if (wildcard == '+' || wildcard == '-') {
      this.wildcard = wildcard;
    }
  }

  private void finish() {
    final TypeReference typeReference = new TypeReference(this);
    this.processor.accept(typeReference);
    this.reset();
  }

  private void reset() {
    this.arrayDimensions = 0;
    this.isTypeVariable = false;
    this.name = null;
    this.qualifier = null;
    this.typeArguments = null;
    this.wildcard = null;
  }

  private void trace(@NotNull String message) {
    //System.err.printf("type[%08x]: %s%n", this.hashCode(), message);
  }

  @NotNull
  @Override
  public SignatureVisitor visitArrayType() {
    this.trace("array-type");
    ++this.arrayDimensions;
    return this;
  }

  @Override
  public void visitBaseType(char descriptor) {
    this.trace("base-type(%c)".formatted(descriptor));
    if (this.name != null) {
      final var msg = "Got a base type (%c) but this signature type already has a name (%s).".formatted(descriptor, this.name);
      throw new IllegalStateException(msg);
    }
    this.name = Character.toString(descriptor);
    this.finish();
  }

  @NotNull
  @Override
  public SignatureVisitor visitClassBound() {
    throw new UnsupportedOperationException("A type reference in a signature should not include a class bound.");
  }

  @Override
  public void visitClassType(String name) {
    this.trace("class-type(%s)".formatted(name));
    if (this.name != null) {
      final var msg = "Got a class type (%s) but this signature type already has a name (%s).".formatted(name, this.name);
      throw new IllegalStateException(msg);
    }
    this.name = ASMUtil.descriptorForName(name);
  }

  @Override
  public void visitEnd() {
    this.trace("end");
    this.finish();
  }

  @NotNull
  @Override
  public SignatureVisitor visitExceptionType() {
    throw new UnsupportedOperationException("A type reference in a signature should not include an exception type.");
  }

  @Override
  public void visitFormalTypeParameter(String name) {
    final String msg = "A type reference in a signature should not include formal type parameters (got '%s').".formatted(name);
    throw new UnsupportedOperationException(msg);
  }

  @Override
  public void visitInnerClassType(String name) {
    this.trace("inner-class-type(%s)".formatted(name));
    if (this.name == null) {
      final var msg = "Got an inner class type (%s) but this type reference does not have a name yet.".formatted(name);
      throw new IllegalStateException(msg);
    }
    final TypeReference qualifier = new TypeReference(this);
    this.reset();
    this.name = name;
    this.qualifier = qualifier;
  }

  @NotNull
  @Override
  public SignatureVisitor visitInterface() {
    throw new UnsupportedOperationException("A type reference in a signature should not include a base interface.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitInterfaceBound() {
    throw new UnsupportedOperationException("A type reference in a signature should not include an interface bound.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitParameterType() {
    throw new UnsupportedOperationException("A type reference in a signature should not include a parameter type.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitReturnType() {
    throw new UnsupportedOperationException("A type reference in a signature should not include a return type.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitSuperclass() {
    throw new UnsupportedOperationException("A type reference in a signature should not include a base class.");
  }

  @Override
  public void visitTypeArgument() {
    this.trace("type-argument");
    if (this.typeArguments == null) {
      this.typeArguments = new ArrayList<>();
    }
    this.typeArguments.add(null);
  }

  @NotNull
  @Override
  public SignatureVisitor visitTypeArgument(char wildcard) {
    this.trace("type-argument(%s)".formatted(wildcard));
    if (this.typeArguments == null) {
      this.typeArguments = new ArrayList<>();
    }
    return new TypeReferenceVisitor(wildcard, this.typeArguments::add);
  }

  @Override
  public void visitTypeVariable(String name) {
    this.trace("type-variable(%s)".formatted(name));
    if (this.name != null) {
      final var msg = "Got a type variable (%s) but this signature type already has a name (%s).".formatted(name, this.name);
      throw new IllegalStateException(msg);
    }
    this.isTypeVariable = true;
    this.name = name;
    this.finish();
  }

}
