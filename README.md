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

#### Ribbon负载规则替换

**<font color="red">官方文档明确给出了警告：</font>**

**<font color="red">这个自定义配置类不能放在@ComponentScan所扫描的当前包下以及子包下，否则我们自定义的这个配置类就会被所有的Ribbon客户端所共享，达不到特殊化定制的目的了</font>**

**说人话就是，不要放在SpringBootApplication启动类所在的包以及它的所有子包里面。**

单独建一个包myrule

创建MySelfRule类

~~~java
@Configuration
public class MySelfRule {
    @Bean
    public IRule myRule(){
         return new RandomRule();  //定义为随机规则
    }
}

~~~

然后在主启动类里面加入@RibbonClient

~~~java
@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "cloud-payment-service",configuration = MySelfRule.class)
public class OrderMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderMain80.class, args);
    }
}
~~~

#### Ribbon负载均衡轮询(RoundRobinRule)算法原理

**<font color="red">负载均衡原理：</font>**

**<font color="red">rest接口第几次请求数 % 服务器集群总数量 = 实际调用服务器位置下标</font>**

**每次服务重启动后rest接口计数从1开始。**

~~~java
List<ServiceInstance> instances = discoveryClient.getInstances("cloud-payment-service");
~~~
如：

~~~java
 instances[0] = 127.0.0.1:8002
 instances[1] = 127.0.0.1:8001
~~~

8001 + 8002组合成为集群，他们共计2台机器，集群总数为2，按照轮询算法原理：

当总请求数为1时：1%2 =1 对应下标位置为1，则获得服务地址为：127.0.0.1:8001

当总请求数为2时：2%2 =0 对应下标位置为0，则获得服务地址为：127.0.0.1:8002

当总请求数为3时：3%2 =1 对应下标位置为1，则获得服务地址为：127.0.0.1:8001

当总请求数为4时：4%2 =0 对应下标位置为0，则获得服务地址为：127.0.0.1:8002

........

#### 手写轮询算法

   原理：JUC (CAS + 自旋锁)

先关掉ApplicationBeanContext中的@LoadBalanced，然后自己写

定义一个接口：

~~~java
public interface LoadBalancer {
    ServiceInstance instances(List<ServiceInstance> serviceInstances);
}
~~~

实现接口，写算法：

~~~java
@Component
public class MyLoadBalancer implements LoadBalancer{
    private AtomicInteger atomicInteger = new AtomicInteger(0);
    public final int getAndIncrement(){
        int current;
        int next;
        do{
            current = this.atomicInteger.get();
            next = current >= 2147483647 ? 0 : current + 1;

        }while(!this.atomicInteger.compareAndSet(current,next));
        System.out.println("第几次访问，次数next: " + next);
        return next;
    }

    @Override
    public ServiceInstance instances(List<ServiceInstance> serviceInstances) {
        int index = getAndIncrement() % serviceInstances.size();
        return serviceInstances.get(index);
    }
}

~~~

在消费者Order80的Controller中使用

~~~java
 @Resource
 private LoadBalancer loadBalancer;

//这里需要预先在Provider里面提供/payment/lb接口
    @GetMapping(value = "/consumer/payment/lb")
    public String getPaymentLB(){
        List<ServiceInstance> instances = discoveryClient.getInstances("cloud-payment-service");
        if(instances == null || instances.size() <= 0){
            return null;
        }

        ServiceInstance serviceInstance = loadBalancer.instances(instances);
        URI uri = serviceInstance.getUri();

        return restTemplate.getForObject(uri+"/payment/lb",String.class);
  }
~~~

### OpenFeign服务调用

早期的Feign已经停止更新，所以不需要研究了

#### Feign是啥？

Feign是一个声明式WebService客户端，使用Feign能让编写Web Service客户端更加简单。它的使用方法是**定义一个服务接口然后再上面添加注解**。Feign也支持可拔插式的编码器和解码器。

#### OpenFeign是啥？

OpenFeign是Spring Cloud在Feign的基础上支持了SpringMVC的注解，如@RequestMapping等等。OpenFeign的@FeignClient可以解析SpringMVC的@RequestMapping注解下的接口，并通过动态代理的方式产生实现类，实现类中做负载均衡并调用其他服务。

**注意：Feign是在消费者使用**

#### 使用OpenFeign

**OpenFeign 2.x.x自带Ribbon负载均衡配置项，使用的是轮询算法**

