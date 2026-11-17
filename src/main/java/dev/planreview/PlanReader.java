package dev.planreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local input only. Resolve symlinks before enforcing the configured workspace boundary. */
public final class PlanReader {
  public static final int MAX_BYTES = 20 * 1024 * 1024;
  public static final ObjectMapper JSON =
      new ObjectMapper()
          .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
  private final Path root;

  public PlanReader(Path root) throws IOException {
    this.root = root.toRealPath();
  }

  public JsonNode read(String name) throws IOException {
    Path requested = Path.of(name);
    Path file = (requested.isAbsolute() ? requested : root.resolve(requested)).toRealPath();
    if (!file.startsWith(root) || !Files.isRegularFile(file))
      throw new IllegalArgumentException("Plan must be a regular file inside the configured root");
    try (InputStream input = Files.newInputStream(file)) {
      byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Plan exceeds 20 MiB");
      try (var parser = JSON.getFactory().createParser(bytes)) {
        JsonNode plan = JSON.readTree(parser);
        if (parser.nextToken() != null) throw new IllegalArgumentException("Trailing JSON content");
        return plan;
      }
    }
  }
}
