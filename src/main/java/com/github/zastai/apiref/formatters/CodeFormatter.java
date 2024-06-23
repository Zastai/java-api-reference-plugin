package com.github.zastai.apiref.formatters;

import com.github.zastai.apiref.model.JavaApplication;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

/** A class for formatting a Java application's (public) API. */
public abstract class CodeFormatter {

  /**
   * Creates a new code formatter.
   *
   * @param out The stream to write the public API to.
   */
  protected CodeFormatter(@NotNull PrintStream out) {
    this.out = out;
  }

  @NotNull
  protected final PrintStream out;

  /**
   * Formats the public API for a Java application and writes it to the specified stream.
   *
   * @param application The application whose public API should be formatted.
   */
  protected void writePublicApi(@NotNull JavaApplication application) {
    this.out.println("// TODO");
  }

}
