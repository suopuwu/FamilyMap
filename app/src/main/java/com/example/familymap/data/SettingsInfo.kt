package com.example.familymap.data

import java.util.*

object SettingsInfo {
    enum class Option {
        LIFE_STORY_LINES,
        FAMILY_TREE_LINES,
        SPOUSE_LINES,
        FILTER_FATHER,
        FILTER_MOTHER,
        FILTER_MALE,
        FILTER_FEMALE
    }

    //we use an enum map for high performance while still being able to edit settings by reference
    private var values: EnumMap<Option, Boolean> = EnumMap(Option::class.java)

    //get setting if it exists, defaults to enabled if it doesn't
    public fun getSetting(setting: Option): Boolean {
        if (!values.containsKey(setting)) {
            values[setting] = true
        }
        return values[setting]!!
    }

    public fun setSetting(setting: Option, value: Boolean) {
        values[setting] = value
    }
}