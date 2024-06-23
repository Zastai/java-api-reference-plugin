package com.github.zastai.apiref.signatures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureReader;

/** A decoded generics signature for a method. */
public final class MethodSignature {

  /** The types of this method's parameters. */
  public @NotNull TypeReference @Nullable [] parameterTypes;

  /** The return type for this method. */
  public @NotNull TypeReference returnType;

  /** The type of exception(s) thrown by this method. */
  public @NotNull TypeReference @Nullable [] thrownTypes;

  /** The formal type parameters associated with this method. */
  public @NotNull FormalTypeParameter @Nullable [] typeParameters;

  private MethodSignature(@NotNull Visitor visitor) {
    if (visitor.baseClass != null) {
      throw new IllegalArgumentException("A method signature should not include a base class.");
    }
    if (visitor.baseInterfaces != null) {
      throw new IllegalArgumentException("A method signature should not include base interfaces.");
    }
    if (visitor.returnType == null) {
      throw new IllegalArgumentException("A method signature must include a return type.");
    }
    this.parameterTypes = visitor.parameterTypes == null ? null : visitor.parameterTypes.toArray(TypeReference[]::new);
    this.returnType = visitor.returnType;
    this.thrownTypes = visitor.thrownTypes == null ? null : visitor.thrownTypes.toArray(TypeReference[]::new);
    this.typeParameters = visitor.typeParameters == null ? null : visitor.typeParameters.toArray(FormalTypeParameter[]::new);
  }

  /**
   * Decodes a method signature.
   *
   * @param signature The method signature to decode.
   *
   * @return The decoded method signature.
   */
  @NotNull
  public static MethodSignature decode(@NotNull String signature) {
    final var reader = new SignatureReader(signature);
    final var visitor = new Visitor();
    reader.accept(visitor);
    return new MethodSignature(visitor);
  }

}
