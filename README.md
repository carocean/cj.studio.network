
# network-node 消息中间件
专门为高并交易开发的中间件。为什么不使用第三方中间件？因为网络节点级联不自由，该中间件可以很好的实现反应器架构。第三方可以通过开发插件来实现自己的业务逻辑。插件可拦截消息的生产并经过处理后很方便的将处理结果发到网络的订阅者。
因此，它并不像传统的MQ服务器那样仅仅是转发消息。
能力列表：
- 负载均衡
- 自回馈网络，单播网络，广播网络
- 命令行客户端远程管理和使用
- 多级网络节点间互连和灾备
- 基于nio的中间件，吞吐高
- 增加路由自动记忆路径的能力

用法：
第一步是先会使用工具；第二步是如何使用network开发应用插件。
- 在cmdtools中有两个命令行工具
* network是节点服务器
* peer是客户端管理工具
-- network
* 启动network.sh
* 配置，conf/node.yaml
```yaml
server:
  #服务地址。目前支持的协议有：tcp,ws,wss
  host: tcp://localhost:6600
  props:
    workThreadCount: 4
    bossThreadCount: 1
    #单位为秒，如果大于0为开启心跳，而客户端未开启心跳的话，服务器将在重启overtimes次后自动关闭客户端。一般服务器开10秒，客户端开5秒，如果服务端开客户端不开则客户端在被闲置时很快掉线
    heartbeat: 10
    #检测到心跳包最大掉包次数，超过此数则主动关闭远程客户端，如果为0表示不会主动关闭
    overtimes: 10
reactor:
  #工作线程数，0为系统自动默认分配
  workThreadCount: 10
  #队列大小
  queueCapacity: 10000

#网络有个特点：除主网络外，发送给工作网络的侦默认不会被返回自身，除非指定网络类型为feedbackcast类型
networks:
  #是否按请求地址上下文自动创建网络
  isAutoCreate: false
  #主网络，用于管理工作网络，主网络固定为feedbackcast模式
  master: master-network
  #工作网络：传播模式有：feedbackcast,unicast,multicast
  works:
   -
     name: network-1
     castmode: unicast
   -
     name: network-2
     castmode: multicast
```
* 配置应用：app/conf/rbac.yaml
```yaml
#是否开启rbac安全控制，默认为false。注：自定义认证插件也使用此开关，如果为false认证插件不可用
enable: true
roles:
  - administrators
  - developers
  - tests
users:
  -
    name: cj
    pwd: 11
    roles:
      - administrators
  -
    name: tom
    pwd: 22
    roles:
      - developers
acl:
#格式：角色名 网络名.请求指令 权限 除非 网络名.请求指令；网络名.请求指令；网络名.请求指令；
#其中$root代表主网络名,!root代表所有工作网络，请求指令可以使用通配符*表示所有网络，权限为deny或allow
#在没有显式配置时默认权限为全部资源deny，除非显式指定为allow，但在显式的配置中deny优先级最高
#网络名支持通配符*，关键字：$root是主网，!root是所有工作网
#请求指令支持通配符*
#注意：由于$root和!root表示的资源具有逻辑非关系，因此对于同一角色不可用之配多行ACE，因此对于同一个资源必须授权一行给一个角色，且仅写一行（可使用except关键字来排除），而同一资源可授权给不同角色
  - administrators *.* allow
  - developers $root.* deny except $root.listenNetwork;$root.auth;$root.listNetwork;!root.*
```
* 配置应用的订阅网络：app/conf/subscribers.yaml
```yaml
#多节点订阅
#如果向列表中节点发送消息所使用的负载均衡器，cluster的值有：none,unorientor,orientor。none表示不向列表中节点发送消息，但列表中的订阅有效。默认为unorientor表示使用一致性哈希算法
balance: orientor
#如果balance不是none则可以指定一致性哈希的虚拟节点数
vNodeCount: 10
nodes:
 -
   enable: true
   #端点名，注意在互联的订阅网络节点中不要重名
   peername: node-1
   #对端主网络名
   masterNetworkName: master-network
   #用户名
   user: cj
   #认证方式
   authmode: auth.password
   #根据对端节点的认证模式输入相应令牌
   token: 11
   #负载的地址
   nodeAddress: tcp://localhost:6601?heartbeat=2
   #订阅的网络列表
   subscribeNetworks:
     -
       network: network-2
       #将收到的远程订阅消息分发到本节点的哪些网络
       castToLocals:
         - network-2
         - network-1

```

- 启动peer,启动peer.sh，-m参数可以看帮助
```
sh peer.sh -g master-network -u cj -p 11 -r tcp://localhos6600?heartbeat=5 -m
usage: network node
 -a,--authmode <arg>
                       [可省略，默认为auth.password]认证方式，包括：auth.password,auth.jw
                       t
 -d,--debug <arg>      调试命令行程序集时使用，需指定以下jar包所在目录
                       cj.studio.network.console
 -g,--mnn <arg>        [必须]管理网络名字
 -m,--man              帮助
 -n,--peername <arg>   [可省略，默认以guid生成]本地peer名
 -p,--pwd <arg>        [必须]密码或令牌可为空
 -r,--url <arg>
                       [必须]远程node的url地址，格式：'protocol://host:port?workThrea
                       dCount=2&prop2=yy'。注意：如果含有&符则必须加单引号将整个url包住
 -u,--user <arg>       [必须]用户名
```
- 插件开发
* 有两类插件：一类是auth插件，用于自定义network的认证逻辑；二是业务插件：拦截network请求，进行业务处理。
* 可参见plugin.auth项目和plugin.example项目。