package com.jodli.coffeeshottimer.data.database

import androidx.room.TypeConverter
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room type converters for handling LocalDate and LocalDateTime objects.
 * These converters allow Room to store and retrieve date/time objects as strings.
 *
 * All dates are stored in ISO format for consistency. The database migration 4→5
 * ensures all existing data is converted to this format.
 */
class Converters {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    @TypeConverter
    fun fromTastePrimary(taste: TastePrimary?): String? {
        return taste?.name
    }

    @TypeConverter
    fun toTastePrimary(tasteString: String?): TastePrimary? {
        return tasteString?.let {
            try {
                TastePrimary.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromTasteSecondary(taste: TasteSecondary?): String? {
        return taste?.name
    }

    @TypeConverter
    fun toTasteSecondary(tasteString: String?): TasteSecondary? {
        return tasteString?.let {
            try {
                TasteSecondary.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
