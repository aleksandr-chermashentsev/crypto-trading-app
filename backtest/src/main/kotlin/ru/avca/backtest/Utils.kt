package ru.avca.backtest

import java.time.LocalDate
import java.time.ZoneOffset

/**
 *
 * @author a.chermashentsev
 * Date: 27.05.2021
 **/
class Utils {
    companion object {
        fun timestampOfTheDate(dateStr: String) =
            LocalDate.parse(dateStr).atStartOfDay().toEpochSecond(
                ZoneOffset.of("Z")
            ) * 1000
    }
}