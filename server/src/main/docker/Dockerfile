FROM openjdk-21-dbg-asyncprofiler:latest

COPY target/${project.artifactId}-${project.version}-jar-with-dependencies.jar /work/
ENTRYPOINT java -jar /work/${project.artifactId}-${project.version}-jar-with-dependencies.jar
