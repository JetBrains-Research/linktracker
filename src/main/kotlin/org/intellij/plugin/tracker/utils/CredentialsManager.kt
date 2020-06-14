package org.intellij.plugin.tracker.utils

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

/**
 * This class handles the logic of storing web-hosted repository platforms' (e.g. GitHub, Gitlab) users' credentials
 *
 * It handles the logic of both storing and retrieving the credentials, upon request.
 */
class CredentialsManager {

    companion object {

        private fun createCredentialAttributes(key: String): CredentialAttributes {
            return CredentialAttributes(generateServiceName("Link tracker", key))
        }

        /**
         * Stores the credentials (token) of a user identified by a username and a specific platform
         */
        fun storeCredentials(platform: String, username: String, token: String) {
            val credentialAttributes: CredentialAttributes = createCredentialAttributes("$platform-$username")
            val passwordSafe: PasswordSafe = PasswordSafe.instance
            val credentials = Credentials(username, token)
            passwordSafe.set(credentialAttributes, credentials)
        }

        /**
         * Gets the credentials (token), if any, of a user identified by a username and platform
         */
        fun getCredentials(platform: String, username: String): String? {
            val credentialAttributes: CredentialAttributes = createCredentialAttributes("$platform-$username")
            val credentials: Credentials? = PasswordSafe.instance.get(credentialAttributes)
            if (credentials != null) {
                return credentials.getPasswordAsString()
            }
            return null
        }
    }
}
