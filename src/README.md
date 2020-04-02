0.前言

    1.该项目是练习项目，用于学习springMVC原理以及设计理念。
    
    2.由于spring和springMVC整体架构非常庞大，这里仅实现一些最简单的内容，且很多情况可能考虑不全。
    
    3.spring非常强大，我们应抱着尊敬的心态去学习和思考。
        
    5.目前注解方式是最流行的方式，因此本项目采用了注解的实现方式。
    
    6.springMVC运行servlet容器中，核心思想是DispatcherServlet，因此本项目除了jdk外还依赖了servlet-api（最新版本4.0.1）。

    7.为了简化实现，不再处理模板引擎，仅做json传输数据。
    
    8.项目采用maven方式，需要打包并运行于tomcat。
    
1.springMVC运行流程
    
    1.
    