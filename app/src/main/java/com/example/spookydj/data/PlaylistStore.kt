package com.example.spookydj.data

import android.content.Context
import com.example.spookydj.model.Track
import org.json.JSONArray

object PlaylistStore {
    private const val PREFS = "spookydj_playlists"
    private const val NAMES_KEY = "playlist_names"

    fun getPlaylistNames(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val namesCsv = prefs.getString(NAMES_KEY, "") ?: ""
        return namesCsv.split("|").filter { it.isNotBlank() }.sorted()
    }

    fun savePlaylist(context: Context, name: String, tracks: List<Track>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val jsonArr = JSONArray()
        tracks.forEach { jsonArr.put(it.toJson()) }
        val key = keyFor(name)
        val current = getPlaylistNames(context).toMutableSet()
        current.add(name)
        prefs.edit()
            .putString(key, jsonArr.toString())
            .putString(NAMES_KEY, current.joinToString("|"))
            .apply()
    }

    fun loadPlaylist(context: Context, name: String): List<Track> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(keyFor(name), null) ?: return emptyList()
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                add(Track.fromJson(arr.getJSONObject(i)))
            }
        }
    }

    fun deletePlaylist(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = keyFor(name)
        val current = getPlaylistNames(context).toMutableSet()
        current.remove(name)
        prefs.edit()
            .remove(key)
            .putString(NAMES_KEY, current.joinToString("|"))
            .apply()
    }

    private fun keyFor(name: String) = "playlist_$name"
}
