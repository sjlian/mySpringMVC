0.前言

    1.该项目是练习项目，用于学习springMVC原理以及设计理念。
    
    2.由于spring和springMVC整体架构非常庞大，这里仅实现一些最简单的内容，且很多情况可能考虑不全。
    
    3.spring非常强大，我们应抱着尊敬的心态去学习和思考。
        
    5.目前注解方式是最流行的方式，因此本项目采用了注解的实现方式。
    
    6.springMVC运行servlet容器中，核心思想是DispatcherServlet，因此本项目除了jdk外还依赖了servlet-api（最新版本4.0.1）。

    7.为了简化实现，不再处理模板引擎，仅做json传输数据。
    
    8.项目采用maven方式，需要打包并运行于tomcat。
    
1.springMVC主要组件

    1.前端控制器DispatcherServlet：
        根据名字可以判断出该组件首先是1个servlet，以servlet规范为基础。然后该组件的目的是转发请求。
        使用springmvc需要在web.xml中配置该servlet，并设置自启动。然后把所有前端请求都由该servlet处理。
        
    2.处理器映射器HandlerMapping：
        该接口仅有1个方法：HandlerExecutionChain getHandler(HttpServletRequest request)
        HandlerMapping负责根据用户请求url找到Handler即处理器，springmvc提供了不同的映射器实现不同的映射方式，配置文件，实现接口，注解等。
        HandlerExecutionChain里包含Handler，同时还有一些interceptors。这里可以参考设计模式责任链模式：交给我后能处理我就处理，不能处理我就交给下家处理。
        
    3.处理器适配器HandlerAdapter：
        主要用于适配以下2者的不一致问题。
            SpringMVC中的Handler可以是任意的形式（指的是实现方式、请求参数等），任意形式是为了方便开发者使用。
            Servlet需要的处理方法的结构却是固定的，都是以request和response为参数的方法。固定是为了使用规范。
        DispatcherServlet通过HandlerAdapter来执行Handler      

    
    4.处理器Handler
        实际业务处理的代码，由用户编写。实现的Controller接口或@Controller注解的类都属于Handler。处理完成后返回ModelAndView或json或字符串。
   
   
    5.视图解析器ViewResolver
        根据视图名称，进行查找，生成视图对象。据逻辑视图名（视图名称）解析成物理视图名（页面地址）
        
    6.视图View
        接口，用于渲染视图。即最终的经过渲染的前端页面。
        

2.springMVC请求流程


    1.启动工程，加载web.xml或者以其他方式加载servlet时，配置在其中的DispatcherServlet被启用并初始化（初始化过程比较复杂，在下1部分详细说明）。
    
    2.用户进行http请求时，被DispatcherServlet拦截，然后交由DispatcherServlet处理（本质上是servlet，因此会交给get/post去执行）。
    
    3.DispatcherServlet的service方法调用,并最终转到doDispatch方法中。
    
    4.DispatcherServlet首先通过HandlerMapping和请求的url匹配到对应的HandlerExecutionChain,也就是找到了Handler。
    
    5.DispatcherServlet把获取到的handler转化成HandlerAdapter。
    
    6.HandlerAdapter调用handle方法，即Controller方法，返回ModelAndView。在执行handle前后分别执行mappedHandler
    
    7.DispatcherServlet通过processDispatchResult解析视图。执行applyPostHandle方法，将mv写进response返给请求用户。
    
    

3.springMVC启动流程

    
    使用过springMVC的同学都知道，还需要加一个ContextLoaderListener，该listener的作用主要是用来实现springMVC容器，和spring搭配起来是子容器。
    springMVC在spring环境启动过程比较复杂，需要父子容器的初始化。这里放在下一个项目中详细介绍。这里大致介绍一下。
    
    Web容器部分：StandardContext -> 创建ServletContext -> findApplicationListeners -> setApplicationLifecycleListeners -> contextInitialized
    spring部分：ContextLoaderListener -> initWebApplicationContext -> refresh -> webApplicationContext添加到ServletContext、
    Web容器部分：createStandWrapper->LoadServlet -> 反射实例化并init ->
    spring部分：DispatcherServlet -> initWebApplicationContext -> refresh  -> initStrategies
    
    
4.本例实现简单说明。

    
    1.定义核心DispatcherServlet。
    
    2.初始化DispatcherServlet。
        2.1 加载配置文件,即把application.properties读取到properties中，这里以扫描路径距离。
        2.2 初始化所有相关联的类,扫描用户设定的包下面所有的类
                doScanner：把扫描路径定义的类全路径名放到classNames中。
        2.3 拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写。
                doInstance：把标记bean注解的类（类名-类实例）放到ioc容器中Map。
        2.4 初始化HandlerMapping(将url和method对应上)
                initHandlerMapping：将请求url和handler中的method对应起来。
    
    3.DispatcherServlet接收用户请求到doDispatch方法中，并处理请求。
        3.1 根据请求的url找到对应的处理方法method。
        3.2 根据request填充请求参数。
        3.3 利用反射机制，从容器中获取实例，然后调用具体方法。
        3.4 将返回值写入到response中。
    
    