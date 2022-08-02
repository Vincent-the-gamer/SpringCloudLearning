# 在Windows Server上搭建zookeeper单机实例和模拟集群
## 作者：诡锋    B站：-诡锋丿Lavafall-

本次部署版本为3.8.0版本，下载地址http://mirrors.cnnic.cn/apache/zookeeper/

**注意！！一定要下载bin.tar.gz，否则是未编译的，会报错说找不到某个包**

1. 修改配置文件：进入apache-zookeeper-3.8.0\conf，修改zoo_sample.cfg文件为zoo.cfg.如果没有特殊需求，不需要修改配置文件，直接使用默认配置文件即可。

   以下为默认配置：

   **zoo_sample.cfg**

~~~yaml
# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial 
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between 
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
# do not use /tmp for storage, /tmp here is just 
# example sakes.
dataDir=/tmp/zookeeper
# the port at which the clients will connect
clientPort=2181
# the maximum number of client connections.
# increase this if you need to handle more clients
#maxClientCnxns=60
#
# Be sure to read the maintenance section of the 
# administrator guide before turning on autopurge.
#
# http://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_maintenance
#
# The number of snapshots to retain in dataDir
#autopurge.snapRetainCount=3
# Purge task interval in hours
# Set to "0" to disable auto purge feature
#autopurge.purgeInterval=1

## Metrics Providers
#
# https://prometheus.io Metrics Exporter
#metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider
#metricsProvider.httpPort=7000
#metricsProvider.exportJvmInfo=true
~~~

各个参数的意义：

tickTime：这个时间是作为 Zookeeper 服务器之间或客户端与服务器之间维持心跳的时间间隔，也就是每个 tickTime 时间就会发送一个心跳。
dataDir：顾名思义就是 Zookeeper 保存数据的目录，默认情况下，Zookeeper 将写数据的日志文件也保存在这个目录里。
clientPort：这个端口就是客户端连接 Zookeeper 服务器的端口，Zookeeper 会监听这个端口，接受客户端的访问请求。
initLimit:集群中的follower服务器(F)与leader服务器(L)之间初始连接时能容忍的最多心跳数（tickTime的数量）
syncLimit:集群中的follower服务器与leader服务器之间请求和应答之间能容忍的最多心跳数（tickTime的数量）。

2、单IP多节点（伪集群）：部署在同一IP，但是有多个节点，各有自己的端口

3、多IP多节点：部署在不同IP，各有自己的端口（未测试）



## 我的配置过程

### 单机实例配置

修改配置文件：

zookeeper1: 

**zoo.cfg**

**「注意」**：

1. 路径分隔符要用反斜杠 **「/」**

~~~yaml
tickTime=2000
initLimit=10
syncLimit=5

dataDir=C:/Users/Administrator/Desktop/zookeeper1/data
dataLogDir=C:/Users/Administrator/Desktop/zookeeper1/log

clientPort=2181

# 嵌入式 Jetty 服务器监听的端口。默认为 8080
admin.serverPort=7000
~~~



可以通过，双击客户端zkCli.cmd批处理命令来验证server是否启动成功。看到如下红框的内容则server启动成功

![img](https://pic.rmb.bdstatic.com/bjh/down/a346eb26be3ad3cf5e660402f35713dd.png)

### 开始搭建伪集群

集群版就相对麻烦一点了，这里的集群还是伪集群，真正实现集群的操作跟单机版的伪集群差不多，只不过是操作不同的服务器而已。步骤如下：

1. 修改配置文件：

   里面server.id=0.0.0.0:xxxx:xxxx，这个IP是填服务器的内网IP，不要填外网IP，为了模拟集群。这里填0.0.0.0就行，通过访问外网IP加端口号就可以直接访问到了
   
   zookeeper1:

~~~yaml
tickTime=2000
initLimit=10
syncLimit=5

dataDir=C:/Users/Administrator/Desktop/zookeeper1/data
dataLogDir=C:/Users/Administrator/Desktop/zookeeper1/log

clientPort=2181
# 集群配置
server.1=0.0.0.0:8000:9000
server.2=0.0.0.0:8001:9001
server.3=0.0.0.0:8002:9002
# 嵌入式 Jetty 服务器监听的端口。默认为 8080
admin.serverPort=7000
~~~

zookeeper2:

~~~yaml
tickTime=2000
initLimit=10
syncLimit=5

dataDir=C:/Users/Administrator/Desktop/zookeeper2/data
dataLogDir=C:/Users/Administrator/Desktop/zookeeper2/log
# the port at which the clients will connect
clientPort=2182
# 集群配置
server.1=0.0.0.0:8000:9000
server.2=0.0.0.0:8001:9001
server.3=0.0.0.0:8002:9002
# 嵌入式 Jetty 服务器监听的端口。默认为 8080
admin.serverPort=7001
~~~



zookeeper3:

~~~yaml
tickTime=2000
initLimit=10
syncLimit=5

dataDir=C:/Users/Administrator/Desktop/zookeeper3/data
dataLogDir=C:/Users/Administrator/Desktop/zookeeper3/log

clientPort=2183
# 集群配置
server.1=0.0.0.0:8000:9000
server.2=0.0.0.0:8001:9001
server.3=0.0.0.0:8002:9002
# 嵌入式 Jetty 服务器监听的端口。默认为 8080
admin.serverPort=7002
~~~



2. 添加myid文件:

   myid文件代表着该zookeeper服务器的id文件，内容也很简单，就是一个名为myid的文件，文件内容就只有一个上面所说的id。在Windows系统中，创建一个名为myid的文本文件，然后输入一个id，最后删除文件后缀，不要任何后缀，否则会启动会报myid找不到异常。

   **myid文件所在的路径就是配置文件中dataDir指定的路径下。内容就是server.xxx的这个xxx**

3. 运行至少两个实例，就可以发现选举出了Leader和Follower，那个no further information的报错不用理，就是你有实例没有启动。这一步就说明集群配置成功。

