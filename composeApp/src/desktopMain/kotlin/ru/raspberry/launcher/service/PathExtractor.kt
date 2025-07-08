package ru.raspberry.launcher.service

import kotlinx.serialization.json.*


fun pathsFromJson(json: String): Map<String, String> {
    val node = Json.parseToJsonElement(json)
    val dict = mutableMapOf<String, String>()
    fromJsonNode("root", node, dict)
    return dict
}

private fun fromJsonNode(path: String, node: JsonElement, dict: MutableMap<String, String>) {
    when (node)
    {
        is JsonArray -> {
            val array = node.jsonArray
            for (i in 0 .. array.size)
                fromJsonNode("$path.$i", array[i], dict)
        }
        is JsonObject -> {
            node.jsonObject.forEach { key, value ->
                fromJsonNode("$path.$key", value, dict)
            }
        }
        is JsonPrimitive -> dict.put(path, node.jsonPrimitive.content)
        JsonNull -> {}
    }
}