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



### Actuator微服务信息完善

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



### 服务发现 Discovery

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





### Eureka自我保护

保护模式主要用于一组客户端和Eureka Server之间存在网络分区场景下的保护。一旦进入保护模式，

<font color="red">Eureka Server将会尝试保护其服务注册表中的信息，不再删除服务注册表中的数据，也就是不会注销任何微服务。</font>

![](http://124.222.43.240:2334/upload/2022-7-29$73622KnycP.png)

**说人话：某时刻某一个微服务不可用了。Eureka不会立刻清理，依旧会对该微服务的信息进行保存。**

它的设计哲学就是宁可保留错误的服务注册信息，也不盲目注销任何可能健康的服务实例。

**<font color="red">简称： 好死不如赖活着</font>**



####  为啥会产生Eureka自我保护机制？

为了防止: **EurekaClient可以正常运行，但是与EurekaServer网络不通的情况**，EurekaServer**不会立刻**将EurekaClient服务剔除。



未完待续。。。。

