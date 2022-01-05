package me.lekov.nbu.citb704

import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

fun fetchJson(url: String): JSONObject? {
    val Url = URL(url)

    try {
        val connection: HttpURLConnection = Url.openConnection() as HttpURLConnection
        val inputStream: InputStream = connection.inputStream
        val sb = StringBuilder()

        inputStream.use {
            val br = BufferedReader(InputStreamReader(inputStream))

            br.useLines {
                it.iterator().forEachRemaining {
                    sb.append(it)
                }
            }
        }

        connection.disconnect()
        return JSONObject(sb.toString())
    } catch(e: Exception) {}

    return null
}

