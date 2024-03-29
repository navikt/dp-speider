FROM eclipse-temurin:17 as jre-build

# Create a custom Java runtime
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.xml,java.naming,java.management \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

# Runtime
FROM debian:buster-slim
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

COPY build/install/* /

CMD ["dp-speider"]