// GoalService/src/main/java/Business/Messaging.java
package Business;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.event.EventReceive;
import io.kubemq.sdk.event.Subscriber;
import io.kubemq.sdk.subscription.EventsStoreType;
import io.kubemq.sdk.subscription.SubscribeRequest;
import io.kubemq.sdk.subscription.SubscribeType;
import io.kubemq.sdk.tools.Converter;
import io.grpc.stub.StreamObserver;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;

public class Messaging {

    private static final Logger LOGGER = Logger.getLogger(Messaging.class.getName());

    public static void Receiving_Events_Store(String channelName) throws SSLException, ServerAddressNotSuppliedException {
        String clientID = "goal-service-subscriber";
        String kubeMQAddress = System.getenv("kubeMQAddress");

         // Handle cases where the env variable might not be set (for local testing before upload to google cloud)
        if (kubeMQAddress == null || kubeMQAddress.isEmpty()) {
            System.out.println("Warning: kubeMQAddress environment variable not set. Using default localhost:50000");
            kubeMQAddress = "localhost:50000";
        }
         System.out.println("Subscribing to KubeMQ at: " + kubeMQAddress + " on channel: " + channelName);

        Subscriber subscriber = new Subscriber(kubeMQAddress);
        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setChannel(channelName);
        subscribeRequest.setClientID(clientID);
        subscribeRequest.setSubscribeType(SubscribeType.EventsStore); // Use EventsStore to get past messages too
        subscribeRequest.setEventsStoreType(EventsStoreType.StartFromFirst); // Start from the beginning

        StreamObserver<EventReceive> streamObserver = new StreamObserver<EventReceive>() {

            @Override
            public void onNext(EventReceive eventReceive) {
                try {
                    String messageBody = (String) Converter.FromByteArray(eventReceive.getBody());
                    LOGGER.log(Level.INFO, "Event Received: ID:{0}, Channel:{1}, Body:{2}",
                            new Object[]{eventReceive.getEventId(), eventReceive.getChannel(), messageBody});

                    // Parse the message: CREATED:{USER_ID}:{USERNAME}:{EMAIL}
                    String[] parts = messageBody.split(":");
                    if (parts.length == 4 && "CREATED".equals(parts[0])) {
                        try {
                            int userId = Integer.parseInt(parts[1]);
                            String username = parts[2];
                            String email = parts[3];

                            LOGGER.log(Level.INFO, "Processing CREATED event for UserID: {0}, Username: {1}", new Object[]{userId, username});

                            // Call persistence layer to save to the cache table
                            Persistence.GoalPersistence.saveUserInfoCache(userId, username, email);

                            LOGGER.log(Level.INFO, "User info cached successfully for UserID: {0}", userId);

                        } catch (NumberFormatException e) {
                            LOGGER.log(Level.WARNING, "Could not parse UserID from message: {0}", messageBody);
                        } catch (SQLException e) {
                            LOGGER.log(Level.SEVERE, "Database error caching user info: {0}", e.getMessage());
                            // Implement retry logic or dead-letter queue
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Unexpected error processing message: {0}", e.getMessage());
                        }
                    } else {
                         LOGGER.log(Level.WARNING, "Received message with unexpected format: {0}", messageBody);
                    }

                } catch (Exception e) {
                     LOGGER.log(Level.SEVERE, "Error processing incoming event: {0}", e.getMessage());
                     e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                 LOGGER.log(Level.SEVERE, "onError received: {0}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                LOGGER.log(Level.INFO, "onCompleted received");
            }

        };

        subscriber.SubscribeToEvents(subscribeRequest, streamObserver);
         System.out.println("Subscription started for channel: " + channelName);
    }
}