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

### annotations

Contains all annotations and classes to allow annotations of guards and constraints on api classes.

### annotation-processor

Uses [annotations](#annotations), [auto-service](https://github.com/google/auto/tree/main/service).

Generates meta information files and classes which are read by the agent at load-time.
These encode all information necessary to correctly weave the check bytecode into the classes.

### bytecode-weaving

Uses [opal](https://www.opal-project.de/) for bytecode manipulation.
We currently use version `4.0.1-SNAPSHOT`, which must be manually built from the latest commit of the `develop` branch.
To do this, clone the [opal](https://www.opal-project.de/) repository and execute the `sbt publishM2` task.
This manual dependency build is necessary, because the public `4.0.0` version of opal uses scala 2.12, which is now
in maintenance.

### agent

Uses [bytecode-weaving](#bytecode-weaving), [annotations](#annotations).


## API-Internal Annotation



## API-External Annotation


