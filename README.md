# Jmeter扩展包种子项目

## 描述
* 此项目为扩展Jmeter的java请求，具体实现是发送mq请求，但是未经与MQ实际交互测试

## 简介
### 开发
* ```setupTest``` 为整个测试执行时的初始化操作，而非每一个线程都会执行，整个测试执行一次
* ```teardownTest``` 测试执行结束后触发
* ```runTest```真正执行测试的方法
* ```getDefaultParameters``` 获取用户传入的参数，如果为${__threadNum}则可以调用到Jmeter自带方法
* 若需要直接执行此项目，而非打包成jar包后用Jmeter执行，则需要加入main方法

## 构建
* 本项目使用maven为构建工具，打包后在target目录下生成jar包，放入$JMETER_HOME/lib/ext中即可

## ref
* http://blog.csdn.net/musen518/article/details/50237153
* http://www.xmeter.net/wordpress/?p=87