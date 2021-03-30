package ru.avca;

import io.micronaut.core.exceptions.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author a.chermashentsev
 * Date: 30.03.2021
 **/
public class GlobalExceptionHandler implements ExceptionHandler<Throwable> {
    Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Override
    public void handle(Throwable exception) {
        log.error("Uncaught exception ", exception);
    }
}
