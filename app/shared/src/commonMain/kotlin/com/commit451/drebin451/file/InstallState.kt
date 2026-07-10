package com.commit451.drebin451.file

/**
 * Snapshot of the (single) in-flight install. [versionId] identifies which version's button should
 * show progress; [message] carries a user-facing reason when [phase] is [InstallPhase.Error].
 */
data class InstallState(
    val phase: InstallPhase = InstallPhase.Idle,
    val versionId: String = "",
    val message: String = "",
)
