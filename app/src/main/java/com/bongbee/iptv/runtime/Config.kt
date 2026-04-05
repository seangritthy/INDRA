package com.bongbee.iptv.runtime

import java.io.File
import android.content.Context

object Config {
    // Directory names
    const val DEBIAN_DIR_NAME = "debian"
    const val ROOTFS_DIR_NAME = "rootfs"
    const val BIN_DIR_NAME = "bin"
    const val ASSET_BIN_DIR = "bin"

    fun getDebianDir(context: Context) = File(context.filesDir, DEBIAN_DIR_NAME)
    fun getRootfsDir(context: Context) = File(getDebianDir(context), ROOTFS_DIR_NAME)
    fun getBinDir(context: Context) = File(context.filesDir, BIN_DIR_NAME)
    fun getProotBinary(context: Context) = File(getBinDir(context), "proot")
}
