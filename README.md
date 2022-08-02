# SpringCloud学习笔记

## 作者：诡锋    B站：-诡锋丿Lavafall-

对应的视频教程： https://www.bilibili.com/video/BV18E411x7eT

## 主要内容

![](http://124.222.43.240:2334/upload/2022-7-21$76143S34nD.png)

## 创建微服务模块

* 创建父工程，配置好pom，dependencyManagement等，以便子模块继承
* 建module
* 写yml
* 写主启动类
* 写业务类

## 服务注册中心

### Eureka服务注册与发现

（这玩意官网停更了，但是老项目历史遗留问题，你懂的，还是得学）

#### 服务治理

SpringCloud封装了Netflix公司开发的Eureka模块来**实现服务治理**。

在传统的rpc远程调用框架中，管理每个服务与服务之间依赖关系比较复杂，管理比较复杂，所以需要使用服务治理，管理服务与服务之间依赖关系，可以实现服务调用、负载均衡、容错等，实现服务发现与注册。

#### 服务注册

![](http://124.222.43.240:2334/upload/2022-7-26$93316GDij4.png)

**Eureka和Dubbo系统架构对比**

![](http://124.222.43.240:2334/upload/2022-7-26$88046EjeJS.png)

#### 单机Eureka构建步骤

1. IDEA生成EurekaServer端服务注册中心

   注意：这里用的是2020.2的新版本(2.x)

   ~~~xml
   以前的老版本(2018)
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-server-eureka</artifactId>
   </dependency>
   
   现在的新版本(2020.2)
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
   </dependency>
   ~~~

   使用@EnableEurekaServer对这个注册中心module的启动类进行注解。

   

2. 将其它模块作为EurekaClient注册进EurekaServer成为服务提供者(Provider)和消费者(Consumer)等

   和上面一样，引入EurekaClient的依赖

~~~xml
以前的老版本(2018)
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-server-eureka</artifactId>
</dependency>

现在的新版本(2020.2)
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
~~~

使用@EnableEurekaClient对所有服务module的启动类进行注解。



#### 集群Eureka构建步骤

![](http://124.222.43.240:2334/upload/2022-7-28$93325HXakt.png)

所以要搭建Eureka注册中心集群，实现负载均衡+故障容错

#### 集群Eureka的特点

集群中的多个Server是一个整体，相互注册，相互守望.

**比如这里，7001要注册进7002，而7002要注册进7001**

![](http://124.222.43.240:2334/upload/2022-7-28$18552eZpnS.png)

#### Server的集群配置

集群中有多个Server, YML配置文件跟单机版不一样

这里其实有**俩办法**

​    （1）修改hosts:

找到C:\Windows\System32\drivers\etc\hosts文件, 添加hosts:

~~~
### SpringCloud ###
127.0.0.1  eureka7001.com
127.0.0.1  eureka7002.com
~~~

​    （2）**<font color="red">但！是！我懒得改！我不喜欢动系统配置！所以我就用第二种方法：localhost和127.0.0.1来模拟了，这俩地址不一样的！</font>**



**注意一个大坑！！service-url的defaultZone必须加/eureka后缀，不然会导致Client只能注册进其中一个Server！！**

**但是加上/eureka后缀后就不能直接点击DS Replicas访问了，所以自己手动输入localhost：7001和127.0.0.1:7002来查看两个EurekaServer的情况吧！**

注册Server的时候，要互相注册，配置文件如下

~~~yaml
# 7001的application.yml 注意这里用localhost和127.0.0.1来模拟不同主机配置集群，不能都用
# localhost，会导致识别成同一主机，而不是一个集群

server:
  port: 7001

eureka:
  instance:
    hostname: localhost  # eureka服务端的实例名称
  client:
    # false表示不向注册中心注册自己
    register-with-eureka: false
    # false表示自己就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    fetch-registry: false
    service-url:
      # 设置与Eureka Server交互的地址查询服务和注册服务都需要依赖这个地址
      defaultZone: http://127.0.0.1:7002/eureka
      
# 7002的application.yml
server:
  port: 7002

eureka:
  instance:
    hostname: 127.0.0.1  # eureka服务端的实例名称
  client:
    # false表示不向注册中心注册自己
    register-with-eureka: false
    # false表示自己就是注册中心，我的职责就是维护服务实例，并不需要去检索服务
    fetch-registry: false
    service-url:
      # 设置与Eureka Server交互的地址查询服务和注册服务都需要依赖这个地址
      defaultZone: http://localhost:7001/eureka
~~~

此时，集群配置完毕，这是一个整体的服务注册中心，所以下一步就是：

#### Client注册到集群中

将业务的微服务80（消费者）和8001（生产者）（EurekaClient) 注册到**集群**中，也就是同时注册到7001和7002中

~~~yaml
# 8001的生产者Client客户端的application.yml，注意注册到集群中每一个Server
# 以防止某个Server down掉的时候不能使用该服务
server:
  port: 8001

# 此处省略数据库配置

eureka:
  client:
    # 表示是否将自己注册进EurekaServer，默认为true
    register-with-eureka: true
    # 是否从EurekaServer抓取已有的注册信息，默认为True。单节点无所谓，集群必须设置为
    # true，才能配合ribbon使用负载均衡
    fetch-registry: true
    service-url:
      # 设置与Eureka Server交互的地址查询服务和注册服务都需要依赖这个地址
      # 单机：defaultZone: http://localhost:7001/eureka
      # 以下是集群版配置
      defaultZone: http://localhost:7001/eureka,http://127.0.0.1:7002/eureka

# 此处省略myBatis配置


# 80端口消费者微服务配置的application.yml
server:
  port: 80

spring:
  application:
    name: cloud-order-service

eureka:
  client:
    # 表示是否将自己注册进EurekaServer，默认为true
    register-with-eureka: true
    # 是否从EurekaServer抓取已有的注册信息，默认为True。单节点无所谓，集群必须设置为
    # true，才能配合ribbon使用负载均衡
    fetch-registry: true
    service-url:
      # 设置与Eureka Server交互的地址查询服务和注册服务都需要依赖这个地址
      # 单机：defaultZone: http://localhost:7001/eureka
      # 以下是集群版配置
      defaultZone: http://localhost:7001/eureka,http://127.0.0.1:7002/eureka
~~~

#### 把服务提供者（生产者,Provider）配置成集群，并配置负载均衡

服务提供者8001的集群构建：注意，通常提供者（生产者）是多个，需要配置集群。

**方法：在同一个服务名称cloud-payment-service下配置两个不同端口的服务，那么他们注册的时候就会被注册在同一个微服务里，也就是集群。**

在提供者配置以后，消费者请求提供者时url不能写死，而要写服务名称

~~~java
//    public static final String PAYMENT_URL = "http://localhost:8001"; //单机版 请求url配置
      public static final String PAYMENT_URL = "http://cloud-payment-service";  //集群版 请求url配置 （别用大写CLOUD-PAYMENT-SERVICE，虽然Eureka查看页面是大写，但是写大写也要报错的！！）
~~~

**然后，一定要配置负载均衡！！不然找不到服务名，会直接报错！！**

如果使用的是RestTemplate，就修改ApplicationContext配置，也就是在@Configuration那个注解的配置类里面，@Bean上加上注解：@LoadBalanced

~~~java
@Configuration
public class ApplicationContextConfig {
    @Bean
    @LoadBalanced  //加上这个注解
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
~~~



#### Actuator微服务信息完善

主要是以下两点：

* 主机名称：服务名称修改
* 访问信息有IP信息提示\

**方法：在eureka下client同级缩进下添加instance，在里面写入instance-id和prefer-ip-address: true**

```yaml
eureka:
  client:
   #### 此处省略 ####
  # 修改服务中显示的主机名
  instance:
    instance-id: payment8002
    # 访问路径可以显示IP地址
    prefer-ip-address: true
```

如下图，添加完毕以后，服务里面的主机名就会显示成对应的instance-id了，而且访问的地址显示的是IP地址（图片左下角显示）

![](http://124.222.43.240:2334/upload/2022-7-29$21384p72Pi.png)



#### 服务发现 Discovery

对于注册进eureka里面的微服务，可以通过服务来获得该服务的信息

方法：

1. 修改Provider的Controller, 添加一个DiscoveryClient对象，并添加一个接口来查看相关信息

~~~java
import org.springframework.cloud.client.discovery.DiscoveryClient;
// 引入的是这个，别引入错了
public class PaymentController {
    .....
    @Resource
    private DiscoveryClient discoveryClient;
    .....
}

.......
@GetMapping(value = "/payment/discovery")
public Object discovery(){
    List<String> services = discoveryClient.getServices();
    for(String element : services){
        log.info("***element:" + element);
    }
    List<ServiceInstance> instances = discoveryClient.getInstances("cloud-payment-service");
    for(ServiceInstance instance : instances){
        log.info(instance.getServiceId() + "\t" + instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
    }
    return this.discoveryClient;
}
~~~

2. 在对应的主启动类上面添加注解：@EnableDiscoveryClient

然后就可以通过对应的接口查看当前服务中心中注册了哪些服务。

结果：

~~~json
{
    "services": [
    "cloud-payment-service",
    "cloud-order-service"
    ],  //可以看到注册了这俩服务
    "order": 0  //这个是顺序的意思，不用管
}
~~~





#### Eureka自我保护

保护模式主要用于一组客户端和Eureka Server之间存在网络分区场景下的保护。一旦进入保护模式，

<font color="red">Eureka Server将会尝试保护其服务注册表中的信息，不再删除服务注册表中的数据，也就是不会注销任何微服务。</font>

![](http://124.222.43.240:2334/upload/2022-7-29$73622KnycP.png)

**说人话：某时刻某一个微服务不可用了。Eureka不会立刻清理，依旧会对该微服务的信息进行保存。**

它的设计哲学就是宁可保留错误的服务注册信息，也不盲目注销任何可能健康的服务实例。

**<font color="red">简称： 好死不如赖活着</font>**



####  为啥会产生Eureka自我保护机制？

为了防止: **EurekaClient可以正常运行，但是与EurekaServer网络不通的情况**，EurekaServer**不会立刻**将EurekaClient服务剔除。



#### 怎么禁止自我保护？

Eureka默认开启了自我保护。

要想关闭，要对Server和Client都做一些操作。

Server端：

~~~yaml
# 在application.yml中
eureka:
    ## 此处省略##
    server:
        # 关闭自我保护机制，包装不可用服务被及时删除
        enable-self-preservation: false
        # 关闭自我保护机制的定时器，2000毫秒后关闭
        eviction-interval-timer-in-ms: 2000
~~~

Client端：

~~~yaml
# 在application.yml中
eureka:
    ## 此处省略##
    instance:
        instance-id: payment8001
        # 访问路径可以显示IP地址
        prefer-ip-address: true
        # Eureka客户端向客户端发送心跳的时间间隔，单位为秒（默认是30秒）
        lease-renewal-interval-in-seconds: 1
        # Eureka服务端在收到最后一次心跳后等待时间上限，单位为秒（默认90秒），超时将剔除服务
        lease-expiration-duration-in-seconds: 2
~~~



这样自我保护机制就关闭了，如果之后Client宕机了，在设定的时间以后，就会自动被Server移除。



#### Eureka停更说明

在https://github.com/Netflix/eureka/wiki，可以看到Eureka 2.0已经停更了，Eureka基本不咋用了，深入的东西就没必要了，所以用SpringCloud整合ZooKeeper代替Eureka。



### ZooKeeper服务注册与发现

#### ZooKeeper是啥？

ZooKeeper是一个分布式协调工具，可以实现注册中心功能，取代Eureka服务器作为服务注册中心。

#### ZooKeeper服务是否持久

ZooKeeper**不是持久性的**，当你关闭Client以后一段时间，ZooKeeper就会检测到这个Client没有心跳了，然后就把服务干掉了。

#### 搭建单机ZooKeeper Server

具体过程请看这篇文档：

[如何搭建zookeeper单机实例](./搭建zookeeper.md)

#### 对单机ZooKeeper进行服务注册

还是老套路，注册生产者和消费者

pom.xml不再引入eureka相关的东西，而是引入：

~~~xml
  <!--  引入zookeeper -->
  <dependency>
      <groupId>org.apache.zookeeper</groupId>
      <artifactId>zookeeper</artifactId>
      <version>3.8.0</version>    <!-- 这里看你的zookeeper版本 -->
  </dependency>
    <!-- 用于Zookeeper服务发现 -->
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
  </dependency>
~~~



application.yml这么写

~~~yaml
server:
  port: 8004
  
# 省略数据库配置和MyBatis配置

# 服务别名，注册zookeeper的注册中心名称
spring:
  application:
    name: cloud-provider-payment
  cloud:
    zookeeper:
      connect-string: 你的Zookeeper实例所在服务器:2181
~~~

其它基本一样

**关于Zookeeper集群的配置，请查看这篇文档：[如何搭建zookeeper模拟集群](./搭建zookeeper.md)**

**服务注册方法和Eureka类似，这里就先不写了。**



### Consul服务注册与发现

#### Consul是啥？

Consul是一套开源的分布式服务发现和配置管理系统，由HashiCorp公司**用Go语言开发**。

它提供了微服务系统中的服务治理、配置中心、控制总线等功能。这些功能中的每一个都可以根据需要单独使用，也可以一起使用以构建全方位的服务网格。

总之Consul提供了一种完整的服务网格解决方案。

#### 下载安装Consul

有服务器的推荐在服务器上安装，不占本地硬盘空间

https://www.consul.io/downloads

在本地上的consul启动方法：

~~~shell
consul agent -dev
~~~

在服务器上的consul启动方法：

~~~shell
consul agent -dev -client=0.0.0.0  # -client 0.0.0.0是为了开启外网远程访问
~~~

#### 服务提供者（生产者）注册进Consul

同样的，省略数据库配置了，毕竟是学习，实际应用中自己配就好

pom.xml

~~~xml
 <dependencies>
        <!--  SpringCloud consul server  -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!--  引入自定义的cloud-api-commons模块，来访问实体类   -->
        <dependency>
            <groupId>com.guifeng</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
~~~

application.yml

~~~yaml
# consul服务端口号
server:
  port: 8006

spring:
  application:
    name: consul-provider-payment
  # consul注册中心地址
  cloud:
    consul:
      host: 你的服务器
      port: 8500  # 这个可以在consul启动的那个命令行窗口看到
      discovery:
        # hostname: xxx
        service-name: ${spring.application.name}
        register-health-check: false # 不进行健康检测
~~~



#### 服务消费者注册进Consul

pom.xml

~~~xml
    <dependencies>
        <!--  SpringCloud consul server  -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!--  引入自定义的cloud-api-commons模块，来访问实体类   -->
        <dependency>
            <groupId>com.guifeng</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
~~~

application.yml

~~~yaml
# consul服务端口号
server:
  port: 80

spring:
  application:
    name: consul-consumer-order
  # consul注册中心地址
  cloud:
    consul:
      host: 你的服务器
      port: 8500
      discovery:
        # hostname: xxx
        service-name: ${spring.application.name}
        register-health-check: false # 不进行健康检测

~~~



### 三个注册中心的异同点

#### CAP的概念：

CAP定理：指的是在一个分布式系统中，Consistency（一致性）、 Availability（可用性）、Partition tolerance（分区容错性），三者不可同时获得。

其中：
一致性（C）：所有节点都可以访问到最新的数据
可用性（A）：每个请求都是可以得到响应的，不管请求是成功还是失败
分区容错性（P）：除了全部整体网络故障，其他故障都不能导致整个系统不可用



**<font color="red">CAP理论的核心是：一个分布式系统不可能同事很好的满足一致性，可用性，分区容错性这三个要求</font>**

![](http://124.222.43.240:2334/upload/2022-8-2$27841SZCAt.png)

#### 异同点

| 组件名    | 语言 | CAP  | 服务健康检查 | 对外暴露接口 | SpringCloud集成 |
| --------- | ---- | ---- | ------------ | ------------ | --------------- |
| Eureka    | Java | AP   | 可配支持     | HTTP         | 已集成          |
| Consul    | Go   | CP   | 支持         | HTTP/DNS     | 已集成          |
| Zookeeper | Java | CP   | 支持         | 客户端       | 已集成          |



## 服务调用

### Ribbon负载均衡服务调用

#### Ribbon是啥？

Spring Cloud Ribbon是基于Netflix Ribbon实现的一套**客户端负载均衡的工具**。

Ribbon是Netflix发布的开源项目，主要功能是提供客户端的**软件负载均衡算法和服务调用**。

Ribbon客户端组件提供一系列完善的配置项如连接超时，重试等。简单来说，就是在配置文件中列出Load Balancer(负载均衡，简称LB) 后面所有的机器，Ribbon会自动的帮助你基于某种规则（如简单轮询，随机连接等）去连接这些机器。



#### Ribbon官网（GitHub)

https://github.com/Netflix/ribbon/wiki/Getting-Started

Ribbon目前也进入了维护模式（停更了）

但是：ribbon-eureka: **deployed at scale in production**

这玩意在大量生产环境中部署使用了，所以还有学习的价值



#### 负载均衡(Load Balance, LB)是啥？

就是将用户的请求平摊到多个服务上，从而达到系统的HA(高可用)。

常见的负载均衡有软件Nginx，LVS，硬件F5等。



#### Ribbon本地负载均衡客户端 与 Nginx服务端负载均衡的区别

Nginx 是服务器负载均衡，客户端所有请求都会交给Nginx，然后由Nginx实现转发请求。即负载均衡是由服务端实现的。

Ribbon是本地负载均衡，在调用微服务接口的时候，会在注册中心上获取注册信息服务列表之后缓存到JVM本地，从而在本地实现RPC远程服务调用技术。



#### 集中式负载均衡（服务端）

即在服务的消费方和提供方之间使用独立的负载均衡设施（如Nginx软件或者F5硬件），由该设施负责把访问请求通过某种策略转发至服务的提供方。



#### 进程内负载均衡（本地）

将负载均衡逻辑集成到消费方，消费方从服务注册中心获知有哪些地址可用，然后自己再从这些地址中选择出一个合适的服务器。

**Ribbon就属于进程内负载均衡**，它只是一个类库，集成于消费方进程，消费方通过它来获取到服务提供方的地址。



#### Ribbon工作原理示意图

**Ribbon一般是给消费者用，让Ribbon帮消费者去分配请求哪个服务**

![](http://124.222.43.240:2334/upload/2022-8-2$39178miFm7.png)

#### Ribbon引入的一个重点

我们在pom.xml里面并没有专门引入ribbon

~~~xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
</dependency>
~~~

但是也能使用ribbon，是因为

~~~xml
 <dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
 </dependency>
~~~

这个东西里面自带了一个ribbon

![](http://124.222.43.240:2334/upload/2022-8-2$53924wtb5J.png)



#### RestTemplate的使用

主要方法：

~~~java
getForObject() / postForObject() 也就是get post方法发请求拿到json对象（响应体）
getForEntity() / postForEntity() 除了会拿到json对象（响应体），还会拿到响应头等很多响应的内容
~~~



#### Ribbon核心组件IRule

IRule是一个Java接口（Interface），功能是根据特定算法从服务列表中选取一个要访问的服务。

![](http://124.222.43.240:2334/upload/2022-8-2$214158hy2b.png)

* RoundRobinRule: **轮询**

* RandomRule: **随机**

* RetryRule: **重试**（先按照**轮询**获取，获取失败就在指定时间内重试）
* BestAvailableRule: 会先过滤掉由于多次访问故障而处于断路器跳闸状态的服务，然后选择一个并发量最小的服务。
* WeightedResponseTimeRule：对**轮询**的扩展，响应速度越快的实例选择权重越大，越容易被选择。
* AvailabilityFilteringRule：先过滤掉故障实例，再选择并发较小的实例
* ZoneAvoidanceRule：**默认规则**，复合判断server所在区域的性能和server的可用性选择服务器。



未完待续。。。





## 服务降级



## 服务网关



## 服务配置





