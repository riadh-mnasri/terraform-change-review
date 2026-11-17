package dev.planreview

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/** Reads local plan files and confines MCP paths to the configured root. */
class PlanReader(root: Path) {
    private val root: Path = root.toRealPath()

    fun read(name: String): JsonNode {
        val requested = Path.of(name)
        val file = (if (requested.isAbsolute) requested else root.resolve(requested)).toRealPath()
        require(file.startsWith(root) && Files.isRegularFile(file)) {
            "Plan must be a regular file inside the configured root"
        }
        Files.newInputStream(file).use { input ->
            val bytes = input.readNBytes(MAX_BYTES + 1)
            require(bytes.size <= MAX_BYTES) { "Plan exceeds 20 MiB" }
            JSON.factory.createParser(bytes).use { parser ->
                val plan = JSON.readTree<JsonNode>(parser)
                require(parser.nextToken() == null) { "Trailing JSON content" }
                return plan
            }
        }
    }

    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        @JvmField
        val JSON: ObjectMapper = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    }
}
