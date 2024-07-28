package com.github.zastai.apiref.model;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.internal.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/** A Java type, consisting of one or more classes (mostly for multi-release jar files). */
public class JavaType {

  @NotNull
  private final SortedMap<Integer, JavaClass> _classes = new TreeMap<>();

  @NotNull
  private final SortedMap<String, JavaType> _nestedTypes = new TreeMap<>();

  /** The classes for this type, grouped by class file version. */
  @NotNull
  @Unmodifiable
  public final SortedMap<Integer, JavaClass> classes = Util.makeUnmodifiable(this._classes);

  /** The full (internal) name of this type, including a package specification. */
  @NotNull
  public final String fullName;

  /** The name of this type. */
  @NotNull
  public final String name;

  /** Any types nested in this one. */
  @NotNull
  @Unmodifiable
  public final SortedMap<String, JavaType> nestedTypes = Util.makeUnmodifiable(this._nestedTypes);

  /** The package containing this type, or {@code null} for a top-level type. */
  @Nullable
  public final JavaPackage parent;

  /** The type in which this one is nested, or {@code null} for a non-nested type. */
  @Nullable
  public final JavaType parentType;

  /**
   * Creates a new Java type.
   *
   * @param name   The name of the type.
   * @param parent The package containing the type, or {@code null} for a top-level type.
   */
  public JavaType(@NotNull String name, @Nullable JavaPackage parent) {
    this(name, parent, null);
  }

  JavaType(@NotNull String name, @Nullable JavaPackage parent, @Nullable JavaType parentType) {
    this.fullName = name;
    this.name = ASMUtil.stripPackage(name);
    this.parent = parent;
    this.parentType = parentType;
  }

  /**
   * Adds a class to this type.
   *
   * @param cn      The class to add.
   * @param verbose Indicates whether verbose output should be enabled.
   */
  public void addClass(@NotNull ClassNode cn, boolean verbose) {
    if (!Objects.equals(cn.name, this.fullName)) {
      throw new IllegalArgumentException("Cannot add a class with a different name (%s != %s).".formatted(cn.name, this.fullName));
    }
    // FIXME: Should this throw if there already is an entry for this class version?
    this._classes.put(cn.version, new JavaClass(cn, this, verbose));
  }

  /**
   * Adds a new nested type to this one.
   *
   * @param name The (internal) name of the nested type.
   *
   * @return The newly-created nested type.
   */
  @NotNull
  public JavaType addNestedType(@NotNull String name) {
    if (!name.startsWith(this.fullName + '$')) {
      final var msg = String.format("A nested type's name (%s) should start with the parent type's name (%s) followed by '$'.",
                                    name, this.fullName);
      throw new IllegalArgumentException(msg);
    }
    final var jt = new JavaType(name, this.parent, this);
    this._nestedTypes.put(name, jt);
    return jt;
  }

}
