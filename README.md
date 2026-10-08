# light-bot
A microservice based devops server and agent that handle multiple repositories and dependencies
from multiple organizations or even multiple git servers.

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-4j) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com/tool/light-bot/) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

### Why this tool

##### Multiple Repo First

All existing DevOps tools on the market are focusing on single repo but in a microservices architecture, there are a lot of related services, and libraries need to be built and tested at the same time if one upstream repository is changed. For example, in the networknt organization, if light-4j is changed, we need to build and test other dozens of repositories that are depending on light-4j. If you build light-4j only, chances are light-eventuate-4j is broken due to the change introduced in light-4j although all unit test cases passed in light-4j.

When we adopt microservices architecture, a traditional monolithic application will be split into dozens of smaller services and each service will have its own repository. Sometime, these repositories will be scattered in multiple organizations or multiple git servers. When one of the services changed, we need to build/test it, and we need to build other upstream and downstream services as well to do the integration tests to ensure all of them are working together. It is very hard to define this kind of dependencies and building multiple repositories in today's DevOps tools as they are all single repository focused.

##### Fully Automatic Pipeline with Code

Most of the existing tools are trying to provide a fancy UI, but it is very hard to build a pipeline from one git merge to production with multiple repositories involved. The cycle git pull-->build--> unit tests-->integration tests-->scanning-->image creation should/will be repeated at every commit, automatically with all the dependencies and contract tested. With only configurations and plugins, it is very hard to build a complicated multiple repositories pipeline. Writing your own plugins can help, but you lose the flexibility of the code and control. Basically, we are trying to provide a framework that you can compose your pipeline with dependency injection. What we are trying to do is DevOps as the code.

##### Linxu and Git only

Our target is the cloud-native environment, and we don't need to worry about Windows support and other version control system like DevOps tools. This will save us 80 percent of the time and resource to deliver a light-weight and optimized solution for our target user base. If you look at the most popular DevOps tool Jenkins, it has built an OS abstract layer to support multiple operating systems and an SCM abstract layer to support all type of version control systems like CVS and SVN, etc. This makes the code very hard to reason about, and at the same time, you lose the opportunity to optimize it.

##### Cache Local Repositories

Most DevOps tools will check out/clone from Git for the entire project for every build. The idea comes from old source control software. In Git, a simple git pull will only sync the changed files from the server and start the build.

For example, if we change one repository in the networknt organization, light-bot takes seconds to pull from the repository and build over 20 other repositories in the workspace immediately. Jenkins will clone all repositories to its workspace from scratch, and it takes about 10 minutes. This might repeat dozens or hundreds of times depending on how big is your team.

##### Shared Dependencies Repository

Dependencies don't need to be downloaded for every build, and they need to be shared by different build tasks on the same host to reduce the network traffic and speed up the build process. In Jenkins, we had a hard time to share the .m2 local repository to support multiple related builds as it treats every build as independent and builds each one in a separated environment. This generates too much network traffic and slows down the build process dramatically.

##### Shared Infrastructure Services

Modern applications need a lot of infrastructure services to run/test — for example, database, Kafka, etc. The build system needs to support a shared environment without impact other build tasks running at the same time. It needs to be Docker friendly and have support isolation on a shared environment without impact other builds running in parallel.

##### Support Environment Segregation

You can have multiple bots running at different servers or even on the same server with different environment tag in the configuration to be responsible for different environment build. The test cycle in a secure zone against real back-ends needs to be segregated, via config, and includes also an environment specific build (say include a config server), then test against that back-end.

##### Easy to Plugin

The implementation of task executor can be easily replaced with the externalized jar file and configuration change. And the different team can wire in only the plugins they need. As it is an open source framework, it is very easy to test your own plugin and see the interactions during the build. You can also customize the framework for your own needs.

##### Build across Multiple Organizations

In microservices architecture, different teams might have their own organizations and some of the dependencies are across multiple organizations. The DevOps tool needs to know how to check out and build repositories from many organizations. Also, these related services might reside in different git providers or git servers. To manage the access control in this complex git environment in DevOps tool is a daunting job.

##### Idempotency

