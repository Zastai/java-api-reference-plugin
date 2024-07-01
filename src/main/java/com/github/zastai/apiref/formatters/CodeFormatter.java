package com.github.zastai.apiref.formatters;

import com.github.zastai.apiref.internal.Constants;
import com.github.zastai.apiref.model.JavaApplication;
import com.github.zastai.apiref.model.JavaClass;
import com.github.zastai.apiref.model.JavaModule;
import com.github.zastai.apiref.model.JavaPackage;
import com.github.zastai.apiref.model.JavaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.PrintStream;
import java.util.Collection;

/** A class for formatting a Java application's (public) API. */
public abstract class CodeFormatter {

  /** The package that is currently being processed (if there is one). */
  @Nullable
  protected JavaPackage currentPackage;

  /** The class that is currently being processed (if there is one). */
  @Nullable
  protected JavaClass currentClass;

  /**
   * Creates a new code formatter.
   *
   * @param out The stream that should receive the formatted output.
   */
  protected CodeFormatter(@NotNull PrintStream out) {
    this.out = out;
  }

  /** An array of 16 space characters. */
  private static final char[] SIXTEEN_BLANKS = new char[] {
    ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '
  };

  /** The current indentation level. */
  private int indentLevel = 0;

  /** The stream this formatter should use for output. */
  @NotNull
  protected final PrintStream out;

  /**
   * Formats the public API for a Java application and writes it to the specified stream.
   * <p>
   * This consists of:
   * <ol>
   *   <li>A file-level header (via {@link #writeFileHeader()}).</li>
   *   <li>A list of all the module definitions (via {@link #writeModuleList(Collection)}).</li>
   *   <li>A list of all the top-level types (via {@link #writeTypeList(Collection, JavaPackage)}}).</li>
   *   <li>A list of all packages (via {@link #writePackageList(Collection)}).</li>
   *   <li>A file-level footer (via {@link #writeFileFooter()}).</li>
   * </ol>
   *
   * @param application The application whose public API should be formatted.
   */
  protected void writePublicApi(@NotNull JavaApplication application) {
    this.writeFileHeader();
    this.writeModuleList(application.modules.values());
    this.writeTypeList(application.topLevelTypes.values(), null);
    this.writePackageList(application.packages.values());
    this.writeFileFooter();
  }

  /** Increases the indentation level by 1. */
  protected void indent() {
    ++this.indentLevel;
  }

  /** Decreases the indentation level by 1. */
  protected void outdent() {
    if (this.indentLevel == 0) {
      throw new IllegalStateException("Indentation imbalance detected.");
    }
    --this.indentLevel;
  }

  /**
   * Determines whether a particular annotation should be retained in the output.
   *
   * @param an The node for the annotation.
   *
   * @return {@code true} when the annotation should be included in the formatted output; {@code false} if it should be omitted.
   */
  protected boolean retain(@NotNull AnnotationNode an) {
    // TODO: Load the annotation (either from a .class file in our input, or from the classpath), and check for @Documented.
    return true;
  }

  /**
   * Writes out the value of a property on an annotation.
   * <p>
   * This can be a literal of a primitive type, an enum value, a class name, or another annotation.
   *
   * @param value The value to write.
   */
  protected void writeAnnotationValue(@Nullable Object value) {
    if (value == null) {
      this.writeNull();
    }
    else if (value instanceof AnnotationNode an) {
      this.writeAnnotation(an);
    }
    else if (value instanceof Boolean b) {
      this.writeLiteral(b);
    }
    else if (value instanceof Byte b) {
      this.writeLiteral(b);
    }
    else if (value instanceof Character c) {
      this.writeLiteral(c);
    }
    else if (value instanceof Double d) {
      this.writeLiteral(d);
    }
    else if (value instanceof Float f) {
      this.writeLiteral(f);
    }
    else if (value instanceof Integer i) {
      this.writeLiteral(i);
    }
    else if (value instanceof Long l) {
      this.writeLiteral(l);
    }
    else if (value instanceof Short s) {
      this.writeLiteral(s);
    }
    else if (value instanceof String s) {
      this.writeLiteral(s);
    }
    else if (value instanceof String[] enumInfo && enumInfo.length == 2) {
      this.writeEnumValue(enumInfo[0], enumInfo[1]);
    }
    else if (value instanceof Type t) {
      this.writeClassName(t);
    }
    else {
      this.writeInlineComment("TODO: handle %s '%s'".formatted(value.getClass(), value));
    }
  }

