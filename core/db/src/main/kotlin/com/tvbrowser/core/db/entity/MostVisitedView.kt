package com.tvbrowser.core.db.entity

import androidx.room.DatabaseView

@DatabaseView(
    viewName = "most_visited",
    value = """SELECT url,
                      MAX(title) AS title,
                      COUNT(*) AS visitCount,
                      MAX(visitedAt) AS lastVisited
               FROM history
               GROUP BY url
               ORDER BY visitCount DESC, lastVisited DESC
               LIMIT 20"""
)
data class MostVisitedView(
    val url: String,
    val title: String?,
    val visitCount: Int,
    val lastVisited: Long,
)
