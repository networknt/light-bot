
package net.lightapi.bot.agent.handler;

import com.networknt.utility.NioUtils;
import com.networknt.rpc.Handler;
import com.networknt.rpc.router.ServiceHandler;
import java.nio.ByteBuffer;

@ServiceHandler(id="lightapi.net/agent/build/0.1.0")
public class Build implements Handler {
    @Override
    public ByteBuffer handle(Object input)  {
        return NioUtils.toByteBuffer("");
    }
}
