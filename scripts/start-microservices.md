# 微服务 + 监控一键启动（需先 mvn package）

# 1. 基础设施
docker-compose up -d

# 2. 监控（Zipkin + Prometheus + Grafana）
docker-compose --profile monitoring up -d

# 3. 微服务（Gateway + User + Appointment + CodeGen）
docker-compose --profile microservices up -d --build

# 访问
# Gateway:    http://localhost:8080
# Grafana:    http://localhost:3000  (admin/admin)
# Prometheus: http://localhost:9090
# Zipkin:     http://localhost:9411
