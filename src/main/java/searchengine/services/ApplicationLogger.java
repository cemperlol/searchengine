package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface ApplicationLogger {
    Logger logger = LogManager.getRootLogger();

    static void log(Exception e) {
        e.printStackTrace();
        logger.error("Cause: " + e.getCause() + "\t" +
                "Message: " + e.getMessage());
    }
}
