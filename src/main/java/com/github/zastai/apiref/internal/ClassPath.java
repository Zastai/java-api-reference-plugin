package com.github.zastai.apiref.internal;

import com.github.zastai.apiref.model.JavaApplication;
import com.github.zastai.apiref.model.JavaModule;
import com.github.zastai.apiref.model.JavaPackage;
import com.github.zastai.apiref.model.JavaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** A Java "class path", used to create a {@link JavaApplication} instance. */
public final class ClassPath implements AutoCloseable {

  /** The class files found so far. */
  @NotNull
  private final Map<@NotNull String, Map<@NotNull Integer, @NotNull ClassNode>> classFiles = new HashMap<>();

  /** Paths to classes that were found multiple times. */
  @NotNull
  private final Map<@NotNull String, Map<@NotNull String, @NotNull String>> duplicates = new HashMap<>();

  /** The file systems created for jar files; these need to stay alive until we're done processing all their {@link Path}s. */
  @NotNull
  private final List<@NotNull FileSystem> jarFileSystems = new ArrayList<>();

  @NotNull
  private final Map<@NotNull String, Map<@NotNull Integer, @NotNull ClassNode>> packageInfo = new HashMap<>();

  @NotNull
  private final Map<@NotNull String, Map<@NotNull Integer, @NotNull ClassNode>> moduleInfo = new HashMap<>();

  private boolean verbose = false;

  /**
   * Gathers all class files into modules and packages and constructs a Java application from them.
   *
   * @return A Java application containing all discovered class files, grouped into modules and packages.
   */
  @NotNull
  public JavaApplication buildApplication() {
    for (final var entry : this.duplicates.entrySet()) {
      final var multiRelease = entry.getValue().size() > 1;
      for (final var subEntry : entry.getValue().entrySet()) {
        if (multiRelease) {
          System.out.printf("[info] Found multiple files defining class %s (for %s); will use the one from %s.%n", entry.getKey(),
                            subEntry.getKey(), subEntry.getValue());
        }
        else {
          System.out.printf("[info] Found multiple files defining class %s; will use the one from %s.%n", entry.getKey(),
                            subEntry.getValue());
        }
      }
    }
    this.duplicates.clear();
    SortedMap<String, JavaModule> modules = null;
    if (!this.moduleInfo.isEmpty()) {
      modules = new TreeMap<>();
      for (final var entry : this.moduleInfo.entrySet()) {
        final var name = entry.getKey();
        final var info = entry.getValue().get(0);
        modules.put(name, new JavaModule(name, info));
      }
    }
    if (modules != null) {
      System.out.printf("[info] Found %d module(s).%n", modules.size());
    }
    SortedMap<String, JavaPackage> packages = null;
    Map<String, SortedMap<String, JavaType>> packageTypes = null;
    if (!this.packageInfo.isEmpty()) {
      packages = new TreeMap<>();
      packageTypes = new HashMap<>();
      for (final var entry : this.packageInfo.entrySet()) {
        var name = entry.getKey();
        {
          final int slash = name.lastIndexOf('/');
          if (slash >= 0) {
            name = name.substring(0, slash);
          }
          else {
            name = "";
          }
        }
        final var info = entry.getValue().get(0);
        packages.put(name, new JavaPackage(name, info, packageTypes.computeIfAbsent(name, n -> new TreeMap<>())));
      }
    }
    SortedMap<String, JavaType> topLevelTypes = null;
    if (!this.classFiles.isEmpty()) {
      System.out.printf("[info] Grouping %d class files into packages...%n", this.classFiles.size());
      for (final var entry : this.classFiles.entrySet()) {
        // Find the associated classes, filtering out nested ones.
        Set<ClassNode> classes = null;
        for (final var subEntry : entry.getValue().entrySet()) {
          final var cn = subEntry.getValue();
          // If it's nested in a class or method, we don't want it at this level.
          if (cn.innerClasses != null && !cn.innerClasses.isEmpty()) {
            // It's not always the first entry.
            if (cn.innerClasses.stream().anyMatch(innerClass -> innerClass.name.equals(cn.name))) {
              continue;
            }
          }
          if (classes == null) {
            classes = new HashSet<>();
          }
          classes.add(cn);
        }
        // No non-nested classes -> no type
        if (classes == null) {
          continue;
        }
        final var name = entry.getKey();
        final JavaType jt;
        {
          final var slash = name.lastIndexOf('/');
          if (slash <= 0) {
            if (topLevelTypes == null) {
              topLevelTypes = new TreeMap<>();
            }
            topLevelTypes.put(name, jt = new JavaType(name, null));
          }
          else {
            if (packages == null) {
              packages = new TreeMap<>();
            }
            if (packageTypes == null) {
              packageTypes = new HashMap<>();
            }
            final var packageName = name.substring(0, slash);
            final var types = packageTypes.computeIfAbsent(packageName, n -> new TreeMap<>());
            final var jp = packages.computeIfAbsent(packageName, n -> new JavaPackage(n, null, types));
            types.put(name, jt = new JavaType(name, jp));
          }
        }
        for (final var cn : classes) {
          jt.addClass(cn, this.verbose);
        }
      }
    }
    if (packages != null) {
      System.out.printf("[info] Found %d packages(s).%n", packages.size());
    }
    if (topLevelTypes != null) {
      System.out.printf("[info] Found %d top-level type(s).%n", topLevelTypes.size());
    }
    System.out.println("[info] Resolving nested types...");
    this.resolveNestedTypesInPackages(packages);
    this.resolveNestedTypes(topLevelTypes);
    this.close();
    return new JavaApplication(modules, packages, topLevelTypes);
  }

