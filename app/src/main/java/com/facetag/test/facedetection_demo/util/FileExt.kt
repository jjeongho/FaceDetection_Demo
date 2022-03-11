package com.facetag.test.facedetection_demo.util

import android.os.Environment
import java.io.File

val rootFolder =
    File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "FaceDetection_Demo${File.separator}"
    ).apply {
        if (!exists())
            mkdirs()
    }

fun makeTempFile(): File = File.createTempFile("${System.currentTimeMillis()}", ".png", rootFolder)