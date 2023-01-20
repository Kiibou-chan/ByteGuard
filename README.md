# JGuard-Agent

JGuard-Agent is an implementation of JGuard using annotation processing and a java agent to add guards at load-time if 
needed. The original JGuard introduces new syntax elements into the java language to annotate functions and classes with
guards and constraints. It then uses a custom compiler to compile the java code with the guards and constraints (or 
without them).

For JGuard, direct access to the library sourcecode is needed to implement the constraints. With our approach, this is 
not the case. We allow api-internal, as well as api-external annotation methodologies to tell JGuard-Agent which checks
to implement at load-time. This way, JGuard-Agent can also ingest specification languages like CrySL and create guards 
and constraints based on those specifications.

Since we can read the bytecode directly, we can also, in principle, do some static checking on the code. JGuard for
example can not encode the `hardCoded` rule from CrySL, since the JVM does not allow such introspection through
reflection. JGuard-Agent can implement such static checks. Thus, JGuard-Agent can incorporate static and dynamic
analysis approaches and implement a suit of hybrid analyses.

## General Structure

### jguard-api

Contains all annotations and classes to allow annotations of guards and constraints on api classes.

### annotation-processor

Uses [jguard-api](#jguard-api), [auto-service](https://github.com/google/auto/tree/main/service).

Generates meta information files and classes which are read by the agent at load-time.
These encode all information necessary to correctly weave the check bytecode into the classes.

### bytecode-weaving

Uses [opal](https://www.opal-project.de/) for bytecode manipulation.
We currently use version `4.0.1-SNAPSHOT`, which must be manually built from the latest commit of the `develop` branch.
To do this, clone the [opal](https://www.opal-project.de/) repository and execute the `sbt publishM2` task.
This manual dependency build is necessary, because the public `4.0.0` version of opal uses scala 2.12, which is now
in maintenance.

### agent

Uses [bytecode-weaving](#bytecode-weaving), [jguard-api](#jguard-api).

To use the agent in a subproject of jguard-agent, add `-javaagent:$ProjectFileDir$/agent/build/libs/agent-1.0-SNAPSHOT.jar` to the jvm
arguments in an IntelliJ run configuration. `$ProjectFileDir$` points to the root directory of the root project.

## API-Internal Annotation



## API-External Annotation

When writing specifications for classes which are loaded by the jvm BootstrapClassloader (like the standard library)
before our agent is loaded, the agent can not intercept the bytecode before it is loaded. Thus, we can not add new
fields to the class, which is necessary for injecting guards. If the class is not final (we are assuming that this
usually happens with interfaces like Iterator), we can create a wrapper class which implements the checks correctly.
If the class is from the java standard library or another library accessible statically at compile time, we can create
a table of constructor invocations which we then can instrument such that the created objects are wrapped by our checked
implementation.
