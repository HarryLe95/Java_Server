import utility.LamportClock;
import utility.http.HTTPRequest;
import utility.http.HTTPResponse;
import utility.json.Parser;
import utility.json.WeatherData;


import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestHandler implements Callable<HTTPResponse> {
    private final HTTPRequest request;
    private final int priority;

    private final Parser parser;
    private final String remoteIP;
    private final ConcurrentMap<String, String> database;

    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, String>>> archive;

    public RequestHandler(
            HTTPRequest request,
            String remoteIP,
            LamportClock clock,
            ConcurrentMap<String, String> database,
            ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, String>>> archive
    ) {
        this.request = request;
        priority = clock.printTimestamp();
        this.database = database;
        this.archive = archive;
        this.remoteIP = remoteIP;
        parser = new Parser();
    }

    private HTTPResponse handleGET() {
        // Empty GET request
        if (request.uri.equals("/"))
            return new HTTPResponse("1.1")
                    .setStatusCode("204")
                    .setReasonPhrase("No Content")
                    .setHeader("Content-Type", "application/json")
                    .setBody("");
        // Station ID provided
        String stationID = request.uri.substring(1);
        // Station ID data is available
        if (database.containsKey(stationID))
            return new HTTPResponse("1.1")
                    .setStatusCode("200")
                    .setReasonPhrase("OK")
                    .setHeader("Content-Type", "application/json")
                    .setBody(database.get(stationID));
        // Station ID data unavailable
        return new HTTPResponse("1.1")
                .setStatusCode("404")
                .setReasonPhrase("Not Found")
                .setHeader("Content-Type", "application/json")
                .setBody("");
    }

    private HTTPResponse handlePUT() {
        HTTPResponse response;
        String fileName = request.uri.substring(1);
        String body = request.body;
        parser.parseMessage(body);
        Map<String, WeatherData> container = parser.getContainer();
        // Response for newly connected host
        if (!archive.containsKey(remoteIP))
            response = new HTTPResponse("1.1")
                    .setStatusCode("201")
                    .setReasonPhrase("Created")
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Content-Length", request.header.get("Content-Length"))
                    .setBody(body);
        else
            response = new HTTPResponse("1.1")
                    .setStatusCode("200")
                    .setReasonPhrase("OK")
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Content-Length", request.header.get("Content-Length"))
                    .setBody(body);
        // Update archive
        ConcurrentMap<String, ConcurrentMap<String, String>>
                remoteEntry = archive.getOrDefault(remoteIP, new ConcurrentHashMap<>());
        ConcurrentMap<String, String> entry = new ConcurrentHashMap<>();
        entry.put("Value", body);
        entry.put("Fresh", "true");
        remoteEntry.put(fileName, entry);
        archive.put(remoteIP, remoteEntry);

        // Update database
        for (Map.Entry<String, WeatherData> weatherEntry : container.entrySet()) {
            String weatherData = weatherEntry.getValue().toString();
            database.put(weatherEntry.getKey(), weatherData);
        }

        // Return response
        return response;
    }


    @Override
    public HTTPResponse call() throws Exception {
        HTTPResponse response;
        if (request.method.equals("GET"))
            response = handleGET();
        else if (request.method.equals("PUT"))
            response = handlePUT();
        else
            response = new HTTPResponse("1.1").setStatusCode("400").setReasonPhrase("Bad Request");
        return response;
    }

    public int getPriority() {
        return priority;
    }
}
