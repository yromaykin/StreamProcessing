FROM landoop/fast-data-dev:cp3.3.0

USER root

RUN apk add --update git

# Install Maven
#ENV MAVEN_VER 3.5.4
#RUN mkdir -p /tmp \
#    && curl -fSL -o /tmp/maven.tar.gz http://it.apache.contactlab.it/maven/maven-3/$MAVEN_VER/binaries/apache-maven-$MAVEN_VER-bin.tar.gz; \
#    tar -xzf /tmp/maven.tar.gz -C /opt; \
#    mv /opt/apache-maven-$MAVEN_VER /opt/maven
#ENV PATH $PATH:/opt/maven/bin
RUN mkdir -p /opt/confluent/share/java/kafka-connect-spooldir
ADD kafka-connect-spooldir/ /opt/confluent/share/java/kafka-connect-spooldir

##ENV KAFKA_CONNECT_PLUGINS_DIR=/opt/landoop/connectors/third-party/
#ARG KAFAK_CONNECT_SPOOLDIR_URL="https://repo1.maven.org/maven2/com/github/jcustenborder/kafka/connect/kafka-connect-spooldir/1.0.31/kafka-connect-spooldir-1.0.31.tar.gz"
#RUN wget $DEVARCH_USER $DEVARCH_PASS "$KAFAK_CONNECT_SPOOLDIR_URL" \
#        -O /opt/spooldir.tar.gz \
##    && mkdir -p /opt/confluent/share/java/kafka-connect-spooldir \
#    && mkdir -p /tmp/kafka-connect-spooldir \
#    && tar --no-same-owner -xf /opt/spooldir.tar.gz \
#        -C /tmp/kafka-connect-spooldir \
#    && mv /tmp/kafka-connect-spooldir/usr/share/kafka-connect/kafka-connect-spooldir /opt/confluent/share/java \
#    && rm -rf /opt/spooldir.tar.gz

#ARG KAFAK_CONNECT_SPOOLDIR_URL="https://github.com/jcustenborder/kafka-connect-spooldir/archive/1.0.41.tar.gz"
#RUN mkdir -p /tmp/kafka-connect-spooldir; \
#    wget $DEVARCH_USER $DEVARCH_PASS "$KAFAK_CONNECT_SPOOLDIR_URL" \
#        -O /tmp/kafka-connect-spooldir/spooldir.tar.gz \
#    && tar --no-same-owner -xf /tmp/kafka-connect-spooldir/spooldir.tar.gz \
#        -C /tmp/kafka-connect-spooldir/ \
##    && cd /tmp/kafka-connect-spooldir;/ \
#    && mvn -f /tmp/kafka-connect-spooldir/kafka-connect-spooldir-1.0.41 -DskipTests package; \
#    mv /tmp/kafka-connect-spooldir/kafka-connect-spooldir-1.0.41/target/kafka-connect-target/usr/share/java/kafka-connect-spooldir /opt/confluent/share/java