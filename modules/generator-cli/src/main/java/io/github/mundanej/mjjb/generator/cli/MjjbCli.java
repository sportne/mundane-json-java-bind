package io.github.mundanej.mjjb.generator.cli;

import io.github.mundanej.mjjb.generator.api.GeneratorDiagnostic;
import io.github.mundanej.mjjb.generator.api.GeneratorProfile;
import io.github.mundanej.mjjb.generator.api.GeneratorRequest;
import io.github.mundanej.mjjb.generator.api.GeneratorResult;
import io.github.mundanej.mjjb.generator.core.CoreGenerator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/** Command-line entry point for JSON Schema to Java binding generation. */
public final class MjjbCli {
  private MjjbCli() {}

  public static void main(String[] args) {
    int exitCode = run(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  public static int run(String[] args) {
    CliArguments parsed = CliArguments.parse(args);
    if (parsed.help) {
      System.out.println(usage());
      return 0;
    }
    if (!parsed.diagnostics.isEmpty()) {
      parsed.diagnostics.forEach(System.err::println);
      return 2;
    }
    GeneratorResult result =
        new CoreGenerator()
            .generate(
                new GeneratorRequest(
                    parsed.schemas,
                    parsed.outputDirectory,
                    parsed.profile,
                    parsed.defaultPackage,
                    Map.of()));
    if (!result.successful()) {
      result.diagnostics().stream()
          .map(GeneratorDiagnostic::toManifestLine)
          .forEach(System.err::println);
      return 1;
    }
    result.generatedSources().forEach(path -> System.out.println("Generated " + path));
    return 0;
  }

  private static String usage() {
    return String.join(
        System.lineSeparator(),
        "Usage: mjjb generate --schema <schema.json> --out <dir> [--package <package>] [--profile JSP-DATA-2020-12]",
        "",
        "Generates explicit Java bindings from supported JSON Schema Draft 2020-12 schemas.");
  }

  private static final class CliArguments {
    private final ArrayList<String> diagnostics = new ArrayList<>();
    private final ArrayList<Path> schemas = new ArrayList<>();
    private Path outputDirectory = Path.of("build/generated/sources/mjjb");
    private GeneratorProfile profile = GeneratorProfile.JSP_DATA_2020_12;
    private String defaultPackage = GeneratorRequest.DEFAULT_PACKAGE;
    private boolean help;

    private static CliArguments parse(String[] args) {
      CliArguments parsed = new CliArguments();
      if (args.length == 0) {
        parsed.help = true;
        return parsed;
      }
      int index = 0;
      if ("generate".equals(args[0])) {
        index = 1;
      }
      while (index < args.length) {
        String arg = args[index++];
        switch (arg) {
          case "--help", "-h" -> parsed.help = true;
          case "--schema" -> parsed.schemas.add(Path.of(requiredValue(args, index++, arg, parsed)));
          case "--out" ->
              parsed.outputDirectory = Path.of(requiredValue(args, index++, arg, parsed));
          case "--package" -> parsed.defaultPackage = requiredValue(args, index++, arg, parsed);
          case "--profile" ->
              parsed.profile = parseProfile(requiredValue(args, index++, arg, parsed), parsed);
          default -> parsed.diagnostics.add("MJJB-CLI-001 | Unsupported argument " + arg + ".");
        }
      }
      if (parsed.schemas.isEmpty() && !parsed.help) {
        parsed.diagnostics.add("MJJB-CLI-002 | At least one --schema argument is required.");
      }
      return parsed;
    }

    private static String requiredValue(
        String[] args, int index, String argument, CliArguments parsed) {
      if (index >= args.length) {
        parsed.diagnostics.add("MJJB-CLI-003 | Missing value for " + argument + ".");
        return "";
      }
      return args[index];
    }

    private static GeneratorProfile parseProfile(String token, CliArguments parsed) {
      return GeneratorProfile.fromCliToken(token)
          .orElseGet(
              () -> {
                parsed.diagnostics.add("MJJB-CLI-004 | Unsupported profile " + token + ".");
                return GeneratorProfile.JSP_DATA_2020_12;
              });
    }
  }
}
