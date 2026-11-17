package dev.planreview;

/** Escape plan-controlled text before generating a Markdown report. */
public final class Markdown {
  private static String escape(String s) {
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("*", "\\*")
        .replace("_", "\\_")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("#", "\\#")
        .replace("|", "\\|")
        .replace("\r", " ")
        .replace("\n", " ");
  }

  public static String render(Review r) {
    var b = new StringBuilder("# Terraform change review\n\n");
    b.append(r.summary().changedResources())
        .append(" changed resources; ")
        .append(r.summary().changedOutputs())
        .append(" changed outputs; ")
        .append(r.summary().high())
        .append(" high and ")
        .append(r.summary().medium())
        .append(" medium findings.\n\n");
    b.append("Partial analysis — absence of findings is not approval to apply.\n\n## Findings\n\n");
    for (var f : r.findings()) {
      b.append("### ")
          .append(f.severity())
          .append(" · ")
          .append(f.ruleId())
          .append(" · ")
          .append(f.title())
          .append("\n\n")
          .append("Resource: ")
          .append(escape(f.resource()))
          .append("\n\n")
          .append(f.impact())
          .append("\n\n");
      for (var e : f.evidence())
        b.append("- ")
            .append(escape(e.path()))
            .append(": ")
            .append(escape(e.before().toString()))
            .append(" → ")
            .append(escape(e.after().toString()))
            .append("\n");
      b.append("\nNext check: ").append(f.nextCheck()).append("\n\n");
    }
    b.append("## Change inventory\n\n");
    for (var c : r.changes())
      b.append("- ")
          .append(escape(c.address()))
          .append(" — ")
          .append(c.actions())
          .append("; coverage: ")
          .append(c.coverage())
          .append("; changed paths: ")
          .append(escape(c.changedPaths().toString()))
          .append("; unknown paths: ")
          .append(escape(c.unknownPaths().toString()))
          .append("\n");
    b.append("\n## Check coverage\n\n");
    b.append("Status | Rule | Resource | Reason\n--- | --- | --- | ---\n");
    for (var check : r.checks())
      b.append(check.status())
          .append(" | ")
          .append(check.ruleId())
          .append(" | ")
          .append(escape(check.resource()))
          .append(" | ")
          .append(check.reasonCode())
          .append("\n");
    b.append("\nPlan completeness: ")
        .append(r.summary().complete())
        .append(" (null = not reported).\n");
    b.append("\n## Limits\n\n");
    r.limitations().forEach(l -> b.append("- ").append(l).append("\n"));
    return b.toString();
  }
}
