package searchengine.logging;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ApplicationLogger {
    Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);

    static void logError(Exception e) {
        e.printStackTrace();
        logger.error("Cause: " + e.getCause() + "\t" +
                "Message: " + e.getMessage());
    }
}
