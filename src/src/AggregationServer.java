import handlers.ConnectionHandler;
import handlers.PriorityRunnableFuture;
import handlers.PriorityRunnableFutureComparator;
import handlers.RequestHandler;
import utility.FileMetadata;
import utility.LamportClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

//TODO - Add BackUpRunnable
//TODO - Add HeartBeat/HealthCheck Runnable
public class AggregationServer {
    public ConcurrentMap<String, String> getDatabase() {
        return database;
    }

    private final ConcurrentMap<String, String> database;

    public ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, String>>> getArchive() {
        return archive;
    }

    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String,
            String>>> archive;

    private final LinkedBlockingQueue<FileMetadata> updateQueue; // Queue referencing
    // archive data based on order of update

    private final ExecutorService connectionHandlerPool; // Thread pool to accept
    // incoming requests

    private final ExecutorService requestHandlerPool; // Thread pool to handle request
    private final ScheduledExecutorService schedulePool; // Thread pool to execute
    private final int POOLSIZE = 10;
    // period background tasks
    private final LamportClock clock;
    public boolean isUp;
    private ServerSocket serverSocket;


    public AggregationServer(String[] argv) throws IOException {
        int port = getPort(argv);
        isUp = true;
        database = new ConcurrentHashMap<>();
        archive = new ConcurrentHashMap<>();
        connectionHandlerPool = Executors.newCachedThreadPool();
        schedulePool = Executors.newScheduledThreadPool(POOLSIZE);
        updateQueue = new LinkedBlockingQueue<>();
        requestHandlerPool = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(10, new PriorityRunnableFutureComparator())
        ) {
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                return new PriorityRunnableFuture<>(newTaskFor,
                        ((RequestHandler) callable).getPriority());
            }
        };

        clock = new LamportClock();
        run(port);
    }

    public static int getPort(String[] args) {
        int port;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length == 0) {
            port = 4567;
        } else {
            throw new RuntimeException("Usage: AggregationServer [port].");
        }
        return port;
    }

    public static void main(String[] args) throws IOException {
        AggregationServer server = new AggregationServer(args);
        server.start();
        server.close();
    }

    public void run(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        System.out.println("Server listening at port: " + port);
    }

    public void start() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();

            // Connection Pool listen for incoming requests
            connectionHandlerPool.execute(new ConnectionHandler(
                    clientSocket,
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream())),
                    new PrintWriter(clientSocket.getOutputStream(), true),
                    clock, database, archive, requestHandlerPool, updateQueue,
                    schedulePool));
        }
    }

    public void close() throws IOException {
        serverSocket.close();
        isUp = false;
    }
}


