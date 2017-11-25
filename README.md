# light-bot
A microservice based devops agent that handles multiple repositories and dependencies

### Why this tool

##### Multiple Repo First

All existing devops tools on the market are focusing on single repo but in a microservices
architecture, there are a lot of related services need to be built and tested at the same
time if one upstream repository is changed. 

##### Fully Automatic Pipeline

Most of existing tools are trying to simplify the UI but it is very hard to build a pipeline
from one git merge to production.

##### Cache Local Repositories

Most devops tools will checkout from Git for the entire project for every build. The idea
was come from old source control software. In git, a simple git pull will only sync the
changed file from server and start the build.

##### Shared Dependencies Repository

Dependencies libraries don't need to be downloaded for every build and they need to be shared
by different build tasks to reduce the network traffic and speed up the build process. 

##### Shared Infrastructure Services

Modern applications need a lot of infrastructure services to run/test. For example, database
, Kafka etc. The build system needs to support shared environment without impact other build
tasks running at the same time.

##### Support Environment Segregation

You can have multiple bots running at different servers or even on the same server with
different environment tag in the configuration to be responsible for different environment
build. 

##### Easy to Plugin

The implementation of task executor can be easily replaced with externalized jar file and
configuration change. And different team can wire in only the plugins they need. 

 