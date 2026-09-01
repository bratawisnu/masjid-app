package com.masjid.display.data.local

import androidx.room.TypeConverter
import com.masjid.display.data.local.entity.AreaPosition
import com.masjid.display.data.local.entity.CalculationMethod
import com.masjid.display.data.local.entity.MadhabMethod
import com.masjid.display.data.local.entity.MainContentType
import com.masjid.display.data.local.entity.SlideType

class Converters {

    @TypeConverter
    fun fromCalculationMethod(value: CalculationMethod): String = value.name

    @TypeConverter
    fun toCalculationMethod(value: String): CalculationMethod = CalculationMethod.valueOf(value)

    @TypeConverter
    fun fromMadhabMethod(value: MadhabMethod): String = value.name

    @TypeConverter
    fun toMadhabMethod(value: String): MadhabMethod = MadhabMethod.valueOf(value)

    @TypeConverter
    fun fromAreaPosition(value: AreaPosition): String = value.name

    @TypeConverter
    fun toAreaPosition(value: String): AreaPosition = AreaPosition.valueOf(value)

    @TypeConverter
    fun fromMainContentType(value: MainContentType): String = value.name

    @TypeConverter
    fun toMainContentType(value: String): MainContentType = MainContentType.valueOf(value)

    @TypeConverter
    fun fromSlideType(value: SlideType): String = value.name

    @TypeConverter
    fun toSlideType(value: String): SlideType = SlideType.valueOf(value)
}
