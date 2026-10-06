package com.example.util

import org.json.JSONArray
import org.json.JSONObject

object ReactionUtils {
    val POPULAR_CHAT_EMOJIS = listOf("👍", "❤️", "🔥", "😂", "🎉", "🚀", "👏", "💡")

    /**
     * Parses a JSON string formatted as { "👍": ["Rahul", "Priya"], "❤️": ["Arjun"] }
     * into a Kotlin Map<String, List<String>>.
     */
    fun parseReactions(json: String?): Map<String, List<String>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, List<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val emoji = keys.next()
                val array = obj.optJSONArray(emoji)
                val users = mutableListOf<String>()
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val name = array.optString(i)
                        if (!name.isNullOrBlank()) {
                            users.add(name)
                        }
                    }
                }
                if (users.isNotEmpty()) {
                    result[emoji] = users
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Formats a Kotlin Map<String, List<String>> into a clean JSON string.
     */
    fun formatReactions(reactions: Map<String, List<String>>): String {
        if (reactions.isEmpty()) return ""
        val obj = JSONObject()
        reactions.forEach { (emoji, users) ->
            if (users.isNotEmpty()) {
                val array = JSONArray()
                users.distinct().forEach { array.put(it) }
                obj.put(emoji, array)
            }
        }
        return obj.toString()
    }

    /**
     * Toggles an emoji for a user:
     * - If the user already reacted with this emoji, removes their name.
     * - If the user hasn't reacted with this emoji, adds their name.
     * Returns Pair<updatedJsonString, isAdded>.
     */
    fun toggleUserReaction(
        currentJson: String?,
        emoji: String,
        userName: String
    ): Pair<String, Boolean> {
        val trimmedUser = userName.trim()
        val reactions = parseReactions(currentJson).toMutableMap()
        val currentUsersForEmoji = reactions[emoji]?.toMutableList() ?: mutableListOf()

        val alreadyReacted = currentUsersForEmoji.any { it.equals(trimmedUser, ignoreCase = true) }
        val isAdded: Boolean

        if (alreadyReacted) {
            currentUsersForEmoji.removeAll { it.equals(trimmedUser, ignoreCase = true) }
            if (currentUsersForEmoji.isEmpty()) {
                reactions.remove(emoji)
            } else {
                reactions[emoji] = currentUsersForEmoji
            }
            isAdded = false
        } else {
            currentUsersForEmoji.add(trimmedUser)
            reactions[emoji] = currentUsersForEmoji
            isAdded = true
        }

        return Pair(formatReactions(reactions), isAdded)
    }
}