  /**
   * Looks for class files in a jar file or a folder.
   *
   * @param jarOrFolder A path to a jar file or a folder.
   *
   * @throws IOException When something went wrong while looking through {@code jarOrFolder}.
   */
  public void add(@NotNull Path jarOrFolder) throws IOException {
    if (PathUtil.isDirectory(jarOrFolder)) {
      this.add(jarOrFolder, jarOrFolder);
    }
    else if (PathUtil.isJarFile(jarOrFolder)) {
      final FileSystem jarFileSystem = FileSystems.newFileSystem(jarOrFolder, PathUtil.class.getClassLoader());
      this.add(jarOrFolder, jarFileSystem.getPath("/"));
      this.jarFileSystems.add(jarFileSystem);
    }
  }

  private void add(@NotNull Path context, @NotNull Path path) throws IOException {
    System.out.printf("[info] Looking for class files in %s...%n", context);
    try (final var classes = Files.walk(path).filter(PathUtil::isClassFile)) {
      classes.forEach(classFile -> {
        // Filter out some classes based purely on their location.
        var classPath = path.relativize(classFile);
        final int parts = classPath.getNameCount();
        if (parts > 1 && "META-INF".equals(classPath.getName(0).toString())) {
          // Assumption: nothing under here matters unless it is specifically under a "versions/<integer>" path.
          // FIXME: This logic should only apply when MANIFEST.MF has 'Multi-Release: true'.
          if (!classPath.startsWith("META-INF/versions/") || parts <= 3) {
            return;
          }
        }
        // Any other places we should explicitly avoid class files from? Maybe ensure all path parts are valid Java identifiers?
        this.addClass(classFile, classPath, context);
      });
    }
  }

  private void addClass(@NotNull Map<@NotNull String, Map<@NotNull Integer, @NotNull ClassNode>> list, @NotNull ClassNode contents,
                        boolean ignoreVersion, @NotNull Path path, @NotNull Path context) {
    final var instances = list.computeIfAbsent(contents.name, n -> new HashMap<>());
    final int version = ignoreVersion ? 0 : contents.version;
    if (instances.containsKey(version)) {
      final var duplicateInstances = this.duplicates.computeIfAbsent(contents.name, n -> new HashMap<>());
      duplicateInstances.put(ignoreVersion ? "???" : Util.runtimeVersion(version), "%s (in %s)".formatted(path, context));
    }
    instances.put(version, contents);
  }

