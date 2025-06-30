package com.mycompany.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAST {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("[GenerateAST] Usage: generate_ast <output path>");
      System.exit(1);
    }
    String output = args[0];
    try {
      defineAST(
          output,
          "Expr",
          Arrays.asList(
              "Binary   : Expr left, Token operator, Expr right",
              "Grouping : Expr expression",
              "Literal  : Object value",
              "Unary    : Token operator, Expr right"));

    } catch (IOException ex) {
      System.err.println("[Generate AST] Error In Saving File");
    }
  }

  private static void defineAST(String outputDirectory, String basename, List<String> types)
      throws IOException {
    var path = outputDirectory + "/" + basename + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.mycompany.app;");
    writer.println("import java.util.List;");
    writer.println("abstract class " + basename + "{");

    for (String type : types) {
      String classname = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, basename, classname, fields);
    }

    writer.println("}");
  }

  private static void defineType(
      PrintWriter writer, String baseName, String className, String fieldList) {
    writer.println("static class " + className + " extends " + baseName + " {");

    // constructor
    writer.println("" + className + "(" + fieldList + ") {");

    String[] fields = fieldList.split(",");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("this." + name + " = " + name + ";");
    }

    writer.println("}");

    // Fields.
    writer.println();
    for (String field : fields) {
      writer.println("final " + field + ";");
    }

    writer.println("}");
    writer.println("}");
    writer.close();
  }
}
