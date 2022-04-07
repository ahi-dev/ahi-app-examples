package com.example.ahi_kotlin_multiscan_boilerplate.utilities

import com.myfiziq.sdk.vo.SdkResultParcelable
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/** Save 3D avatar mesh result on local device. */
fun saveObjResultToFile(res: SdkResultParcelable, objFile: File) {
    val meshResObj = JSONObject(res.result)
    val objString = meshResObj["mesh"].toString()
    val words: List<String> = objString.split(",")
    val stream = FileOutputStream(objFile)
    val writer = BufferedWriter(OutputStreamWriter(stream))
    for (word in words) {
        writer.write(word)
        writer.newLine()
    }
    writer.close()
}