在消费者里面的pom.xml中引入：

~~~xml
        <!-- openfeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
~~~

**注意：OpenFeign 2.x.x版本中整合了Ribbon，但是3版本就没有整合Ribbon了，在本笔记中，使用的是2.2.1版本，所以自带Ribbon。**

现在主启动类添加@EnableFeignClients注解

~~~java
@SpringBootApplication
@EnableFeignClients
public class OrderFeignMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderFeignMain80.class, args);
    }
}

~~~

然后创建接口，添加注解@FeignClient(value = "cloud-payment-service")，注意value填的是提供者的服务名称

~~~java
@FeignClient(value = "cloud-payment-service")
public interface PaymentFeignService {
    @GetMapping(value = "/payment/get/{id}")
    public CommonResult<Payment> getPaymentById(@Param("id") Long id);
}
~~~

#### OpenFeign超时控制

1. 模拟超时错误

OpenFeign默认等待1秒钟，所以如果我们让线程停留3秒来模拟超时，就可以发现报错：Read Timed out

在提供者8001里面的Controller:

~~~java
  //模拟Feign超时控制的接口,这里线程停3秒，feign默认只等待1秒
    @GetMapping(value = "/payment/feign/timeout")
    public String paymentFeignTimeout(){
        //暂停几秒钟线程
        try{
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        return serverPort;
    }
~~~

然后还是在Feign消费者的接口里面写入

~~~java
 @GetMapping(value = "/payment/feign/timeout")
    String paymentFeignTimeout();
~~~

在Feign消费者的Controller写入：

~~~java
   @GetMapping(value = "/consumer/payment/feign/timeout")
    public String paymentFeignTimeout(){
        //openfeign-ribbon，客户端一般默认等待1秒
        return paymentFeignService.paymentFeignTimeout(); //这边要花3秒钟
    }
~~~

2. 对超时进行控制：

   在OpenFeign管理的消费者的application.yml中写入

   ~~~yaml
   feign:
     client:
       config:
         default:
           # 设置feign客户端超时时间
           # 指的是建立连接后从服务器读取到可用资源所用的时间，单位：毫秒
           read-timeout: 5000
           # 指的是建立连接所用的时间，适用于网络状况正常的情况下，两端连接所用的时间
           connect-timeout: 5000
   
   ~~~

   然后发现可以正常等3秒访问了。

   

#### OpenFeign日志打印功能

Feign提供了日志打印功能，我们可以通过配置来调整日志级别，从而了解 Feign 中 HTTP 请求的细节。

说人话就是**<font color="red">对Feign接口的调用情况进行监控和输出</font>**。



##### 日志级别

* NONE: 默认的，不显示任何日志
* BASIC: 仅记录请求方法、URL、响应状态码及执行时间
* HEADERS: 除了BASIC中定义的信息之外，还有请求头和响应头信息
* FULL: 除了HEADERS中定义的信息之外，还有请求和响应的正文及元数据。

在OpenFeign的消费者模块里面，建一个config包，写一个配置类

~~~java
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
}
~~~

然后在application.yml中添加：

~~~yaml
logging:
  level:
    # feign日志以什么级别监控哪个接口
    application.service.PaymentFeignService: debug
~~~



配置完成后，当调用这个消费者接口后，就可以在对应的服务器查看到日志了

