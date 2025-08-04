package ru.raspberry.launcher.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.raspberry.launcher.models.repo.Argument
import ru.raspberry.launcher.models.repo.Rule

object ArgumentSerializer : KSerializer<Argument> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Argument") {
        element<String>("value")
        element<List<Rule>>("rules", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): Argument {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Only Json supported")

        val element = input.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> Argument(value = element.content)
            is JsonObject -> {
                val valueElement = element["value"]
                val value = when (valueElement) {
                    is JsonPrimitive -> valueElement.content
                    is JsonArray -> valueElement.joinToString(" ") { it.jsonPrimitive.content }
                    else -> throw SerializationException("Unsupported 'value' format: $valueElement")
                }
                val rules = element["rules"]?.let {
                    input.json.decodeFromJsonElement(ListSerializer(Rule.serializer()), it)
                } ?: emptyList()
                Argument(value, rules)
            }
            else -> throw SerializationException("Unsupported element format: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: Argument) {
        val output = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder")
        val json = buildJsonObject {
            put("value", JsonPrimitive(value.value))
            if (value.rules.isNotEmpty()) {
                put("rules", output.json.encodeToJsonElement(ListSerializer(Rule.serializer()), value.rules))
            }
        }
        output.encodeJsonElement(json)
    }
}