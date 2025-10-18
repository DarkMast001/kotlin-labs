package org.example

import org.example.Exceptions.CycleDetectedException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

// Поддерживаемые типы: String, Int, Double, Boolean, List, data class

class TomlSerializer {

    companion object {
        fun <T : Any> serialize(
            obj: T,
            visited: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap()),
            from: String? = null
        ): String {
            if (!visited.add(obj)) {
                throw CycleDetectedException("Cycle has been detected")
            }

            val kClass = obj::class
            val simpleParts = mutableListOf<String>()
            val nestedParts = mutableListOf<String>()

            for (prop in kClass.declaredMemberProperties) {
                prop.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val value = (prop as KProperty1<Any, *>).get(obj)

                val formattedValue = when {
                    value == null -> "null"
                    value is String -> "\"$value\""
                    value is Number -> value.toString()
                    value is Boolean -> value.toString()
                    value is Collection<*> -> {
                        val items = value.map { item ->
                            when {
                                item == null -> "null"
                                item is String -> "\"$item\""
                                item is Number || item is Boolean -> item.toString()
                                item::class.isData -> serialize(item, visited)
                                else -> "\"${item}\""
                            }
                        }
                        "[${items.joinToString(", ")}]"
                    }
                    value::class.isData -> {
                        serialize(value, visited, if (from != null) "$from.${prop.name}" else prop.name)
                    }
                    else -> "\"$value\""
                }

                if (value != null && value::class.isData) {
                    val sectionName = if (from != null) "$from.${prop.name}" else prop.name
                    nestedParts.add("\n[$sectionName]\n$formattedValue")
                } else {
                    simpleParts.add("${prop.name} = $formattedValue")
                }
            }

            visited.remove(obj)

            return (simpleParts + nestedParts).joinToString("\n")
        }

        inline fun <reified T : Any> deserialize(toml: String): T {
            val lines = toml.lines()
            val flatMap = parseFlatMap(lines)
            val nestedMap = flattenToNested(flatMap)
            return instantiate(T::class, nestedMap)
        }

        fun parseFlatMap(lines: List<String>): Map<String, Any?> {
            val result = mutableMapOf<String, Any?>()
            var currentSection = ""

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.substring(1, trimmed.length - 1)
                    continue
                }

                if (trimmed.contains("=")) {
                    val eqIndex = trimmed.indexOf('=')
                    val key = trimmed.substring(0, eqIndex).trim()
                    val valueStr = trimmed.substring(eqIndex + 1).trim()
                    val fullKey = if (currentSection.isEmpty()) key else "$currentSection.$key"
                    result[fullKey] = parseValue(valueStr)
                }
            }

            return result
        }

        private fun parseValue(valueStr: String): Any? {
            return when {
                valueStr == "null" -> null
                valueStr.startsWith("\"") && valueStr.endsWith("\"") ->
                    valueStr.substring(1, valueStr.length - 1)
                valueStr == "true" -> true
                valueStr == "false" -> false
                valueStr.startsWith("[") && valueStr.endsWith("]") -> {
                    if (valueStr == "[]") {
                        emptyList<String>()
                    } else {
                        valueStr
                            .substring(1, valueStr.length - 1)
                            .split(",")
                            .map { it.trim().removeSurrounding("\"") }
                    }
                }
                valueStr.contains(".") -> valueStr.toDouble()
                else -> valueStr.toIntOrNull() ?: valueStr
            }
        }

        fun flattenToNested(flat: Map<String, Any?>): Map<String, Any?> {
            val result = mutableMapOf<String, Any?>()

            for ((key, value) in flat) {
                insertIntoNested(result, key.split(".").toTypedArray(), value)
            }

            return result
        }

        private fun insertIntoNested(map: MutableMap<String, Any?>, path: Array<String>, value: Any?) {
            if (path.isEmpty()) return

            if (path.size == 1) {
                map[path[0]] = value
                return
            }

            val currentKey = path[0]
            val remainingPath = path.drop(1).toTypedArray()

            var nested = map[currentKey]
            if (nested == null) {
                nested = mutableMapOf<String, Any?>()
                map[currentKey] = nested
            }

            if (nested is MutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                insertIntoNested(nested as MutableMap<String, Any?>, remainingPath, value)
            }
        }

        fun <T : Any> instantiate(kClass: KClass<T>, values: Map<String, Any?>): T {
            val constructor = kClass.primaryConstructor
                ?: throw IllegalArgumentException("No primary constructor")

            val args = mutableMapOf<KParameter, Any?>()
            for (param in constructor.parameters) {
                val paramName = param.name ?: continue
                var value = values[paramName]

                if (value is Map<*, *> && param.type.classifier is KClass<*>) {
                    @Suppress("UNCHECKED_CAST")
                    value = instantiate(param.type.classifier as KClass<Any>, value as Map<String, Any?>)
                }

                value = coerceToType(value, param.type)
                args[param] = value
            }

            return constructor.callBy(args)
        }

        private fun coerceToType(value: Any?, kType: KType): Any? {
            if (value == null) return null

            return when (kType.classifier) {
                String::class -> value.toString()
                Int::class -> (value as? Number)?.toInt() ?: value.toString().toInt()
                Double::class -> (value as? Number)?.toDouble() ?: value.toString().toDouble()
                Boolean::class -> value as? Boolean ?: value.toString().toBoolean()
                else -> value
            }
        }
    }
}