The testing aspect of the build, whether it is against multiple environments or not, should indeed use shared infrastructure (say Kafka across DIT/SIT) but also segregated back-ends. In all cases, testing using multiple bots needs to be idempotent. Idempotent testing is an aspect with most customers are struggling right now, especially in an evolving eco-system (ex, you don't have to create and delete APIs available maybe yet you still wish to test adding a product to a customer. this needs to be addressed via other means)

## Build and run

light-bot uses Maven for all modules and CLI packaging. Install JDK 25 and
Maven 3.6.3 or newer, then run these commands from the repository root:

```sh
mvn clean verify
```

This builds all modules, runs their tests, and creates the executable CLI JAR
at `bot-cli/target/bot-cli.jar`. To install the module artifacts in your local
Maven repository, run `mvn clean install`.

The CLI runtime also requires Java 25 or newer. Maven Enforcer checks the
build requirements before compilation. Source and Javadoc JARs are generated
only by the existing `release-sign-artifacts` profile.

Prepare a complete external task configuration directory, including
`service.yml` and the configuration for each registered task. Bundled files
are examples, not a runnable default: release tasks require additional keys
such as `skip_change_log` and `skip_gradle`. Register only the tasks you intend
to use. Run from your configuration workspace with:

```sh
java -Dlight-4j-config-dir=/absolute/path/to/config \
  -jar /absolute/path/to/light-bot/bot-cli/target/bot-cli.jar --task <task-name>
```

## Release changelogs

Maven release, snapshot, and Docker release changelogs are generated from git
commit history by default. No source-selection setting is required. Configure
the previous release tag and new version in the release configuration:

```yaml
prev_tag: 2.3.7
version: 2.3.8
```

Generation uses each repository's `prev_tag..HEAD` ancestry range and emits
commit subjects with GitHub SHA links, including PR commits and direct commits.
It needs local git history and the previous tag, but no `CHANGELOG_GITHUB_TOKEN`
or GitHub API calls. The previous tag must be
present locally and be an ancestor of HEAD; generation fails before writing
the changelog otherwise. Fetch the required history and tags before running
the release task if the checkout is shallow or missing tags.

Merge commits, `light-bot checkin CHANGELOG.md`, `bot checkin`, and Maven release
plugin preparation commits are omitted. Other commits are included regardless
of their author. Entries credit the git author and retain issue/PR references.
User mentions and Markdown formatting in subjects and author names are neutralized. Generation creates a missing `CHANGELOG.md`, preserves older
release sections, and replaces the current version's section on reruns.

Git errors retain their diagnostics and stop the task. The CLI returns a nonzero
exit status and uses the configured failure notification for task failures.
GitHub release publication passes JSON directly to curl without a shell.

## Maven release preparation

Both `ReleaseMavenTask` and `SnapshotMavenTask` support optional local
preparation commands in `release-maven.yml`. They run after
checkout and before changelog generation, checkin, and Maven publication. A
nonzero exit stops the task before those later stages. Existing configurations
without `prepare` retain their previous behavior.

```yaml
skip_prepare: false
prepare:
  - light-4j:
      - mvn clean install -pl status,monad-result,config,client-config,cluster -am
release:
  - networknt/http-client
  - networknt/light-4j
prev_tags:
  networknt/http-client: 1.0.18
```

Each preparation entry maps a repository directory in the configured workspace
to commands run there, in list order. Use one repository per entry when order
matters. The foundational build supplies the framework artifacts needed to
compile `http-client` without selecting framework modules that depend on it.
Preparation runs even with `skip_release: true`; use `skip_prepare: true` to
skip an already completed build on a retry. The configured commands should use
local build goals, without `deploy` or the release-signing profile.

`prev_tags` overrides `prev_tag` for individual `organization/repository`
entries. Repositories without an override use the shared `prev_tag`. This lets
a repository join the release cycle from a different previous tag.

Prepare and review release POM versions before running this task: `version`
labels changelogs and GitHub releases and does not rewrite Maven versions.
For a coordinated version, align the `http-client` project version and its
`version.light-4j`, the framework version and its `version.http-client`, and
downstream consumers. Use release versions rather than snapshots. The local
installation makes the ordered builds possible, but Central publication of
separate repositories is not atomic: the new client may be visible before its
framework dependencies. No staging or publication-wait behavior is added by
the preparation phase.