~~~
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] <--- HTTP/1.1 200 (246ms)
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] connection: keep-alive
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] content-type: application/json
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] date: Thu, 04 Aug 2022 06:42:51 GMT
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] keep-alive: timeout=60
2022-08-04 14:42:51.293 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] transfer-encoding: chunked
2022-08-04 14:42:51.294 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] 
2022-08-04 14:42:51.295 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] {"code":200,"message":"查询成功,服务器端口为：8001","data":{"id":1,"serial":"老电线"}}
2022-08-04 14:42:51.295 DEBUG 7612 --- [p-nio-80-exec-1] application.service.PaymentFeignService  : [PaymentFeignService#getPaymentById] <--- END HTTP (100-byte body)
2022-08-04 14:42:52.124  INFO 7612 --- [erListUpdater-0] c.netflix.config.ChainedDynamicProperty  : Flipping property: cloud-payment-service.ribbon.ActiveConnectionsLimit to use NEXT property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit = 2147483647
~~~


## 服务降级

### 分布式系统面临的问题

复杂分布式体系结构中的应用程序有**数十个依赖关系**，每个依赖关系在某些时候将不可避免的失败。

![](http://124.222.43.240:2334/upload/2022-8-4$16214Y37GZ.png)



#### 服务雪崩

多个微服务之间调用的时候，假设微服务A调用微服务B和微服务C，微服务B和微服务C又调用其它的服务，这就是**<font color="red">"扇出"</font>**。如果扇出的链路上某个微服务的调用响应时间过长或者不可用，对微服务A的调用就会占用越来越多的系统资源，进而引起系统崩溃，所谓的”雪崩效应“。



### Hystrix

Hystrix是一个用于处理分布式系统的**延迟**和**容错**的开源库，在分布式系统里，许多依赖不可避免的会调用失败，比如超时，异常等。Hystrix能够保证在一个依赖出问题的情况下，**不会导致整体服务失败，避免级联故障，以提高分布式系统的弹性。**



“断路器” 本身是一种开关装置，当某个服务单元发生故障之后，通过断路器的故障监控（类似熔断保险丝），**向调用方返回一个符合预期的、可处理的备选响应(Fallback)，而不是长时间的等待或抛出调用方无法处理的异常**，这样就保证了服务调用方的线程不会被长时间、不必要地占用，从而避免了故障在分布式系统中的蔓延，乃至雪崩。

#### Hystrix能干啥？

* 服务降级
* 服务熔断
* 接近实时的监控
* .....

#### Hystrix官网资料（Github）

官网：https://github.com/Netflix/Hystrix

使用教程（老了）https://github.com/Netflix/Hystrix/wiki/How-To-Use

<font color="red">**Hystrix官宣，停更进入维护**</font>



#### Hystrix重要概念

* **服务降级（Fallback)**

  说人话：

  ​    服务器忙，请稍后再试，不让服务器等待并立刻返回一个友好提示Fallback

  哪些情况会触发服务降级：

  * 程序运行异常
  * 超时
  * 服务熔断
  * 线程池/信号量打满

  

* **服务熔断 (Break)**

  类比保险丝：

  服务达到最大访问量后，直接拒绝访问，拉闸限电，先熔断

  然后调用服务降级的方法返回友好提示。

  服务降级 => 熔断 =》 恢复调用链路

  

* **服务限流 (Flowlimit)**

​       秒杀、高并发等操作，严禁一窝蜂的拥挤，让访问线程排队，一秒钟N个，有序进行。



#### Hystrix案例

* **构建**

使用单机Eureka服务注册中心来做，把7001变成单机的就好

配置一个用到Hystrix的module，具体代码参考cloud-provider-hystrix-payment8001

* **高并发测试**

  上面的案例在非高并发下还能撑住

  **Jmeter压力测试：（我就不测了，舍不得烧电脑，这段看视频吧，第51p，大概2:50左右）**

  开启Jmeter，用20000个并发干死8001，这20000个请求都去访问那个paymentInfo_Timeout服务

  **测试参数：**

  **添加线程组，线程数200，循环次数100**

  **在线程组中添加sampler的HTTP请求去请求服务即可**

  **测试结果：**

  **导致的结果就是服务器响应不过来，访问转圈圈，或者直接CPU负载过大。**

  **而且同一个微服务里面其它接口也寄了，直接卡死。**

​      上面还只是服务**提供者8001自己测试**，假如此时外部的消费者80也来访问，那**消费者**只能干等，最终导致消费端80不满意，服务端8001直接被拖死。

​      接下来启动压测的时候让80消费者去访问，就会发现直接卡死了，要么转圈圈等待，要么报错，因为处理不过来了。代码写好了，压测想做的大家自己去用jmeter去搞2333。



​      <font color="red">**故障现象和导致原因：**</font> 

​      <font color="red">**8001同一层次的其它接口服务被困死，因为tomcat线程池里面的工作线程已经被挤占完毕。80此时调用8001，客户端访问响应缓慢，转圈圈。**</font> 

* **上述结论**

  正因为有上述故障或不佳表现，才有降级、容错、限流等技术诞生。

* **如何解决？**

  *  超时导致服务器变慢（转圈）

  ​        解决： 超时不再等待

  *  出错（宕机或程序运行出错）

  ​        解决： 出错要有兜底

  **解决办法：**

  1. 提供者（8001）服务超时了，调用者（80）不能一直卡死等待，必须有服务降级。
  2. 提供者（8001）服务宕机了，调用者（80）不能一直卡死等待，必须有服务降级。
  3. 提供者（8001）服务OK，调用者（80）自己出故障或有自我要求（自己的等待时间小于服务提供者处理业务的时间），自己处理降级。

  ##### Hystrix服务降级配置

**服务降级一般在客户端配置**

使用注解@HystrixCommand

这里也对8001设置自身调用超时时间的峰值，峰值以内正常运行，超过峰值就需要做服务降级

~~~java
//这个方法一旦出事，就自动调用fallbackMethod,这里规定延时3秒钟以内算正常
    @HystrixCommand(fallbackMethod = "paymentInfo_TimeOutHandler",commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value="3000")
    })
    public String paymentInfo_TimeOut(Integer id){
        int timeNumber = 5;
        try{
            TimeUnit.SECONDS.sleep(timeNumber);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return "线程池： " + Thread.currentThread().getName() + "   paymentInfo_Timeout,id:  " + id + "\t" + "O(∩_∩)O哈哈~ 耗时(秒)：" + timeNumber;
    }

    public String paymentInfo_TimeOutHandler(Integer id){
        return "线程池： " + Thread.currentThread().getName() + "   paymentInfo_TimeOutHandler,id:  " + id + "\t" + "卧槽，寄了o(╥﹏╥)o";
    }
}