  private void addClass(@NotNull Path fullPath, @NotNull Path path, @NotNull Path context) {
    try {
      final var cn = ASMUtil.readClassFile(fullPath);
      final Map<@NotNull String, Map<@NotNull Integer, @NotNull ClassNode>> list;
      final boolean ignoreVersion;
      if ((cn.access & Opcodes.ACC_MODULE) != 0) {
        if (cn.module == null) {
          System.out.printf("[warning] Skipping %s because it is a module which contains no module information.%n", cn.name);
          return;
        }
        if (!cn.name.endsWith("/" + WellKnown.Names.MODULE_INFO)) {
          System.out.printf("[warning] Found module in %s but would have expected that to be called 'module-info'.%n", cn.name);
        }
        // No other reason to exclude a module - we need to document it even if it has no annotations or contents.
        list = this.moduleInfo;
        // For now, we assume there is no need to treat module-info as versioned.
        ignoreVersion = true;
      }
      // FIXME: Is there another way to detect a package-info pseudo-class?
      else if (cn.name.endsWith("/" + WellKnown.Names.PACKAGE_INFO)) {
        // We currently only care about any annotations that may be set on the package. Their (run-time) visibility does not matter.
        // FIXME: Do we need to check both regular and type annotations here?
        if (!ASMUtil.isAnnotated(cn)) {
          if (this.verbose) {
            System.out.printf("[info] Skipping %s because it includes no annotations.%n", ASMUtil.describe(cn));
          }
          return;
        }
        list = this.packageInfo;
        // For now, we assume there is no need to treat package-info as versioned.
        ignoreVersion = true;
      }
      else {
        // We only want public classes.
        if ((cn.access & Constants.ACC_VISIBLE) == 0) {
          if (this.verbose) {
            System.out.printf("[info] Skipping %s because it is not externally visible.%n", ASMUtil.describe(cn));
          }
          return;
        }
        if ((cn.access & Opcodes.ACC_SYNTHETIC) != 0) {
          if (this.verbose) {
            System.out.printf("[info] Skipping %s because it is synthetic.%n", ASMUtil.describe(cn));
          }
          return;
        }
        // Any other reasons to exclude?
        list = this.classFiles;
        ignoreVersion = false;
      }
      if (this.verbose) {
        System.out.printf("[info] Selected %s for inclusion in the public API.%n", ASMUtil.describe(cn));
      }
      this.addClass(list, cn, ignoreVersion, path, context);
    }
    catch (Exception ex) {
      System.err.printf("[error] Could not load class from %s: %s%n", fullPath, ex);
    }
  }

  @Override
  public void close() {
    for (final var fs : this.jarFileSystems) {
      try {
        fs.close();
      }
      catch (Exception ex) {
        System.out.printf("[warning] Could not clean up jar file system: %s%n", ex);
      }
    }
    this.classFiles.clear();
    this.duplicates.clear();
    this.jarFileSystems.clear();
    this.moduleInfo.clear();
    this.packageInfo.clear();
  }

  private void resolveNestedTypes(@NotNull JavaType jt) {
    for (final var jc : jt.classes.values()) {
      final var cn = jc.contents;
      if (cn.innerClasses != null) {
        for (final var innerClass : cn.innerClasses) {
          if (!Objects.equals(innerClass.outerName, jt.fullName)) {
            continue;
          }
          final String nestedName = innerClass.name;
          final var nestedClassFiles = this.classFiles.get(nestedName);
          if (nestedClassFiles == null) {
            System.out.printf("[warning] Skipping nested type %s (assumed to be neither public nor protected).%n", nestedName);
            continue;
          }
          final var nested = jt.addNestedType(nestedName);
          nestedClassFiles.values().forEach(ncf -> nested.addClass(ncf, this.verbose));
          this.resolveNestedTypes(nested);
        }
      }
    }
  }

  private void resolveNestedTypes(@Nullable SortedMap<String, JavaType> types) {
    if (types == null) {
      return;
    }
    types.values().forEach(this::resolveNestedTypes);
  }

  private void resolveNestedTypesInPackages(@Nullable SortedMap<String, JavaPackage> packages) {
    if (packages == null) {
      return;
    }
    for (final var jp : packages.values()) {
      this.resolveNestedTypes(jp.types);
    }
  }

  /**
   * Enables or disables verbose output.
   *
   * @param yes Indicates whether verbose output should be enabled.
   */
  public void setVerbose(boolean yes) {
    this.verbose = yes;
  }

}
