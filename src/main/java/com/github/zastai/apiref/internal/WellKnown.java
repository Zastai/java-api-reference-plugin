package com.github.zastai.apiref.internal;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;

/** Well known values. */
public interface WellKnown {

  /** Well-known descriptors. */
  interface Descriptors {

    /** The (internal) type name for {@link Annotation}. */
    @NotNull
    String ANNOTATION = Type.getDescriptor(Annotation.class);

    /** The (internal) type name for {@link Enum}. */
    @NotNull
    String ENUM = Type.getDescriptor(Enum.class);

    /** The (internal) type name for {@link Object}. */
    @NotNull
    String OBJECT = Type.getDescriptor(Object.class);

    /** The (internal) type name for {@link Record}. */
    @NotNull
    String RECORD = Type.getDescriptor(Record.class);

  }

  /** Well-known names. */
  interface Names {

    /** The (internal) type name for {@link Annotation}. */
    @NotNull
    String ANNOTATION = Type.getInternalName(Annotation.class);

    /** The special method name used for constructors. */
    @NotNull
    String CONSTRUCTOR = "<init>";

    /** The (internal) type name for {@link Enum}. */
    @NotNull
    String ENUM = Type.getInternalName(Enum.class);

    /** The name of the "module info" pseudo-class. */
    @NotNull
    String MODULE_INFO = "module-info";

    /** The (internal) type name for {@link Object}. */
    @NotNull
    String OBJECT = Type.getInternalName(Object.class);

    /** The name of the "package info" pseudo-class. */
    @NotNull
    String PACKAGE_INFO = "package-info";

    /** The (internal) type name for {@link Record}. */
    @NotNull
    String RECORD = Type.getInternalName(Record.class);
  }

}
