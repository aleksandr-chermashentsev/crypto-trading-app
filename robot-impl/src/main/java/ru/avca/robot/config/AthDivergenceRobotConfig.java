package ru.avca.robot.config;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * @author a.chermashentsev
 * Date: 07.10.2021
 **/
@ConfigurationProperties("robotState")
public class AthDivergenceRobotConfig {
    private String robotName;
    private String usdCoin;
    private int defaultUsdBalance;

    public String getRobotName() {
        return robotName;
    }

    public void setRobotName(String robotName) {
        this.robotName = robotName;
    }

    public String getUsdCoin() {
        return usdCoin;
    }

    public void setUsdCoin(String usdCoin) {
        this.usdCoin = usdCoin;
    }

    public int getDefaultUsdBalance() {
        return defaultUsdBalance;
    }

    public void setDefaultUsdBalance(int defaultUsdBalance) {
        this.defaultUsdBalance = defaultUsdBalance;
    }
}