~~~

然后需要在主启动类里面去激活：

~~~java
@SpringBootApplication
@EnableEurekaClient
//@EnableCircuitBreaker 激活Hystrix相关功能（服务熔断，服务降级）
@EnableCircuitBreaker
public class PaymentHystrixMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class, args);
    }
}
~~~



这里设置两种异常方式：

1. 上面的代码是只接受服务运行时间峰值是3秒，但是运行5秒触发服务降级
2. 让程序出错，int age = 10/0;

这两种方式都会让当前服务不可用，触发服务降级，兜底的方法都是**paymentInfo_TimeOutHandler**



**feign-hystrix80消费客户端对自己进行服务降级：**

先在application.yml里面对OpenFeign进行超时控制，注意，这里要和Hystrix的延时一致：

~~~yaml
feign:
  # 开启hystrix支持
  hystrix:
    enabled: true
  client:
    config:
      default:
        # 设置feign客户端超时时间
        # 指的是建立连接后从服务器读取到可用资源所用的时间，单位：毫秒
        read-timeout: 1500
        # 指的是建立连接所用的时间，适用于网络状况正常的情况下，两端连接所用的时间
        connect-timeout: 1500

~~~

主启动类加入@EnableHystrix

~~~java
@SpringBootApplication
@EnableFeignClients
@EnableHystrix
public class OrderHystrixMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderHystrixMain80.class, args);
    }
}

~~~

OrderHystrixController里面改装paymentInfo_TimeOut函数：

~~~java

    @GetMapping(value = "/consumer/payment/hystrix/timeout/{id}")
    //这个方法一旦出事，就自动调用fallbackMethod
    @HystrixCommand(fallbackMethod = "paymentTimeoutFallbackMethod",commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value="1500")
    })
    public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
        return paymentHystrixService.paymentInfo_TimeOut(id);
    }
    public String paymentTimeoutFallbackMethod(@PathVariable("id") Integer id){
        return "我是消费者80，对方支付系统繁忙请10秒钟后再试 或 自己运行出错，请检查自己o(╥﹏╥)o";
    }
~~~

#### Hystrix服务降级

目前问题：

* 每个业务方法对应一个兜底的方法，代码膨胀
* 兜底的方法和业务逻辑混在一起，代码混乱（耦合）

##### 未来可能遇到的异常

* 运行异常（程序报错）
* 超时
* 宕机

##### Hystrix 全局服务降级：解决代码膨胀

使用**Hystrix全局服务降级**:  @DefaultProperties(defaultFallback = "函数名")

上面的配置是 1对1 ，每个方法配置一个服务降级方法，技术上可以，实际上傻X

这里采用的方法是 1对N ：除了个别重要核心业务有专属，其它普通的可以通过@DefaultProperties(defaultFallback = "函数名") 统一跳转到同一个处理结果的接口。

**通用的和独享的各自分开，避免了代码膨胀，合理减少了代码量，O(∩_∩)O哈哈~**

