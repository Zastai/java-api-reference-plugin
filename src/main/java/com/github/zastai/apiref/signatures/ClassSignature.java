package com.github.zastai.apiref.signatures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureReader;

/** A decoded generics signature for a class. */
public final class ClassSignature {

  /** The interface(s) this class inherits from. */
  public @NotNull TypeReference @Nullable [] baseInterfaces;

  /** The class extended by this class. */
  public @Nullable TypeReference baseClass;

  /** The formal type parameters associated with this class. */
  public @NotNull FormalTypeParameter @Nullable [] typeParameters;

  private ClassSignature(@NotNull Visitor visitor) {
    if (visitor.parameterTypes != null) {
      throw new IllegalArgumentException("A class signature should not include parameter types.");
    }
    if (visitor.returnType != null) {
      throw new IllegalArgumentException("A class signature should not include a return type.");
    }
    if (visitor.thrownTypes != null) {
      throw new IllegalArgumentException("A class signature should not include thrown exception types.");
    }
    this.baseInterfaces = visitor.baseInterfaces == null ? null : visitor.baseInterfaces.toArray(TypeReference[]::new);
    this.baseClass = visitor.baseClass;
    this.typeParameters = visitor.typeParameters == null ? null : visitor.typeParameters.toArray(FormalTypeParameter[]::new);
  }

  /**
   * Decodes a class signature.
   *
   * @param signature The class signature to decode.
   *
   * @return The decoded class signature.
   */
  @NotNull
  public static ClassSignature decode(@NotNull String signature) {
    final var reader = new SignatureReader(signature);
    final var visitor = new Visitor();
    reader.accept(visitor);
    return new ClassSignature(visitor);
  }

}
