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
import java.util.List;

/** A class for formatting a Java application's (public) API. */
public abstract class CodeFormatter {

  @Nullable
  protected JavaPackage currentPackage;

  @Nullable
  protected JavaClass currentClass;

  /**
   * Creates a new code formatter.
   *
   * @param out The stream to write the public API to.
   */
  protected CodeFormatter(@NotNull PrintStream out) {
    this.out = out;
  }

  private static final char[] SIXTEEN_BLANKS = new char[] {
    ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '
  };

  private int indentLevel = 0;

  @NotNull
  protected final PrintStream out;

  /**
   * Formats the public API for a Java application and writes it to the specified stream.
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

  protected void indent() {
    ++this.indentLevel;
  }

  protected boolean retain(@NotNull AnnotationNode an) {
    // TODO: Load the annotation (either from a .class file in our input, or from the classpath), and check for @Documented.
    return true;
  }

  protected void undent() {
    if (this.indentLevel == 0) {
      throw new IllegalStateException("Indentation imbalance detected.");
    }
    --this.indentLevel;
  }

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

  protected abstract void writeAnnotation(@NotNull AnnotationNode an);

  protected void writeAnnotations(@NotNull ClassNode cn) {
    this.writeAnnotations(cn.visibleAnnotations);
    this.writeAnnotations(cn.invisibleAnnotations);
  }

  protected void writeAnnotations(@Nullable List<AnnotationNode> annotations) {
    if (annotations != null) {
      annotations.stream().filter(this::retain).forEach(an -> {
        this.writeIndent();
        this.writeAnnotation(an);
        this.out.println();
      });
    }
  }

  protected void writeClass(@NotNull JavaClass jc) {
    final var previousClass = this.currentClass;
    this.currentClass = jc;
    this.out.println();
    this.writeClassHeader(jc);
    this.writeClassContents(jc);
    this.writeClassFooter(jc);
    this.currentClass = previousClass;
  }

  protected void writeClassContents(@NotNull JavaClass jc) {
    // FIXME: We might need to pass the ClassNode through too.
    this.writeFieldList(jc.fields);
    this.writeMethodList(jc.methods);
    this.writeNestedClassList(jc.nestedClasses());
  }

  protected void writeClassFooter(@NotNull JavaClass jc) {
    // default: no footer
  }

  protected void writeClassHeader(@NotNull JavaClass jc) {
    // default: no header
  }

  protected abstract void writeClassName(@NotNull Type type);

  protected abstract void writeEndOfEnumValues();

  protected abstract void writeEnumValue(@NotNull String descriptor, @NotNull String member);

  protected abstract void writeField(@NotNull FieldNode field);

  protected void writeFieldList(@NotNull Collection<FieldNode> list) {
    if (list.isEmpty()) {
      return;
    }
    // TODO: Filter out enum values and emit those first, using separate methods.
    this.writeFieldListHeader(list);
    this.writeFieldListContents(list);
    this.writeFieldListFooter(list);
  }

  protected void writeFieldListContents(@NotNull Collection<FieldNode> list) {
    // Assumption: the enum values will always be first.
    var enumValueSeen = false;
    for (final var item : list) {
      if (item.access == Constants.ACC_ENUM_VALUE) {
        enumValueSeen = true;
      }
      else if (enumValueSeen) {
        enumValueSeen = false;
        this.writeEndOfEnumValues();
      }
      this.writeField(item);
    }
  }

  protected void writeFieldListFooter(@NotNull Collection<FieldNode> list) {
    // default: no footer
  }

  protected void writeFieldListHeader(@NotNull Collection<FieldNode> list) {
    // default: no header
  }

  protected void writeFileFooter() {
    // default: no footer
  }

  protected void writeFileHeader() {
    this.writeLineComment("=== Generated API Reference === DO NOT EDIT BY HAND ===");
  }

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

  protected void writeInlineComment(@NotNull String comment) {
    this.out.print("/* ");
    this.out.print(comment);
    this.out.print(" */");
  }

  protected void writeLineComment(@NotNull String comment) {
    this.writeIndent();
    this.out.print("// ");
    this.out.print(comment);
    this.out.println();
  }

  protected abstract void writeLiteral(@NotNull Boolean literal);

  protected abstract void writeLiteral(@NotNull Byte literal);

  protected abstract void writeLiteral(@NotNull Character literal);

  protected abstract void writeLiteral(@NotNull Double literal);

  protected abstract void writeLiteral(@NotNull Float literal);

  protected abstract void writeLiteral(@NotNull Integer literal);

  protected abstract void writeLiteral(@NotNull Long literal);

  protected abstract void writeLiteral(@NotNull Short literal);

  protected abstract void writeLiteral(@NotNull String literal);

  protected abstract void writeMethod(@NotNull MethodNode method);

  protected void writeMethodList(@NotNull Collection<MethodNode> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeMethodListHeader(list);
    this.writeMethodListContents(list);
    this.writeMethodListFooter(list);
  }

  protected void writeMethodListContents(@NotNull Collection<MethodNode> list) {
    list.forEach(this::writeMethod);
  }

  protected void writeMethodListFooter(@NotNull Collection<MethodNode> list) {
    // default: no footer
  }

  protected void writeMethodListHeader(@NotNull Collection<MethodNode> list) {
    // default: no header
  }

  protected void writeModule(@NotNull JavaModule jm) {
    this.out.println();
    this.writeModuleHeader(jm);
    this.writeModuleContents(jm);
    this.writeModuleFooter(jm);
  }

  protected abstract void writeModuleContents(@NotNull JavaModule jm);

  protected void writeModuleFooter(@NotNull JavaModule jm) {
    // default: no footer
  }

  protected void writeModuleHeader(@NotNull JavaModule jm) {
    // default: no header
  }

  protected void writeModuleList(@NotNull Collection<JavaModule> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeModuleListHeader(list);
    this.writeModuleListContents(list);
    this.writeModuleListFooter(list);
  }

  protected void writeModuleListContents(@NotNull Collection<JavaModule> list) {
    list.forEach(this::writeModule);
  }

  protected void writeModuleListFooter(@NotNull Collection<JavaModule> list) {
    // default: no footer
  }

  protected void writeModuleListHeader(@NotNull Collection<JavaModule> list) {
    // default: no header
  }

  protected void writeNestedClass(@NotNull JavaClass jc) {
    this.writeClass(jc);
  }

  protected void writeNestedClassList(@NotNull Collection<JavaClass> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writeNestedTypeListHeader(list);
    this.writeNestedTypeClassContents(list);
    this.writeNestedTypeListFooter(list);
  }

  protected void writeNestedTypeClassContents(@NotNull Collection<JavaClass> list) {
    list.forEach(this::writeNestedClass);
  }

  protected void writeNestedTypeListFooter(@NotNull Collection<JavaClass> list) {
    // default: no footer
  }

  protected void writeNestedTypeListHeader(@NotNull Collection<JavaClass> list) {
    this.out.println();
    this.writeLineComment("Nested Classes");
  }

  protected void writeNull() {
    this.out.print("null");
  }

  protected void writePackage(@NotNull JavaPackage jp) {
    this.out.println();
    this.writePackageHeader(jp);
    this.currentPackage = jp;
    this.writePackageContents(jp);
    this.currentPackage = null;
    this.writePackageFooter(jp);
  }

  protected void writePackageContents(@NotNull JavaPackage jp) {
    this.writeTypeList(jp.types.values(), jp);
  }

  protected void writePackageFooter(@NotNull JavaPackage jp) {
    // default: no footer
  }

  protected void writePackageHeader(@NotNull JavaPackage jp) {
    // default: no header
  }

  protected void writePackageList(@NotNull Collection<JavaPackage> list) {
    if (list.isEmpty()) {
      return;
    }
    this.writePackageListHeader(list);
    this.writePackageListContents(list);
    this.writePackageListFooter(list);
  }

  protected void writePackageListContents(@NotNull Collection<JavaPackage> list) {
    list.forEach(this::writePackage);
  }

  protected void writePackageListFooter(@NotNull Collection<JavaPackage> list) {
    // default: no footer
  }

  protected void writePackageListHeader(@NotNull Collection<JavaPackage> list) {
    // default: no header
  }

  protected void writeType(@NotNull JavaType jt) {
    this.writeTypeHeader(jt);
    this.writeTypeContents(jt);
    this.writeTypeFooter(jt);
  }

  protected void writeTypeContents(@NotNull JavaType jt) {
    jt.classes.values().forEach(this::writeClass);
  }

  protected void writeTypeFooter(@NotNull JavaType jt) {
    // default: no footer
  }

  protected void writeTypeHeader(@NotNull JavaType jt) {
    // default: no header
  }

  protected void writeTypeList(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    if (list.isEmpty()) {
      return;
    }
    this.writeTypeListHeader(list, jp);
    this.writeTypeListContents(list, jp);
    this.writeTypeListFooter(list, jp);
  }

  protected void writeTypeListContents(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    list.forEach(this::writeType);
  }

  protected void writeTypeListFooter(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    // default: no footer
  }

  protected void writeTypeListHeader(@NotNull Collection<JavaType> list, @Nullable JavaPackage jp) {
    // default: no header
  }

  protected void writeTypeName(@NotNull String descriptor) {
    this.writeTypeName(Type.getType(descriptor));
  }

  protected abstract void writeTypeName(@NotNull Type type);

}
