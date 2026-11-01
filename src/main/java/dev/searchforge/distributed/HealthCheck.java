package dev.searchforge.distributed;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
public final class HealthCheck {
    public static void main(String[] args) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create(args[0] + "/health")).timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString("{}" )).build();
            if (client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() != 200) System.exit(1);
        }
    }
}
