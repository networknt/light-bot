package com.networknt.bot.core;

public class Constants {
    // develop-build
    public static final String WORKSPACE = "workspace";
    public static final String COMMENT = "comment";
    public static final String CHECKOUT = "checkout";
    public static final String BRANCH = "branch";
    public static final String REPOSITORY = "repository";
    public static final String BUILD = "build";
    public static final String PROJECT = "project";
    public static final String SKIP_TEST = "skip_test";
    public static final String SKIP_BUILD = "skip_build";
    public static final String SKIP = "skip";
    public static final String TEST = "test";
    public static final String SERVER = "server";
    public static final String REQUEST = "request";
    public static final String PATH = "path";
    public static final String CMD = "cmd";
    public static final String TIMEOUT = "timeout";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String METHOD = "method";
    public static final String RESPONSE = "response";
    public static final String STATUS = "status";
    public static final String HEADER = "header";
    public static final String BODY = "body";
    public static final String SKIP_COPYFILE = "skip_copyFile";
    public static final String SKIP_COPYWILDCARDFILE = "skip_copyWildcardFile";
    public static final String COPYFILE = "copyFile";
    public static final String COPYWILDCARDFILE = "copyWildcardFile";
    public static final String SKIP_START = "skip_start";
    public static final String START = "start";
    public static final String TASKS = "tasks";
    public static final String SERVICES = "services";

    // config merge
    public static final String SKIP_MERGECONFIG = "skip_mergeConfig";
    public static final String MERGE_CONFIG = "mergeConfig";
    public static final String FILE = "file";
    public static final String OUTPUT = "output";
    public static final String OUTPUT_FORMAT = "outputFormat";

    // Eclipse project
    public static final String SKIP_GENERATE_ECLIPSE_PROJECT = "skip_generateEclipseProject";
    
    // version-upgrade
    public static final String OLD_VERSION = "old_version";
    public static final String NEW_VERSION = "new_version";
    public static final String MATCH = "match";
    public static final String MAVEN = "maven";
    public static final String VERSION = "version";
    public static final String SKIP_VERSION = "skip_version";

    // release-maven
    public static final String ORGANIZATION = "organization";
    public static final String RELEASE = "release";
    public static final String SKIP_RELEASE = "skip_release";
    public static final String SKIP_RELEASE_NOTE = "skip_release_note";
    public static final String SKIP_CHANGE_LOG = "skip_change_log";
    public static final String SKIP_MERGE = "skip_merge";
    public static final String PREV_TAG = "prev_tag";
    public static final String LAST = "last";

    // release-docker
    public static final String MERGE = "merge";
    public static final String DOCKER = "docker";
    public static final String SKIP_MAVEN = "skip_maven";
    public static final String SKIP_DOCKER = "skip_docker";

    // regex-replace
    public static final String REPLACE = "replace";
    public static final String OLD_VALUE = "old_value";
    public static final String NEW_VALUE = "new_value";
    public static final String SKIP_CHECKOUT = "skip_checkout";
    public static final String SKIP_REPLACE = "skip_replace";
    public static final String SKIP_CHECKIN = "skip_checkin";
    public static final String GLOB = "glob";

    // create-branch
    public static final String FROM_TAG = "from_tag";
    public static final String TAG = "tag";
    public static final String SKIP_BRANCH = "skip_branch";
    public static final String SKIP_PUSH = "skip_push";

    // merge-branch
    public static final String FROM_BRANCH = "from_branch";
    public static final String TO_BRANCH = "to_branch";

    // this result code indicate not repository has been change
    // when pulling from remote. need to skip the build and test
    public static final int NO_REPO_CHANGE = 100;
}
