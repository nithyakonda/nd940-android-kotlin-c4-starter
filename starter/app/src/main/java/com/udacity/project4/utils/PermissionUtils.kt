package com.udacity.project4.utils

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel

private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.Q

fun requestBackgroundPermission(laucher: ActivityResultLauncher<String>) {
    if (runningQOrLater) {
        laucher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}

fun requestForegroundPermission(laucher: ActivityResultLauncher<String>) {
    laucher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
}

fun foregroundLocationPermissionApproved(context: Context): Boolean {
    return PackageManager.PERMISSION_GRANTED ==
            ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) &&
            PackageManager.PERMISSION_GRANTED ==
            ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION)
}
@TargetApi(29)
fun backgroundPermissionApproved(context: Context): Boolean  {
    if (runningQOrLater) {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
    } else {
        return true
    }
}