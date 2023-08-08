import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModMetaDefinitionFile(
    @SerialName("resolution-override")
    val resolutionOverride: Map<String, String>,
)