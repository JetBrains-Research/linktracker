package org.intellij.plugin.tracker.data


data class UserInfo(
    val username: String,
    val token: String? = null,
    val platform: String
)