package org.intellij.plugin.tracker.utils

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

class CredentialsManager {

    companion object {

        private fun createCredentialAttributes(key: String): CredentialAttributes {
            return CredentialAttributes(generateServiceName("Link tracker", key))
        }

        fun storeCredentials(username: String, password: String) {
            val credentialAttributes: CredentialAttributes =  createCredentialAttributes(username)
            val passwordSafe: PasswordSafe = PasswordSafe.instance
            val credentials = Credentials(username, password)
            passwordSafe.set(credentialAttributes, credentials)
        }

        fun getCredentials(username: String): String? {
            val credentialAttributes: CredentialAttributes = createCredentialAttributes(username)
            val credentials: Credentials? = PasswordSafe.instance.get(credentialAttributes)
            if (credentials != null) {
                return credentials.getPasswordAsString()
            }
            return null
        }

    }

}