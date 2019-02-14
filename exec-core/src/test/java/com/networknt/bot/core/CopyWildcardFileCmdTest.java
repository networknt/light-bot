package com.networknt.bot.core;

import com.networknt.bot.core.cmd.CopyWildcardFileCmd;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CopyWildcardFileCmdTest {
    static final Logger logger = LoggerFactory.getLogger(CopyWildcardFileCmdTest.class);
    @Test
    public void testCopy() throws InterruptedException, IOException {
        CopyWildcardFileCmd cmd = new CopyWildcardFileCmd(
                "/home/steve",
                "networknt",
                "light-codegen/codegen-core/target",
                "light-config-test/light-portal/hybrid-query/service",
                //"codegen-core-*.jar");
                //"codegen-core-[0-9]*.[0-9]*.[0-9][0-9].jar");
                "codegen-core-*[!javadoc][!sources].jar");
                cmd.execute();
    }
}
