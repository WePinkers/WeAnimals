package com.example.weanimals.core.session

import android.content.Context

object SessionAccountType {

    enum class Type { CITIZEN, ORGANIZATION }

    private const val PREFERENCES_NAME = "active_session"
    private const val KEY_ACCOUNT_TYPE = "account_type"
    private const val VALUE_CITIZEN = "citizen"
    private const val VALUE_ORGANIZATION = "organization"

    fun rememberCitizen(context: Context) {
        save(context, VALUE_CITIZEN)
    }

    fun rememberOrganization(context: Context) {
        save(context, VALUE_ORGANIZATION)
    }

    fun get(context: Context): Type? = when (
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT_TYPE, null)
    ) {
        VALUE_CITIZEN -> Type.CITIZEN
        VALUE_ORGANIZATION -> Type.ORGANIZATION
        else -> null
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCOUNT_TYPE)
            .apply()
    }

    private fun save(context: Context, value: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNT_TYPE, value)
            .apply()
    }
}
