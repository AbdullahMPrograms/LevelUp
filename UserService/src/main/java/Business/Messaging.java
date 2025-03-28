package Business;

import io.kubemq.sdk.basic.ServerAddressNotSuppliedException;
import io.kubemq.sdk.event.Event;
import io.kubemq.sdk.tools.Converter;
import javax.net.ssl.SSLException;
import java.io.IOException;

public class Messaging {

    public static void sendMessage(String message) throws IOException {
        String channelName = "user_creation_channel";
        String clientID = "user-service-publisher";
        String kubeMQAddress = System.getenv("kubeMQAddress");

         // Handle cases where the env variable might not be set (for local testing before upload to google cloud)
         if (kubeMQAddress == null || kubeMQAddress.isEmpty()) {
            System.out.println("Warning: kubeMQAddress environment variable not set. Using default localhost:50000");
            kubeMQAddress = "localhost:50000"; 
        }
        System.out.println("Sending message to KubeMQ at: " + kubeMQAddress + " on channel: " + channelName);

        io.kubemq.sdk.event.Channel channel = null;
        try {
            channel = new io.kubemq.sdk.event.Channel(channelName, clientID, false, kubeMQAddress);
            channel.setStore(true); 
            Event event = new Event();
            event.setBody(Converter.ToByteArray(message));
            event.setEventId("event-store-" + clientID);

            channel.SendEvent(event);
            System.out.println("Message sent successfully: " + message);

        } catch (SSLException e) {
            System.out.printf("SSLException: %s%n", e.getMessage());
            e.printStackTrace();
            throw new IOException("SSL Error sending message", e);
        } catch (ServerAddressNotSuppliedException e) {
            System.out.printf("ServerAddressNotSuppliedException: %s%n", e.getMessage());
            e.printStackTrace();
            throw new IOException("KubeMQ server address error", e);
        } catch (Exception e) {
            System.out.printf("General Exception sending message: %s%n", e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to send message", e);
        }
    }
}