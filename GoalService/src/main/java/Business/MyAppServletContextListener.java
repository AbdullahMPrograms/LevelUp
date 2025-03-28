package Business;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener 
public class MyAppServletContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(MyAppServletContextListener.class.getName());
    private Thread subscriberThread;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("ServletContextListener initialized - Starting KubeMQ subscriber thread...");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    // Channel name MUST match the publisher
                    Messaging.Receiving_Events_Store("user_creation_channel");
                } catch (SSLException ex) {
                    LOGGER.log(Level.SEVERE, "KubeMQ SSLException in listener thread", ex);
                } catch (ServerAddressNotSuppliedException ex) {
                    LOGGER.log(Level.SEVERE, "KubeMQ ServerAddressNotSuppliedException in listener thread", ex);
                } catch (Exception ex) {
                     LOGGER.log(Level.SEVERE, "Unexpected exception in KubeMQ listener thread", ex);
                }
            }
        };

        subscriberThread = new Thread(r);
        subscriberThread.setName("KubeMQGoalSubscriberThread");
        subscriberThread.start();
         LOGGER.info("KubeMQ subscriber thread started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
         LOGGER.info("ServletContextListener destroyed - Interrupting KubeMQ subscriber thread...");
         if (subscriberThread != null && subscriberThread.isAlive()) {
             subscriberThread.interrupt(); // Attempt to interrupt the thread
             LOGGER.info("KubeMQ subscriber thread interrupted.");
         } else {
             LOGGER.info("KubeMQ subscriber thread was not running or already finished.");
         }
    }
}