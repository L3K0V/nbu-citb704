package me.lekov.nbu.citb704

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

fun fetchJson(url: String): JSONObject? {
    val Url = URL(url)
    val connection: HttpURLConnection = Url.openConnection() as HttpURLConnection
    val inputStream: InputStream = connection.inputStream
    val br = BufferedReader(InputStreamReader(inputStream))
    val sb = StringBuilder()
    var line: String?
    while (br.readLine().also { line = it } != null) {
        sb.append(line)
    }
    line = sb.toString()
    connection.disconnect()
    inputStream.close()
    sb.delete(0, sb.length)
    return JSONObject(line)
}

