package com.example.weanimals.entry.auth

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.ActionCodeSettings

object PasswordResetActionSettings {

    fun forContext(context: Context, isOrganization: Boolean): ActionCodeSettings {
        val accountType = if (isOrganization) "organization" else "citizen"
        val continueUrl = Uri.parse(ACTION_HOST_URL)
            .buildUpon()
            .appendQueryParameter("account", accountType)
            .build()
            .toString()

        return ActionCodeSettings.newBuilder()
            .setUrl(continueUrl)
            .setAndroidPackageName(context.packageName, false, null)
            .setHandleCodeInApp(false)
            .build()
    }

    private const val ACTION_HOST_URL =
        "https://weanimals-642a9.web.app/password-reset-complete"
}