  /**
   * Writes out a single annotation.
   *
   * @param an The node for the annotation.
   */
  protected abstract void writeAnnotation(@NotNull AnnotationNode an);

  /**
   * Writes out the annotations attached to a class.
   *
   * @param cn The node for the class.
   */
  protected void writeAnnotations(@NotNull ClassNode cn) {
    this.writeAnnotations(cn.visibleAnnotations);
    this.writeAnnotations(cn.invisibleAnnotations);
  }

  /**
   * Writes out a set of annotations (using {@link #writeAnnotation(AnnotationNode)}).
   *
   * @param annotations The annotations.
   */
  protected void writeAnnotations(@Nullable Collection<AnnotationNode> annotations) {
    if (annotations != null) {
      annotations.stream().filter(this::retain).forEach(an -> {
        this.writeIndent();
        this.writeAnnotation(an);
        this.out.println();
      });
    }
  }

  /**
   * Writes out a class; this consists of a header (via {@link #writeClassHeader(JavaClass)}), its contents (via
   * {@link #writeClassContents(JavaClass)}), and a footer (via {@link #writeClassFooter(JavaClass)}).
   *
   * @param jc The class.
   */
  protected void writeClass(@NotNull JavaClass jc) {
    final var previousClass = this.currentClass;
    this.currentClass = jc;
    this.out.println();
    this.writeClassHeader(jc);
    this.writeClassContents(jc);
    this.writeClassFooter(jc);
    this.currentClass = previousClass;
  }

  /**
   * Writes out the contents of a class.
   * <p>
   * This consists of:
   * <ol>
   *   <li>Any fields declared by the class (via {@link #writeFieldList(Collection)}).</li>
   *   <li>Any methods declared by the class (via {@link #writeMethodList(Collection)}).</li>
   *   <li>Any types nested inside the class (via {@link #writeNestedClassList(Collection)}).</li>
   * </ol>
   *
   * @param jc The class.
   */
  protected void writeClassContents(@NotNull JavaClass jc) {
    // FIXME: We might need to pass the ClassNode through too.
    this.writeFieldList(jc.fields);
    this.writeMethodList(jc.methods);
    this.writeNestedClassList(jc.nestedClasses());
  }

  /**
   * Writes out a footer for a class.
   *
   * @param jc The class.
   */
  protected void writeClassFooter(@NotNull JavaClass jc) {
    // default: no footer
  }

  /**
   * Writes out a header for a class.
   *
   * @param jc The class.
   */
  protected void writeClassHeader(@NotNull JavaClass jc) {
    // default: no header
  }

  /**
   * Writes out a reference to the class for a type.
   *
   * @param type The type.
   */
  protected abstract void writeClassName(@NotNull Type type);

  /** Writes out the end of a set of enum values. */
  protected void writeEndOfEnumValues() {
    // Default: no special marker
  }

  /**
   * Writes out an enum value.
   *
   * @param descriptor The type descriptor for the enum.
   * @param member     The name of the enum member.
   */
  protected abstract void writeEnumValue(@NotNull String descriptor, @NotNull String member);

  /**
   * Writes out a single field.
   *
   * @param fn The node for the field.
   */
  protected abstract void writeField(@NotNull FieldNode fn);

  /**
   * Writes out a list of fields; this consists of a header (via {@link #writeFieldListHeader(Collection)}), its contents (via
   * {@link #writeFieldListContents(Collection)}), and a footer (via {@link #writeFieldListFooter(Collection)}).
   *
   * @param list The fields.
   */
  protected void writeFieldList(@NotNull Collection<FieldNode> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeFieldListHeader(list);
    this.writeFieldListContents(list);
    this.writeFieldListFooter(list);
  }

  /**
   * Writes out the contents of a list of fields (via {@link #writeField(FieldNode)}).
   * <p>
   * For an enum, the enum values are expected to be first in the list; if they are present, {@link #writeStartOfEnumValues()} and
   * {@link #writeEndOfEnumValues()} will be invoked around them.
   *
   * @param list The list of fields.
   */
  protected void writeFieldListContents(@NotNull Collection<FieldNode> list) {
    // Assumption: the enum values will always be first.
    var enumValueSeen = false;
    for (final var item : list) {
      if (item.access == Constants.ACC_ENUM_VALUE) {
        if (!enumValueSeen) {
          this.writeStartOfEnumValues();
        }
        enumValueSeen = true;
      }
      else if (enumValueSeen) {
        enumValueSeen = false;
        this.writeEndOfEnumValues();
      }
      this.writeField(item);
    }
  }

