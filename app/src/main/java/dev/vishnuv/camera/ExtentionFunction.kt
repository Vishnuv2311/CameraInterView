package dev.vishnuv.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun Activity.shareImage(uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_TEXT, "Vishnu")
        putExtra(Intent.EXTRA_STREAM, uri)
    }

fun Activity.showToast(msg: String) {
    Toast.makeText(
        baseContext,
        msg,
        Toast.LENGTH_SHORT
    ).show()
}