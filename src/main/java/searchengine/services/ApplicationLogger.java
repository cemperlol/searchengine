package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationLogger {
    public static Logger logger = LogManager.getRootLogger();

    public static void log(Exception e) {
        e.printStackTrace();
        logger.error("Cause: " + e.getCause() + "\t" +
                "Message: " + e.getMessage());
    }
}
