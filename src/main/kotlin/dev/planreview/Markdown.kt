package dev.planreview

/** Escapes plan-controlled text before generating a Markdown report. */
object Markdown {
    private fun escape(value: String) = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\\", "\\\\").replace("`", "\\`").replace("*", "\\*")
        .replace("_", "\\_").replace("[", "\\[").replace("]", "\\]")
        .replace("#", "\\#").replace("|", "\\|").replace("\r", " ").replace("\n", " ")

    @JvmStatic
    fun render(r: Review): String = buildString {
        appendLine("# Terraform change review").appendLine()
        append("${r.summary.changedResources} changed resources; ${r.summary.changedOutputs} changed outputs; ")
            .append("${r.summary.high} high and ${r.summary.medium} medium findings.").appendLine().appendLine()
        appendLine("Partial analysis — absence of findings is not approval to apply.").appendLine()
        appendLine("## Findings").appendLine()
        for (f in r.findings) {
            appendLine("### ${f.severity} · ${f.ruleId} · ${f.title}").appendLine()
            appendLine("Resource: ${escape(f.resource)}").appendLine()
            appendLine(f.impact).appendLine()
            for (e in f.evidence) appendLine("- ${escape(e.path)}: ${escape(e.before.toString())} → ${escape(e.after.toString())}")
            appendLine().appendLine("Next check: ${f.nextCheck}").appendLine()
        }
        appendLine("## Change inventory").appendLine()
        for (c in r.changes) appendLine("- ${escape(c.address)} — ${c.actions}; coverage: ${c.coverage}; changed paths: ${escape(c.changedPaths.toString())}; unknown paths: ${escape(c.unknownPaths.toString())}")
        appendLine().appendLine("## Check coverage").appendLine()
        appendLine("Status | Rule | Resource | Reason").appendLine("--- | --- | --- | ---")
        for (check in r.checks) appendLine("${check.status} | ${check.ruleId} | ${escape(check.resource)} | ${check.reasonCode}")
        appendLine().appendLine("Plan completeness: ${r.summary.complete} (null = not reported).")
        appendLine().appendLine("## Limits").appendLine()
        r.limitations.forEach { appendLine("- $it") }
    }
}
