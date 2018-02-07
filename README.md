# light-bot
A microservice based devops server and agent that handle multiple repositories and dependencies
from multiple organizations or even multiple git servers.

### Why this tool

##### Multiple Repo First

All existing devops tools on the market are focusing on single repo but in a microservices
architecture, there are a lot of related services and libraries need to be built and tested at 
the same time if one upstream repository is changed. For example, in networknt organization,
if light-4j is changed, we need to build and test another dozens of repos that are depending
on light-4j. If you build light-4j only, chances are light-eventuate-4j is broken due to the 
change introduced in light-4j although all unit test cases passed in light-4j. 

When we adopt microserivces architecture, a traditional monolithic application will be split
into dozens smaller services and each service will have its own repository. Sometime, these
repository will be scattered in multiple organizations or multiple git servers. When one of
the services changed, we need to build/test it and we need to build other upstream and downstream
services as well to do the integration tests to ensure all of them are working together. It
is very hard to define this kind of dependencies and building multiple repositories in today's
DevOps tools as they are all single repo focused.   

##### Fully Automatic Pipeline with Code

Most of existing tools are trying to provide a fancy UI but it is very hard to build a pipeline
from one git merge to production with multiple repositories involved. The cycle git pull-->build-->
unit tests-->integration tests-->scanning-->image creation should/will be repeated at every commit, 
automatically with all the dependencies and contract tested. With only configurations and plugins,
it is very hard to build a complicated multiple repo pipe. Writing your own plugins can help but
you loose the flexibility of the code and control. Basically, we are trying to provide a framework
that you can compose you pipeline with dependency injection. What we are trying to do is DevOps as
code. 


##### Linxu and Git only

Our target is cloud native environment and we don't need to worry about Windows support and other
version control system like DevOps tools. This will save us 80 percent of time and resource to
deliver a light-weight and optimized solution for our target user base. If you look at the most
popular DevOps tool Jenkins, it has built an OS abstract layer to support multiple operating systems
and an SCM abstract layer to support all type of version control system like CVS and SVN etc.
This make the code very hard to reason about and at the same time you loose the opportunity to
optimize it.  

##### Cache Local Repositories

Most devops tools will checkout/clone from Git for the entire project for every build. The idea
was come from old source control software. In git, a simple git pull will only sync the changed 
files from server and start the build.

For example, if we change one repo in networknt organization, light-bot takes seconds
to pull from the repo and build over 20 other repos in the workspace immediately. Jenkins will
clone all repositories to its workspace from scratch and it takes about 10 minutes. This might
repeat dozens or hundreds of times depending on how big is your team. 

##### Shared Dependencies Repository

Dependencies libraries don't need to be downloaded for every build and they need to be shared
by different build tasks on the same host to reduce the network traffic and speed up the build 
process. In Jenkins, we had hard time to to share the .m2 local repository to support multiple
related build as it treat every build as in dependent and build each one in a separated environment.
This generate too much network traffic and slows down the build process dramatically.   

##### Shared Infrastructure Services

Modern applications need a lot of infrastructure services to run/test. For example, database
, Kafka etc. The build system needs to support shared environment without impact other build
tasks running at the same time. It needs to be Docker friendly and have support isolation on
shared environment without impact other builds running in parallel. 

##### Support Environment Segregation

You can have multiple bots running at different servers or even on the same server with
different environment tag in the configuration to be responsible for different environment
build. The test cycle in a secure zone against real back-ends needs to be segregated, via 
config, and includes also an environment specific build (say include a config server), 
then test against that back-end. 

##### Easy to Plugin

The implementation of task executor can be easily replaced with externalized jar file and
configuration change. And different team can wire in only the plugins they need. As it is an
open source framework, it is very easy to test your own plugin and see the interactions
during the build. You can also customize the framework for your own needs.  

##### Build across Multiple Organizations

In microservices architecture, different teams might have their own organizations and some
of the dependencies are across multiple organizations. The devops tool need to know how to
checkout and build repositories from many organization. Also, these related services might 
reside in different git providers or git servers. To manage the the access control in this
complicated git environment in DevOps tool is a daunting job. 

##### Idempotency  

The testing aspect of the build, whether it is against multiple environments or not, should 
indeed use shared infrastructure (say Kafka across DIT/SIT) but also segregated back-ends. 
In all cases, testing using multiple bots needs to be idempotent. Idempotent testing is an 
aspect with most customers are struggling right now, especially in an evolving eco-system 
(ex, you don't have create and delete APIs available maybe yet you still wish to test 
adding a product to a customer. this needs to be addressed via other means)
