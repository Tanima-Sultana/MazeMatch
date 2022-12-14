package com.example.imageapplication.float

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

private var toast: Toast? = null

fun Context.showToast(message: CharSequence?) {
    message?.let {
        toast?.cancel()
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT).apply { show() }
    }
}

val Context.canDrawOverlays: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)



