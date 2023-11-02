package kr.co.telecons.mconhudsdk.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal object MConHudPermissionUtil {
    internal val inSensitivePermissions: Array<String> = setPermissionArrayListToArray()
    internal val sensitivePermissions: Array<String> = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun setPermissionArrayListToArray(): Array<String> {
        val permissionsArrayList = setPermissionArrayList()
        var permissionsArray: Array<String> = arrayOf()
        permissionsArrayList.forEach {
            it.forEach { permissions ->
                permissionsArray = permissionsArray.plus(permissions)
            }
        }
        return permissionsArray
    }

    private fun setPermissionArrayList(): ArrayList<Array<String>> {
        val permissions = ArrayList<Array<String>>()

        // default permissions
        permissions.add(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        permissions.add(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
        permissions.add(arrayOf(Manifest.permission.BLUETOOTH))
        permissions.add(arrayOf(Manifest.permission.BLUETOOTH_ADMIN))

        // Android 12(S) changed permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(arrayOf(Manifest.permission.BLUETOOTH_SCAN))
            permissions.add(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            permissions.add(arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE))
            permissions.removeIf { item -> item.contentEquals(arrayOf(Manifest.permission.BLUETOOTH_ADMIN)) }
            permissions.removeIf { item -> item.contentEquals(arrayOf(Manifest.permission.BLUETOOTH)) }
        }

        return permissions
    }

    /**
     * checkPermissions(@param, @param)
     *
     * @param context: Context,
     * @param permissions: Array<String>
     *
     * @return Boolean
     */
    internal fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach {
            if(ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Context.isInsensitivePermissionGranted()
     *
     * Returns whether to allow non-sensitive permissions
     *
     * @return Boolean
     */
    internal fun Context.isInsensitivePermissionsGranted(): Boolean {
        return checkPermissions(this, inSensitivePermissions)
    }

    /**
     * Context.isSensitivePermissionGranted()
     *
     * Returns whether sensitive permissions are allowed
     *
     * @return Boolean
     */
    internal fun Context.isSensitivePermissionGranted(): Boolean {
        return checkPermissions(this, sensitivePermissions)
    }
}