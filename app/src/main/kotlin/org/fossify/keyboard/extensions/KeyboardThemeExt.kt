package org.fossify.keyboard.extensions

import android.content.Context
import android.graphics.Color
import org.fossify.commons.extensions.darkenColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.isDynamicTheme
import org.fossify.commons.extensions.isSystemInDarkMode
import org.fossify.commons.extensions.lightenColor
import org.fossify.keyboard.R

fun Context.getKeyboardBackgroundColor(): Int {
    return Color.parseColor("#111622")
}

fun Context.getStrokeColor(): Int {
    return if (isDynamicTheme()) {
        if (isSystemInDarkMode()) {
            resources.getColor(R.color.md_grey_800, theme)
        } else {
            resources.getColor(R.color.md_grey_400, theme)
        }
    } else {
        val lighterColor = safeStorageContext.getProperBackgroundColor().lightenColor()
        if (lighterColor == Color.WHITE || lighterColor == Color.BLACK) {
            resources.getColor(R.color.divider_grey, theme)
        } else {
            lighterColor
        }
    }
}
