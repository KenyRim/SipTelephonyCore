package com.appdev.siptelephonycore

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsHelper {
    fun checkAndRequestCallPermissions(activity: MainActivity): Boolean {
        val PERMISSIONS = arrayOf(
            Manifest.permission.USE_SIP,
            Manifest.permission.RECORD_AUDIO
        )
        val isGranted = ArrayList<String>()

        for(permission in PERMISSIONS){

            if (ContextCompat.checkSelfPermission(activity,permission)
                == PackageManager.PERMISSION_GRANTED){
                isGranted.add(permission)

            }else{
                ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSIONS.size)
            }
        }

        return isGranted.size == 1
    }

}