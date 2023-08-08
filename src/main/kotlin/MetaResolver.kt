class MetaResolver(
    private val nativeOverrides: ModMetaDefinitionFile,
    private val userOverrides: ModMetaDefinitionFile? = null,
) {
    fun resolveKnownModId(id: String): String? {
        return userOverrides?.resolutionOverride?.get(id) ?: nativeOverrides.resolutionOverride[id]
    }
}