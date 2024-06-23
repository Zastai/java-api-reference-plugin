package com.github.zastai.apiref.signatures;

import com.github.zastai.apiref.internal.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

final class Visitor extends SignatureVisitor {

  @Nullable
  public TypeReference baseClass;

  public @Nullable List<@NotNull TypeReference> baseInterfaces;

  public @Nullable List<@Nullable TypeReference> parameterTypes;

  public @Nullable TypeReference returnType;

  public @Nullable List<@NotNull TypeReference> thrownTypes;

  public @Nullable List<@NotNull FormalTypeParameter> typeParameters;

  Visitor() {
    super(Constants.API_VERSION);
  }

  private @Nullable String typeParameter;

  private @Nullable TypeReference classBound;

  public @Nullable List<@NotNull TypeReference> interfaceBounds;

  private void finishTypeParameter() {
    if (this.typeParameter == null) {
      return;
    }
    if (this.typeParameters == null) {
      this.typeParameters = new ArrayList<>();
    }
    this.typeParameters.add(new FormalTypeParameter(this.typeParameter, this.classBound, this.interfaceBounds));
    this.typeParameter = null;
    this.classBound = null;
    this.interfaceBounds = null;
  }

  private void trace(@NotNull String message) {
    //System.err.printf("signature[%08x]: %s%n", this.hashCode(), message);
  }

  @Override
  public SignatureVisitor visitArrayType() {
    throw new UnsupportedOperationException("A signature should not include an array type at the top level.");
  }

  @Override
  public void visitBaseType(char descriptor) {
    throw new UnsupportedOperationException("A signature should not include a base type at the top level.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitClassBound() {
    if (this.typeParameter == null) {
      throw new UnsupportedOperationException("A signature should not include an interface bound outside a formal type parameter.");
    }
    if (this.classBound != null) {
      final var msg = "Encountered a second class bound for formal type parameter '%s' in a signature.";
      throw new IllegalStateException(msg.formatted(this.typeParameter));
    }
    this.trace("class-bound");
    return new TypeReferenceVisitor(tr -> this.classBound = tr);
  }

  @Override
  public void visitClassType(@NotNull String name) {
    throw new UnsupportedOperationException("A signature should not include a class type at the top level.");
  }

  @Override
  public void visitEnd() {
    this.trace("end");
    this.finishTypeParameter();
  }

  @NotNull
  @Override
  public SignatureVisitor visitExceptionType() {
    this.trace("exception-type");
    this.finishTypeParameter();
    if (this.thrownTypes == null) {
      this.thrownTypes = new ArrayList<>();
    }
    return new TypeReferenceVisitor(this.thrownTypes::add);
  }

  @Override
  public void visitFormalTypeParameter(@NotNull String name) {
    this.trace("formal-type-parameter(%s)".formatted(name));
    this.finishTypeParameter();
    this.typeParameter = name;
  }

  @Override
  public void visitInnerClassType(@NotNull String name) {
    throw new UnsupportedOperationException("A signature should not include an inner class type at the top level.");
  }

  @NotNull
  @Override
  public SignatureVisitor visitInterface() {
    this.trace("interface");
    this.finishTypeParameter();
    if (this.baseInterfaces == null) {
      this.baseInterfaces = new ArrayList<>();
    }
    return new TypeReferenceVisitor(this.baseInterfaces::add);
  }

  @NotNull
  @Override
  public SignatureVisitor visitInterfaceBound() {
    if (this.typeParameter == null) {
      throw new UnsupportedOperationException("A signature should not include an interface bound outside a formal type parameter.");
    }
    this.trace("interface-bound");
    if (this.interfaceBounds == null) {
      this.interfaceBounds = new ArrayList<>();
    }
    return new TypeReferenceVisitor(this.interfaceBounds::add);
  }

  @NotNull
  @Override
  public SignatureVisitor visitParameterType() {
    this.trace("parameter-type");
    this.finishTypeParameter();
    if (this.parameterTypes == null) {
      this.parameterTypes = new ArrayList<>();
    }
    return new TypeReferenceVisitor(this.parameterTypes::add);
  }

  @NotNull
  @Override
  public SignatureVisitor visitReturnType() {
    if (this.returnType != null) {
      throw new IllegalStateException("Encountered a second return type in a signature.");
    }
    this.trace("return-type");
    this.finishTypeParameter();
    return new TypeReferenceVisitor(tr -> this.returnType = tr);
  }

  @NotNull
  @Override
  public SignatureVisitor visitSuperclass() {
    if (this.baseClass != null) {
      throw new IllegalStateException("Encountered a second super class in a signature.");
    }
    this.trace("super-class");
    this.finishTypeParameter();
    return new TypeReferenceVisitor(tr -> this.baseClass = tr);
  }

  @Override
  public void visitTypeArgument() {
    throw new UnsupportedOperationException("A signature should not include a type argument at the top level.");
  }

  @Override
  public SignatureVisitor visitTypeArgument(char wildcard) {
    throw new UnsupportedOperationException("A signature should not include a type argument at the top level.");
  }

  @Override
  public void visitTypeVariable(@NotNull String name) {
    throw new UnsupportedOperationException("A signature should not include a type variable at the top level.");
  }

}
