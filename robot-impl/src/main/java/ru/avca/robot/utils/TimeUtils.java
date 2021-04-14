package ru.avca.robot.utils;

import java.time.ZonedDateTime;

/**
 * @author a.chermashentsev
 * Date: 08.04.2021
 **/
public class TimeUtils {
    public static long getCurrentTimeUtc() {
        return ZonedDateTime.now().toInstant().toEpochMilli();
    }
}