  /**
   * Writes out a footer for a list of fields.
   *
   * @param list The list of fields.
   */
  protected void writeFieldListFooter(@NotNull Collection<FieldNode> list) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of fields.
   *
   * @param list The list of fields.
   */
  protected void writeFieldListHeader(@NotNull Collection<FieldNode> list) {
    // default: no header
  }

  /** Writes out a footer for the output file. */
  protected void writeFileFooter() {
    // default: no footer
  }

  /** Writes out a header for the output file. */
  protected void writeFileHeader() {
    this.writeLineComment("=== Generated API Reference === DO NOT EDIT ===");
  }

  /** Writes out whitespace to represent the current indentation level. */
  protected void writeIndent() {
    if (this.indentLevel == 0) {
      return;
    }
    var spaces = this.indentLevel * 2;
    for (; spaces >= 16; spaces -= 16) {
      this.out.print(CodeFormatter.SIXTEEN_BLANKS);
    }
    for (; spaces > 0; --spaces) {
      this.out.print(' ');
    }
  }

  /**
   * Writes out an inline comment (with neither leading nor trailing whitespace).
   *
   * @param comment The comment text.
   */
  protected void writeInlineComment(@NotNull String comment) {
    this.out.print("/* ");
    this.out.print(comment);
    this.out.print(" */");
  }

  /**
   * Writes out a line comment (including leading indentation), ending the current line.
   *
   * @param comment The comment text.
   */
  protected void writeLineComment(@NotNull String comment) {
    this.writeIndent();
    this.out.print("// ");
    this.out.print(comment);
    this.out.println();
  }

  /**
   * Writes out a boolean literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Boolean literal);

  /**
   * Writes out an 8-bit integer literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Byte literal);

  /**
   * Writes out a character literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Character literal);

  /**
   * Writes out a double-precision floating-point literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Double literal);

  /**
   * Writes out a single-precision floating-point literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Float literal);

  /**
   * Writes out a 32-bit integer literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Integer literal);

  /**
   * Writes out a 64-bit integer literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Long literal);

  /**
   * Writes out a 16-bit integer literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull Short literal);

  /**
   * Writes out a string literal.
   *
   * @param literal The literal.
   */
  protected abstract void writeLiteral(@NotNull String literal);

  /**
   * Writes out a single method.
   *
   * @param mn The node for the method.
   */
  protected abstract void writeMethod(@NotNull MethodNode mn);

  /**
   * Writes out a list of methods; this consists of a header (via {@link #writeMethodListHeader(Collection)}), its contents (via
   * {@link #writeMethodListContents(Collection)}), and a footer (via {@link #writeMethodListFooter(Collection)}).
   *
   * @param list The methods.
   */
  protected void writeMethodList(@NotNull Collection<MethodNode> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeMethodListHeader(list);
    this.writeMethodListContents(list);
    this.writeMethodListFooter(list);
  }

  /**
   * Writes out the contents of a list of methods (via {@link #writeMethod(MethodNode)}).
   *
   * @param list The list of methods.
   */
  protected void writeMethodListContents(@NotNull Collection<MethodNode> list) {
    list.forEach(this::writeMethod);
  }

  /**
   * Writes out a footer for a list of methods.
   *
   * @param list The list of methods.
   */
  protected void writeMethodListFooter(@NotNull Collection<MethodNode> list) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of methods.
   *
   * @param list The list of methods.
   */
  protected void writeMethodListHeader(@NotNull Collection<MethodNode> list) {
    // default: no header
  }

  /**
   * Writes out a module; this consists of a header (via {@link #writeModuleHeader(JavaModule)}), its contents (via
   * {@link #writeModuleContents(JavaModule)}), and a footer (via {@link #writeModuleFooter(JavaModule)}).
   *
   * @param jm The module.
   */
  protected void writeModule(@NotNull JavaModule jm) {
    this.out.println();
    this.writeModuleHeader(jm);
    this.writeModuleContents(jm);
    this.writeModuleFooter(jm);
  }

