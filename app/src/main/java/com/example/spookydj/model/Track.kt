package com.example.spookydj.model

import android.net.Uri
import org.json.JSONObject

data class Track(
    val uri: Uri,
    val displayName: String,
    var loop: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("uri", uri.toString())
        put("displayName", displayName)
        put("loop", loop)
    }

    companion object {
        fun fromJson(obj: JSONObject): Track = Track(
            uri = Uri.parse(obj.getString("uri")),
            displayName = obj.getString("displayName"),
            loop = obj.optBoolean("loop", false)
        )
    }
}
