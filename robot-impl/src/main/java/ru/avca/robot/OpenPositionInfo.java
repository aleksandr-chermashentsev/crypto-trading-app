package ru.avca.robot;

import lombok.Value;

import java.math.BigDecimal;

/**
 * @author a.chermashentsev
 * Date: 03.05.2021
 **/
@Value
public class OpenPositionInfo {
    String symbol;
    BigDecimal balance;
    BigDecimal price;
}
