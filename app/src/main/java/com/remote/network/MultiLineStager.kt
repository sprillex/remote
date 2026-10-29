package com.remote.network

class MultiLineStager {

    private val tokenRegex = Regex("\\{\\{([a-zA-Z0-9_]+)\\}\\}")

    fun extractTokens(template: String): Set<String> {
        val matches = tokenRegex.findAll(template)
        return matches.map { it.groupValues[1] }.toSet()
    }

    fun getUnboundTokens(template: String, bindings: Map<String, String>): Set<String> {
        val required = extractTokens(template)
        return required.filter { !bindings.containsKey(it) || bindings[it].isNullOrBlank() }.toSet()
    }

    fun substituteTokens(template: String, bindings: Map<String, String>): String {
        return tokenRegex.replace(template) { matchResult ->
            val tokenName = matchResult.groupValues[1]
            bindings[tokenName] ?: matchResult.value
        }
    }

    fun parseMultiLineCommands(script: String): List<String> {
        return script.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
    }
}
