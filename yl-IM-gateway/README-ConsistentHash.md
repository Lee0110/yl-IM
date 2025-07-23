# WebSocket一致性哈希路由使用说明

## 功能概述

在gateway网关中实现了基于一致性哈希的WebSocket连接路由功能，确保相同用户的WebSocket连接总是路由到同一台服务器上，这对于维护用户会话状态非常重要。

## 实现原理

1. **一致性哈希负载均衡器**：`ConsistentHashLoadBalancer`类实现了Spring Cloud LoadBalancer接口
2. **用户ID提取**：从多个位置提取用户标识（查询参数、请求头、URL路径）
3. **服务器选择**：使用Guava一致性哈希工具类选择目标服务器
4. **自动配置**：仅对`yl-im-netty`服务使用一致性哈希，其他服务使用默认负载均衡

## 配置文件

### application.yml
```yaml
spring:
  cloud:
    gateway:
      routes:
        # WebSocket 路由配置
        - id: yl-im-websocket
          uri: lb://yl-im-netty
          predicates:
            - Path=/ws/**
          filters:
            - StripPrefix=0
    loadbalancer:
      configurations: local
```

## 用户ID提取规则

系统会按以下优先级提取用户ID：

1. **查询参数**：`?userId=12345`
2. **请求头**：`X-User-Id: 12345`
3. **URL路径**：`/ws/12345` （第三段作为用户ID）

## 使用示例

### WebSocket连接示例

```javascript
// 方式1：通过查询参数
const ws1 = new WebSocket('ws://localhost:10001/ws/chat?userId=12345');

// 方式2：通过URL路径
const ws2 = new WebSocket('ws://localhost:10001/ws/12345/chat');

// 方式3：通过请求头（需要在建立连接时设置）
const ws3 = new WebSocket('ws://localhost:10001/ws/chat', [], {
    headers: {
        'X-User-Id': '12345'
    }
});
```

### 测试接口

#### 1. 测试单个用户路由
```
GET http://localhost:10001/test-hash?userId=12345&serviceName=yl-im-netty
```

响应示例：
```json
{
    "userId": "12345",
    "serviceName": "yl-im-netty",
    "availableInstances": ["192.168.1.100:8080", "192.168.1.101:8080"],
    "selectedInstance": "192.168.1.100:8080",
    "totalInstances": 2
}
```

#### 2. 测试负载均衡分布
```
GET http://localhost:10001/test-distribution?serviceName=yl-im-netty&userCount=100
```

响应示例：
```json
{
    "serviceName": "yl-im-netty",
    "totalUsers": 100,
    "availableInstances": ["192.168.1.100:8080", "192.168.1.101:8080"],
    "distribution": {
        "192.168.1.100:8080": 48,
        "192.168.1.101:8080": 52
    },
    "averageUsersPerInstance": 50.0,
    "maxDeviationFromAverage": 2.0
}
```

## 关键特性

### 1. 会话粘性
相同用户ID的连接总是路由到同一台服务器，确保：
- WebSocket会话状态一致性
- 用户消息不会丢失
- 减少服务器间的状态同步需求

### 2. 负载均衡
- 使用一致性哈希算法确保用户均匀分布
- 当服务器节点变化时，只有少量用户需要重新路由
- 避免了传统哈希在节点变化时的大量重新分配

### 3. 容错处理
- 如果无法提取用户ID，使用默认负载均衡
- 如果选择的服务器不可用，自动选择第一个可用服务器
- 兼容现有的Spring Cloud Gateway配置

## 部署注意事项

1. **服务发现**：确保`yl-im-netty`服务已注册到Nacos
2. **用户ID传递**：客户端连接时必须提供用户ID
3. **监控**：使用测试接口监控负载分布情况
4. **扩缩容**：新增或删除服务器节点时，一致性哈希会自动重新分配

## 故障排查

### 1. 连接路由不一致
- 检查用户ID是否正确传递
- 验证服务实例是否在Nacos中正确注册
- 查看gateway日志确认路由逻辑

### 2. 负载不均衡
- 使用`/test-distribution`接口检查分布情况
- 确认服务实例数量和健康状态
- 检查一致性哈希算法的虚拟节点配置

### 3. 编译或启动错误
- 确保common模块已正确编译并安装到本地仓库
- 检查Spring Cloud版本兼容性
- 验证所有依赖项是否正确导入

## 性能优化建议

1. **缓存优化**：考虑缓存服务实例列表以减少服务发现调用
2. **监控指标**：添加路由选择的监控指标
3. **连接池**：在高并发场景下优化WebSocket连接池配置
4. **日志记录**：添加详细的路由决策日志用于调试

通过这个实现，你的WebSocket连接现在可以基于用户ID进行一致性哈希路由，确保相同用户总是连接到同一台服务器，提高了系统的可靠性和用户体验。