​      **步骤：**

  1. 在hystrix80客户端controller添加全局兜底fallback函数

     ~~~java
      //下面是全局兜底fallback方法
         public String paymentGlobalFallbackMethod(){
             return "全局Global异常处理信息，请稍后再试。 o(╥﹏╥)o";
         }
     ~~~

  2.  在整个controller添加注解，配置全局兜底

    ~~~java
    @RestController
    @DefaultProperties(defaultFallback = "paymentGlobalFallbackMethod")
    public class OrderHystrixController {
        ...
    }
    ~~~

3.  如果你需要兜底的方法只写了@HystrixCommand注解，而没有单独配置里面的属性，则会使用全局兜底的函数

   ~~~java
       //如果出错，就跑到全局的兜底函数那
       @HystrixCommand
       public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
           return paymentHystrixService.paymentInfo_TimeOut(id);
       }
   ~~~



##### Hystrix通配服务降级：解决代码耦合

如果一个一个对方法进行服务降级进行注解，会导致耦合度过高。

这里设计一个案例，客户端去调用服务端，碰上服务端突然宕机或者关闭。

本次案例服务降级是在**客户端80实现完成的**，与服务端8001没有关系，**只需要为Feign客户端定义的接口添加一个服务降级处理的实现类即可实现解耦。**



这里的案例模拟宕机，在通过客户端80访问服务端8001的时候，关闭8001，然后80端刷新就触发了兜底。

##### 如何配置通配服务降级

采用实现接口的方式，因为在OpenFeign中，客户端是直接使用接口去调用服务端的，所以通过实现这个接口，重写里面的方法，这样在8001寄掉的时候，接口就会访问你的实现类的方法，实现通配兜底。

用一个类来实现接口，它就是通配的兜底类，所有方法的Fallback全部在这里面定义

~~~java
@Component
public class PaymentFallbackService implements PaymentHystrixService{
    @Override
    public String paymentInfo_OK(Integer id) {
        return "PaymentFallbackService兜底类 的 paymentInfo_OK 方法";
    }

    @Override
    public String paymentInfo_TimeOut(Integer id) {
        return "PaymentFallbackService兜底类 的 paymentInfo_TimeOut 方法";
    }
}

~~~

这样就实现了通配服务降级，而且还解除了耦合。



#### Hystrix服务熔断

##### 熔断机制概述

熔断机制是应对雪崩效应的一种微服务链路保护机制。当扇出链路的某个微服务出错不可用或者响应时间太长时，会进行服务的降级，进而熔断该节点微服务的调用，快速返回错误的响应信息。

**当检测到该节点微服务调用响应正常后，恢复调用链路。**



在Spring Cloud框架里，熔断机制通过Hystrix实现。Hystrix会监控微服务间调用的状况。

当失败的调用到一定阈值，缺省是5秒内20次调用失败，就会启动熔断机制。

**熔断机制的注解是@HystrixCommand**



##### 服务熔断过程（重点）

1. 调用失败会触发降级，而降级会调用fallback方法
2. 但是无论如何降级的流程一定会先调用正常方法，正常方法报错或不符合条件再调用fallback方法
3. 假如单位时间内调用失败次数过多，也就是降级次数过多，则触发熔断
4. 熔断以后就会跳过正常方法直接调用fallback方法
5. 所谓“熔断后服务不可用”就是因为跳过了正常方法直接执行fallback



##### 服务熔断总结

###### 熔断状态类型

* 熔断打开状态（把“电闸”断开）：请求不再进行调用当前服务，内部设置时钟一般为MTTR (平均故障处理时间)，当打开时长达到所设的时间，则进入半熔断状态。
* 熔断关闭状态（把“电闸”闭合）：熔断关闭不会对服务进行熔断
* 熔断半开状态：部分请求根据规则调用当前服务，如果请求成功且符合规则则认为当前服务恢复正常，关闭熔断



###### 涉及到断路器的三个重要参数

~~~java
  @HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
            @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),  // 是否开启断路器
            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),  // 请求次数的峰值：如果实际请求超过了峰值，熔断器将会从关闭状态转换成打开状态
            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"), // 断路后的休眠时间窗，在这么多毫秒内如果请求错误减少，满足条件后主逻辑会恢复
            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60"), // 失败率达到多少后跳闸，注意单位是百分数
    })
    public String paymentCircuitBreaker(@PathVariable("id") Integer id){
        ......
    }
~~~

* 快照时间窗

