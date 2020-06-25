package org.intellij.plugin.tracker.data

/**
 * Data class for storing user information for web-hosted repository platforms
 * such as GitHub, GitLab etc.
 */
data class UserInfo(
    val username: String,
    val token: String? = null,
    val platform: String
)
