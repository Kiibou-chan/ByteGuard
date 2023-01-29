# ByteGuard

ByteGuard is an implementation of jGuard using annotation processing and a java agent to add guards at load-time if
needed. The original jGuard introduces new syntax elements into the java language to annotate functions and classes with
guards and constraints. It then uses a custom compiler to compile the java code with the guards and constraints (or
without them).

The rules for ByteGuard guards and checks are not written in the same class they apply to. Instead, they exist in
external classes. This also means, that it is in principle possible to use other "api-external" specification languages
like CrySL as input for ByteGuard.

Since we can read the bytecode directly, we can also, in principle, do some static checking on the code. jGuard for
example can not encode the `hardCoded` rule from CrySL, since the JVM does not allow such introspection through
reflection. ByteGuard can implement such static checks. Thus, ByteGuard can incorporate static and dynamic
analysis approaches and implement a suit of hybrid analyses.

## General Structure

### byteguard-api

Contains all annotations and classes to allow annotations of guards and constraints on api classes.

### annotation-processor

Uses [byteguard-api](#byteguard-api), [auto-service](https://github.com/google/auto/tree/main/service).

Generates meta information files and classes which are read by the agent at load-time.
These encode all information necessary to correctly weave the check bytecode into the classes.

### bytecode-weaving

Uses [opal](https://www.opal-project.de/) for bytecode manipulation.
We currently use version `4.0.1-SNAPSHOT`, which must be manually built from the latest commit of the `develop` branch.
To do this, clone the [opal](https://www.opal-project.de/) repository and execute the `sbt publishM2` task.
This manual dependency build is necessary, because the public `4.0.0` version of opal uses scala 2.12, which is now
in maintenance.

### agent

Uses [bytecode-weaving](#bytecode-weaving), [byteguard-api](#byteguard-api).

To use the agent in a subproject of ByteGuard,
add `-javaagent:$ProjectFileDir$/agent/build/libs/agent-1.0-SNAPSHOT.jar` to the jvm
arguments in an IntelliJ run configuration. `$ProjectFileDir$` points to the root directory of the root project.

## Writing ByteGuard checks for an API

## Known Difficulties

### Instrumenting Bootstrap Classes

When writing specifications for classes which are loaded by the jvm BootstrapClassloader (like the standard library)
before our agent is loaded, the agent can not intercept the bytecode before it is loaded. Thus, we can not add new
fields to the class, which is necessary for injecting guards. If the class is not final (we are assuming that this
usually happens with interfaces like Iterator), we can create a wrapper class which implements the checks correctly.
If the class is from the java standard library or another library accessible statically at compile time, we can create
a table of constructor invocations which we then can instrument such that the created objects are wrapped by our checked
implementation.

### Dealing with Inheritance