要确定**断路器是否打开**，需要统计一些请求和错误数据，而**统计的时间范围就是快照时间窗**，默认为最近的10秒。

* 请求总数阈值

 在**快照时间窗**内，必须满足请求总数阈值才有资格熔断。默认为10，意味着在10秒内，**如果该Hystrix命令的调用次数不足20次，即使所有的请求都超时或其它原因失败，断路器都不会打开。**

* 错误百分比阈值

  当**请求总数在快照时间窗内超过了阈值**，比如发生了30次调用，如果在这30次调用中，有15次发生了超时异常，也就是超过50%的错误百分比，在默认设定50%阈值的情况下，这时候就会将断路器打开。



#### Hystrix服务限流

这块在SpringCloud Alibaba的时候，用Sentinel来学了，Hystrix现在已经几乎淘汰了



#### Hystrix图形化Dashboard搭建

除了隔离依赖服务的调用以外，Hystrix还提供了<font color="red">**准实时的调用监控（Hystrix Dashboard)**</font>，Hystrix会持续地记录所有通过Hystrix发起的请求的执行信息，并以统计报表和图形的形式展示给用户，包括每秒执行多少请求，多少成功，多少失败等。Netflix通过**hystrix-metrics-event-stream**项目实现了对以上指标的监控。SpringCloud也提供了Hystrix Dashboard的整合，对监控内容转化成可视化界面。

**方法：使用新注解，在启动类上加入@EnableHystrixDashboard**

**所有的Provider微服务提供类（8001/8002/8004）都需要监控依赖配置**



**注意几个坑：**

1. **监控者Dashboard和被监控的服务，pom中一定要添加web和actuator的starter**

~~~xml
<dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
~~~

2. **新版本Hystrix需要在主启动类PaymentHystrixMain8001中指定监控路径**

~~~java
/*
    此配置是为了服务监控而配置，与服务容错本身无关，这是springcloud升级后的坑
    ServletRegistrationBean配置是因为SpringBoot的默认路径不是"/hystrix.stream"
    只要在自己的项目里配置上下面的servlet就可以了
     */
    @Bean
    public ServletRegistrationBean getServlet(){
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }
~~~



开始监控：

9001监控8001

dashboard地址：http://localhost:9001/hystrix

监控时输入： http://localhost:8001/hystrix.stream  （按照上面servlet配置的来）

使用正确的和错误的来测试，观察monitor的变化

正确：id是正数 http://localhost:8001/payment/circuit/2

错误：id是负数 http://localhost:8001/payment/circuit/-2

