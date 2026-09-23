# 后端镜像：直接使用已打包的 jar（先在宿主机执行 mvn package）
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/market-backend-1.0.0.jar app.jar
# 上传目录挂载点
RUN mkdir -p /app/upload
VOLUME /app/upload
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