  /**
   * Writes out the contents of a module.
   *
   * @param jm The module.
   */
  protected abstract void writeModuleContents(@NotNull JavaModule jm);

  /**
   * Writes out a footer for a module.
   *
   * @param jm The module.
   */
  protected void writeModuleFooter(@NotNull JavaModule jm) {
    // default: no footer
  }

  /**
   * Writes out a header for a module.
   *
   * @param jm The module.
   */
  protected void writeModuleHeader(@NotNull JavaModule jm) {
    // default: no header
  }

  /**
   * Writes out a list of modules; this consists of a header (via {@link #writeModuleListHeader(Collection)}), its contents (via
   * {@link #writeModuleListContents(Collection)}), and a footer (via {@link #writeModuleListFooter(Collection)}).
   *
   * @param list The list of modules.
   */
  protected void writeModuleList(@NotNull Collection<JavaModule> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeModuleListHeader(list);
    this.writeModuleListContents(list);
    this.writeModuleListFooter(list);
  }

  /**
   * Writes out the contents of a list of modules (via {@link #writeModule(JavaModule)}).
   *
   * @param list The list of modules.
   */
  protected void writeModuleListContents(@NotNull Collection<JavaModule> list) {
    list.forEach(this::writeModule);
  }

  /**
   * Writes out a footer for a list of modules.
   *
   * @param list The list of modules.
   */
  protected void writeModuleListFooter(@NotNull Collection<JavaModule> list) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of modules.
   *
   * @param list The list of modules.
   */
  protected void writeModuleListHeader(@NotNull Collection<JavaModule> list) {
    // default: no header
  }

  /**
   * Writes out a nested class.
   *
   * @param jc The nested class.
   */
  protected void writeNestedClass(@NotNull JavaClass jc) {
    this.writeClass(jc);
  }

  /**
   * Writes out a list of nested classes; this consists of a header (via {@link #writeNestedClassListHeader(Collection)}), its
   * contents (via {@link #writeNestedClassListContents(Collection)}), and a footer (via
   * {@link #writeNestedClassListFooter(Collection)}).
   *
   * @param list The list of nested classes.
   */
  protected void writeNestedClassList(@NotNull Collection<JavaClass> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeNestedClassListHeader(list);
    this.writeNestedClassListContents(list);
    this.writeNestedClassListFooter(list);
  }

  /**
   * Writes out the contents of a list of nested classes (via {@link #writeNestedClass(JavaClass)}).
   *
   * @param list The list of nested classes.
   */
  protected void writeNestedClassListContents(@NotNull Collection<JavaClass> list) {
    list.forEach(this::writeNestedClass);
  }

  /**
   * Writes out a footer for a list of nested classes.
   *
   * @param list The list of nested classes.
   */
  protected void writeNestedClassListFooter(@NotNull Collection<JavaClass> list) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of nested classes.
   *
   * @param list The list of nested classes.
   */
  protected void writeNestedClassListHeader(@NotNull Collection<JavaClass> list) {
    this.out.println();
    this.writeLineComment("Nested Types");
  }

  /** Writes out the special {@code null} value. */
  protected void writeNull() {
    this.out.print("null");
  }

  /**
   * Writes out a package; this consists of a header (via {@link #writePackageHeader(JavaPackage)}), its contents (via
   * {@link #writePackageContents(JavaPackage)}), and a footer (via {@link #writePackageFooter(JavaPackage)}).
   *
   * @param jp The package.
   */
  protected void writePackage(@NotNull JavaPackage jp) {
    this.out.println();
    this.writePackageHeader(jp);
    this.currentPackage = jp;
    this.writePackageContents(jp);
    this.currentPackage = null;
    this.writePackageFooter(jp);
  }

  /**
   * Writes out the contents of a package; this consists of a list of types (via {@link #writeTypeList(Collection, JavaPackage)}).
   *
   * @param jp The package.
   */
  protected void writePackageContents(@NotNull JavaPackage jp) {
    this.writeTypeList(jp.types.values(), jp);
  }

  /**
   * Writes out a footer for a package.
   *
   * @param jp The package.
   */
  protected void writePackageFooter(@NotNull JavaPackage jp) {
    // default: no footer
  }

