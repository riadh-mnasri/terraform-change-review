package dev.planreview;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class CliTest {
  @Test
  void thresholdsAndFormatsWork() throws Exception {
    var original = System.out;
    var bytes = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(bytes));
      assertEquals(1, Main.run(new String[] {"review", "examples/gcp-risky.json"}));
      assertEquals(
          0,
          Main.run(
              new String[] {
                "review", "examples/gcp-risky.json", "--fail-on", "none", "--format", "markdown"
              }));
      assertEquals(0, Main.run(new String[] {"review", "examples/gcp-label-update.json"}));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              Main.run(
                  new String[] {"review", "examples/gcp-risky.json", "--fail-on", "critical"}));
      assertFalse(bytes.toString().contains("DEMO_SECRET_MUST_NEVER_APPEAR"));
    } finally {
      System.setOut(original);
    }
  }
}
