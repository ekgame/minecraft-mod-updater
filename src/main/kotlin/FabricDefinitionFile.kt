import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

@Serializable
data class FabricDefinitionFile(
    val id: String,
    val name: String,
    @Serializable(with = AuthorSerializer::class)
    val authors: List<String>,
    val version: String,
    val depends: Map<String, String>,
)

class AuthorSerializer : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) {
            error("Expected array, got $element")
        }
        val result = mutableListOf<JsonPrimitive>()
        element.forEach {
            when (it) {
                is JsonObject -> {
                    val name = it["name"] ?: error("Author object is missing a name field")
                    if (name !is JsonPrimitive) error("Author name is not a string")
                    if (!name.isString) error("Author name is not a string")
                    result.add(name)
                }
                is JsonPrimitive -> {
                    if (!it.isString) error("Author name is not a string")
                    result.add(it)
                }
                else -> error("Author is not a string or object")
            }
        }
        return JsonArray(result)
    }
}