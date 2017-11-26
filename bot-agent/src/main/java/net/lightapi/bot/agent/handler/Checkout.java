
package net.lightapi.bot.agent.handler;

import com.networknt.config.Config;
import com.networknt.utility.NioUtils;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import net.lightapi.bot.agent.AgentConfig;
import net.lightapi.bot.agent.Constants;
import net.lightapi.bot.agent.Utility;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@ServiceHandler(id="lightapi.net/agent/checkout/0.1.0")
public class Checkout implements Handler {
    static final Logger logger = LoggerFactory.getLogger(Checkout.class);
    AgentConfig config = (AgentConfig)Config.getInstance().getJsonObjectConfig(Constants.CONFIG_NAME, AgentConfig.class);

    @Override
    public ByteBuffer handle(Object input)  {
        Map<String, Object> map = (Map<String, Object>)input;
        String repository = (String)map.get("repository");
        String branch = (String)map.get("branch");
        if(branch == null) branch = "develop";
        Boolean cascade = (Boolean)map.get("cascade");
        logger.debug("reposiotry = " + repository + " branch = " + branch + " cascade = " + cascade);
        String repoName = repository.substring(repository.lastIndexOf('/') + 1, repository.length() - 4);
        File repoFile = Paths.get(System.getProperty("user.home"), config.getWorkspace(),  repoName).toFile();
        if (repoFile.exists()) {
            logger.debug("workspace is available");
            try (Git git = Git.init().setDirectory(repoFile).call()) {
                git.checkout().setName(branch).call();
                git.pull().call();
            } catch (GitAPIException e) {
                logger.error("GitAPIExcepiton", e);
            }
        } else {
            logger.debug("repo is on in workspace, clone it.");
            try (Git git = Git.cloneRepository()
                    .setURI(repository)
                    .setDirectory(repoFile)
                    .setBranchesToClone(Arrays.asList("master", "develop"))
                    .setBranch(branch)
                    .call()) {
            } catch (GitAPIException e) {
                logger.error("GitAPIExcepiton", e);
            }
        }
        return NioUtils.toByteBuffer("");
    }
}
