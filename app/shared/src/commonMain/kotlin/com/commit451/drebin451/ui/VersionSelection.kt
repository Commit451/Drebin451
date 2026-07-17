package com.commit451.drebin451.ui

/** Selection state for Gmail-style contextual actions on uploaded builds. */
internal data class VersionSelection(
    val versionIds: Set<String> = emptySet(),
) {
    val active: Boolean
        get() = versionIds.isNotEmpty()

    fun select(versionId: String): VersionSelection =
        copy(versionIds = versionIds + versionId)

    fun toggle(versionId: String): VersionSelection =
        copy(
            versionIds = if (versionId in versionIds) {
                versionIds - versionId
            } else {
                versionIds + versionId
            },
        )

    fun selectAll(availableVersionIds: Set<String>): VersionSelection =
        copy(versionIds = availableVersionIds)

    fun retainAvailable(availableVersionIds: Set<String>): VersionSelection =
        copy(versionIds = versionIds intersect availableVersionIds)

    fun clear(): VersionSelection = VersionSelection()
}
