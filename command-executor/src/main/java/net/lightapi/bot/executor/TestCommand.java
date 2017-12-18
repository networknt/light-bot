package net.lightapi.bot.executor;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(TestCommand.class);
    public static final String CONFIG_NAME = "light-bot";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    String workspace = (String)config.get(Constants.WORKSPACE);
    Map<String, Object> test = (Map<String, Object>)config.get(Constants.TEST);
    String userHome = System.getProperty("user.home");

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = 0;

        for(Map.Entry<String, Object> entry : test.entrySet()) {
            String testName = entry.getKey();
            Map<String, Object> testInfo = (Map<String, Object>)entry.getValue();

            // get server entry and start server one by one.
            List<Map<String, Object>> servers = (List<Map<String, Object>>)testInfo.get(Constants.SERVER);
            for(Map<String, Object> server: servers) {
                String path = (String)server.get(Constants.PATH);
                String cmd = (String)server.get(Constants.CMD);
                logger.info("start server at " + path + " with " + cmd);
                Path cmdPath = Paths.get(userHome, workspace, path);

                List<String> commands = new ArrayList<>();
                commands.add("nohup");
                commands.add("bash");
                commands.add("-c");
                String c = cmdPath.toString() + "/" + cmd;
                commands.add("java -jar " + c);
                result = executor.startServer(commands, cmdPath.toFile());
                StringBuilder stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }
            }
            // execute test cases
            logger.info("start testing...");
            final Http2Client client = Http2Client.getInstance();
            final CountDownLatch latch = new CountDownLatch(1);
            final ClientConnection connection;
            try {
                connection = client.connect(new URI("https://localhost:8443"), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            try {
                ClientRequest request = new ClientRequest().setPath("/v2/pet/111").setMethod(Methods.GET);

                connection.sendRequest(request, client.createClientCallback(reference, latch));

                latch.await();
            } catch (Exception e) {
                logger.error("Exception: ", e);
                throw new RuntimeException(e);
            } finally {
                IoUtils.safeClose(connection);
            }
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("statusCode = " + statusCode);
            System.out.println("body = " + body);
            // shutdown servers
            executor.stopServers();
            if(result != 0) {
                break;
            }
        }
        return result;
    }
}
