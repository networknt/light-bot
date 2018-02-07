# light-bot
A microservice based devops server and agent that handle multiple repositories and dependencies
from multiple organizations or git servers.

### Why this tool

##### Multiple Repo First

All existing devops tools on the market are focusing on single repo but in a microservices
architecture, there are a lot of related services need to be built and tested at the same
time if one upstream repository is changed.  

##### Fully Automatic Pipeline

Most of existing tools are trying to simplify the UI but it is very hard to build a pipeline
from one git merge to production. The cycle git pull-->build-->unit tests-->integration tests-->
scanning-->image creation should/will be repeated at every commit, automatically. 

##### Focus on Code

Most devops tools focus on the interface with configurations and plugins to allow users to
design their pipelines. It is very easy to start with but for complicated workflow, it is
very hard to with just configuration and a stack of plugins. We are trying to do DevOps as
code. 

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
build. The test cycle in a secure zone against real back-ends needs to be segregated, via 
config, and includes also an environment specific build (say include a config server), 
then test against that back-end. 

##### Easy to Plugin

The implementation of task executor can be easily replaced with externalized jar file and
configuration change. And different team can wire in only the plugins they need. 

##### Build across Multiple Organizations

In microservices architecture, different teams might have their own organizations and some
of the dependencies are across multiple organizations. The devops tool need to know how to
checkout and build repositories from many organization. Also, these related services might 
reside in different git providers or git servers.

##### Idempotency  

The testing aspect of the build, whether it is against multiple environments or not, should 
indeed use shared infrastructure (say Kafka across DIT/SIT) but also segregated back-ends. 
In all cases, testing using multiple bots needs to be idempotent. Idempotent testing is an 
aspect with most customers are struggling right now, especially in an evolving eco-system 
(ex, you don't have create and delete APIs available maybe yet you still wish to test 
adding a product to a customer. this needs to be addressed via other means)