![](http://124.222.43.240:2334/upload/2022-8-8$68427CtQQ6.png)



## 服务网关

如果有必要，单独去学习一下Zuul，这里主要学习Gateway。

### Gateway

#### 官网

Zuul 1.x: https://github.com/Netflix/zuul

**Gateway（版本要和SpringBoot对应上**）: https://cloud.spring.io/spring-cloud-static/spring-cloud-gateway/2.2.1.RELEASE/reference/html

#### Gateway是啥？

Cloud全家桶中，很重要的组件就是网关，在1.x版本中都是采用的Zuul网关。

但在2.x版本中，zuul的升级一直跳票，SpringCloud最后自己研发了一个网关代替Zuul

那就是**SpringCloud Gateway**。

**Gateway是Zuul 1.x版的替代。**



Gateway是在Spring生态系统之上构建的API网关服务，基于Spring 5，Spring Boot 2和Project Reactor等技术。

Gateway旨在提供一种简单而有效的方式来对API进行路由，以及提供一些强大的过滤器功能，例如：熔断，限流，重试等。 



**SpringCloud Gateway使用的Webflux中的reactor-netty响应式编程组件，底层使用了Netty通讯框架。**

#### Gateway能干啥？

* 反向代理
* 鉴权
* 流量控制
* 日志监控
* ....

#### 微服务架构中网关在哪里

![](http://124.222.43.240:2334/upload/2022-8-12$82515aAmYh.png)



#### 有Zuul了怎么又出来了Gateway?

##### 我们为什么选择Gateway

1. netflix不太靠谱，zuul2.0一直跳票

2. SpringCloud Gateway具有如下特性：

   * **基于Spring Framework 5，Project Reactor 和 Spring Boot 2.0进行构建。**

   * 动态路由：能够匹配任何请求属性。
   * 可以对路由指定 Predicate（断言）和 Filter（过滤器）
   * 集成了 SpringCloud 服务发现功能
   * 易于编写的 Predicate（断言）和 Filter（过滤器）
   * 请求限流功能
   * 支持路径重写

#### WebFlux

Gateway有一个WebFlux，**它是一个非阻塞的Web框架**。

传统的Web框架，比如struts2，SpringMVC等都是基于Servlet API与Servlet容器基础之上运行的。

**在Servlet 3.1之后有了异步非阻塞的支持。**而WebFlux是一个典型非阻塞异步的框架，它的核心是基于Reactor相关API实现的。相对于传统的Web框架来说，它可以运行在诸如Netty，Undertow及支持Servlet3.1的容器上。非阻塞式+函数式编程（Spring 5必须让你使用java8）



#### Gateway三大核心概念

* Route（路由）

  路由是构建网关的基本模块，它由ID，目标URI，一系列的断言和过滤器组成，如果断言为true则匹配该路由。

* Predicate（断言）

  参考Java8的java.util.function.Predicate

  开发人员可以匹配HTTP请求中的所有内容（例如请求头或请求参数），**如果请求与断言相匹配则进行路由**。

* Filter (过滤)

​       指的是Spring框架中GatewayFilter的实例，使用过滤器，可以在请求被路由之前或者之后对请求进行修改。

**总结**：

Web请求，通过一些匹配条件，定位到真正的服务节点。并在这个转发过程的前后，进行一些精细化控制。

predicate就是我们的匹配条件。

而Filter就可以理解为一个无所不能的拦截器。有了这两个元素，再加上目标uri，就可以实现一个具体的路由了



#### Gateway核心逻辑

路由转发 + 执行过滤器链

![](http://124.222.43.240:2334/upload/2022-8-12$417055hEMC.png)

##### 主要流程

客户端向SpringCloud Gateway发出请求。然后在Gateway Handler Mapping 中找到与请求相匹配的路由，将其发送到Gateway Web Handler。

Handler 再通过指定的过滤器链来将请求发送到我们实际的服务执行业务逻辑，然后返回。

过滤器之间用虚线分开是因为过滤器可能会在发送代理请求之前（pre）或之后（post）执行业务逻辑。

Filter在 “pre” 类型的过滤器可以做参数校验、权限校验、流量监控、日志输出、协议转换等，

在 “post” 类型的过滤器中可以做响应内容，响应头的修改，日志的输出，流量监控等有着非常重要的作用。



#### Gateway网关搭建（使用9527端口）

1. 第一种配置方式

在9527的application.yml中配置：

~~~yaml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway
  cloud:
    # Gateway网关配置
    gateway:
      routes: # 路由
        - id: payment_route # 路由的ID, 没有固定规则，但要求唯一，建议配合服务名
          uri: http://localhost:8001 # 匹配后提供服务的路由地址
          predicates: # 断言
            - Path=/payment/get/**  # 路径相匹配的进行路由
            -
        - id: payment_route2 # 路由的ID, 没有固定规则，但要求唯一，建议配合服务名
          uri: http://localhost:8001 # 匹配后提供服务的路由地址
          predicates: # 断言
            - Path=/payment/lb/**  # 路径相匹配的进行路由
eureka:
  instance:
    hostname: cloud-gateway-service
  client: # 服务提供者provider注册进eureka服务列表内
    service-url:
       register-with-eureka: true
       fetch-registry: true
       defaultZone: http://localhost:7001/eureka

~~~



注意pom依赖添加gateway的，然后不要添加web和actuator

~~~xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    <!--eureka-client-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--  引入自定义的cloud-api-commons模块，来访问实体类   -->
    <dependency>
        <groupId>com.guifeng</groupId>
        <artifactId>cloud-api-commons</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
~~~



2. 第二种配置方式：

   代码中注入RouteLocator的Bean，配合@Configuration和@Bean来进行编程式配置

   ~~~java
   @Configuration
   public class GateWayConfig {
       /*
       配置了一个id为path_route_guifeng的路由规则
       当访问地址为： http://localhost:9527/guonei时会自动转发到地址：http://news.baidu.com/guonei
        */
       @Bean
       public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder){
           RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
           // http://news.baidu.com/guonei
           routes.route("path_route_guifeng",r -> r.path("/guonei").uri("http://news.baidu.com/guonei"));
           return routes.build();
       }
   }
   
   ~~~

未完待续。。。。。


