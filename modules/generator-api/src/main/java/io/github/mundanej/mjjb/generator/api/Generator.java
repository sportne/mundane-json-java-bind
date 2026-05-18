package io.github.mundanej.mjjb.generator.api;

/** Public generator entry point. */
public interface Generator {
  GeneratorResult generate(GeneratorRequest request);
}
