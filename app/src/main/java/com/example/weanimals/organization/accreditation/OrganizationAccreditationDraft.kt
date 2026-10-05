package com.example.weanimals.organization.accreditation

import android.content.Context

/**
 * Stores only the non-sensitive accreditation draft while the institutional
 * e-mail is waiting for verification. Passwords and document contents are
 * intentionally never persisted.
 */
object OrganizationAccreditationDraft {

    private const val PREFERENCES_NAME = "organization_accreditation_draft"
    private const val KEY_PENDING = "pending"
    private const val KEY_CNPJ = "cnpj"
    private const val KEY_LEGAL_NAME = "legal_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_DOCUMENT_NAME = "document_name"
    private const val KEY_DOCUMENT_SIZE = "document_size"
    private const val KEY_DOCUMENT_CONTENT_TYPE = "document_content_type"
    private const val KEY_PIX = "pix"

    data class Values(
        val cnpj: String,
        val legalName: String,
        val email: String,
        val documentName: String,
        val documentSizeBytes: Long,
        val documentContentType: String,
        val pixKey: String
    )

    fun save(context: Context, values: Values) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING, true)
            .putString(KEY_CNPJ, values.cnpj)
            .putString(KEY_LEGAL_NAME, values.legalName)
            .putString(KEY_EMAIL, values.email)
            .putString(KEY_DOCUMENT_NAME, values.documentName)
            .putLong(KEY_DOCUMENT_SIZE, values.documentSizeBytes)
            .putString(KEY_DOCUMENT_CONTENT_TYPE, values.documentContentType)
            .putString(KEY_PIX, values.pixKey)
            .apply()
    }

    fun hasPending(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PENDING, false)

    fun load(context: Context): Values? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_PENDING, false)) return null

        return Values(
            cnpj = preferences.getString(KEY_CNPJ, "").orEmpty(),
            legalName = preferences.getString(KEY_LEGAL_NAME, "").orEmpty(),
            email = preferences.getString(KEY_EMAIL, "").orEmpty(),
            documentName = preferences.getString(KEY_DOCUMENT_NAME, "").orEmpty(),
            documentSizeBytes = preferences.getLong(KEY_DOCUMENT_SIZE, 0L),
            documentContentType = preferences.getString(KEY_DOCUMENT_CONTENT_TYPE, "").orEmpty(),
            pixKey = preferences.getString(KEY_PIX, "").orEmpty()
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
