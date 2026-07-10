package com.commit451.drebin451.ui

import com.commit451.drebin451.model.App

data class AppShareState(
    val app: App,
    val refreshingShareId: Boolean = false,
    val message: String? = null,
)