  /**
   * Writes out a header for a package.
   *
   * @param jp The package.
   */
  protected void writePackageHeader(@NotNull JavaPackage jp) {
    // default: no header
  }

  /**
   * Writes out a list of packages; this consists of a header (via {@link #writePackageListHeader(Collection)}), its contents (via
   * {@link #writePackageListContents(Collection)}), and a footer (via {@link #writePackageListFooter(Collection)}).
   *
   * @param list The list of packages.
   */
  protected void writePackageList(@NotNull Collection<JavaPackage> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writePackageListHeader(list);
    this.writePackageListContents(list);
    this.writePackageListFooter(list);
  }

  /**
   * Writes out the contents of a list of packages (via {@link #writePackage(JavaPackage)}).
   *
   * @param list The list of packages.
   */
  protected void writePackageListContents(@NotNull Collection<JavaPackage> list) {
    list.forEach(this::writePackage);
  }

  /**
   * Writes out a footer for a list of packages.
   *
   * @param list The list of packages.
   */
  protected void writePackageListFooter(@NotNull Collection<JavaPackage> list) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of packages.
   *
   * @param list The list of packages.
   */
  protected void writePackageListHeader(@NotNull Collection<JavaPackage> list) {
    // default: no header
  }

  /** Writes out the start of a set of enum values. */
  protected void writeStartOfEnumValues() {
    // Default: no special marker
  }

  /**
   * Writes out a type; this consists of a header (via {@link #writeTypeHeader(JavaType)}), its contents (via
   * {@link #writeTypeContents(JavaType)}), and a footer (via {@link #writeTypeFooter(JavaType)}).
   *
   * @param jt The type.
   */
  protected void writeType(@NotNull JavaType jt) {
    this.writeTypeHeader(jt);
    this.writeTypeContents(jt);
    this.writeTypeFooter(jt);
  }

  /**
   * Writes out the contents of a type; this consists of a list of classes (via {@link #writeClass(JavaClass)}).
   *
   * @param jt The type.
   */
  protected void writeTypeContents(@NotNull JavaType jt) {
    jt.classes.values().forEach(this::writeClass);
  }

  /**
   * Writes out a footer for a type.
   *
   * @param jt The type.
   */
  protected void writeTypeFooter(@NotNull JavaType jt) {
    // default: no footer
  }

  /**
   * Writes out a header for a type.
   *
   * @param jt The type.
   */
  protected void writeTypeHeader(@NotNull JavaType jt) {
    // default: no header
  }

  /**
   * Writes out a list of types; this consists of a header (via {@link #writeTypeListHeader(Collection, JavaPackage)}), its contents
   * (via {@link #writeTypeListContents(Collection, JavaPackage)}), and a footer (via
   * {@link #writeTypeListFooter(Collection, JavaPackage)}).
   *
   * @param list The list of types.
   * @param jp   The package containing the types, or {@code null} for a list of top-level types.
   */
  protected void writeTypeList(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    if (list.isEmpty()) {
      return;
    }
    this.writeTypeListHeader(list, jp);
    this.writeTypeListContents(list, jp);
    this.writeTypeListFooter(list, jp);
  }

  /**
   * Writes out the contents of a list of types (via {@link #writeType(JavaType)}).
   *
   * @param list The list of types.
   * @param jp   The package containing the types, or {@code null} for a list of top-level types.
   */
  protected void writeTypeListContents(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    list.forEach(this::writeType);
  }

  /**
   * Writes out a footer for a list of types.
   *
   * @param list The list of types.
   * @param jp   The package containing the types, or {@code null} for a list of top-level types.
   */
  protected void writeTypeListFooter(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    // default: no footer
  }

  /**
   * Writes out a header for a list of types.
   *
   * @param list The list of types.
   * @param jp   The package containing the types, or {@code null} for a list of top-level types.
   */
  protected void writeTypeListHeader(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    // default: no header
  }

  /**
   * Writes out the name of a type.
   *
   * @param descriptor The type's descriptor.
   */
  protected void writeTypeName(@NotNull String descriptor) {
    this.writeTypeName(Type.getType(descriptor));
  }

  /**
   * Writes out the name of a type.
   *
   * @param type The type.
   */
  protected abstract void writeTypeName(@NotNull Type type);

}
