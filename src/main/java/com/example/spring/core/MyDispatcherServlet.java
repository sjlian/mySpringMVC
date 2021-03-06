package com.example.spring.core;

import com.example.spring.annotation.MyController;
import com.example.spring.annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * web容器对servlet仅实例化一次。该servlet生命周期伴随着整个web项目，也就是说仅存在1个，因此不用考虑线程安全问题。
 * list和map初始化大小需要根据项目中实际情况指定。
 * 如标记多少个bean就可以大致计算出需要多个个iocMap，
 * 有多少个需要扫描的类就可以大致计算出classNames大小。
 */
public class MyDispatcherServlet extends HttpServlet {
    /**
     * jdk自带，用于读取和存放配置信息。本质是Hashtable。
     */
    private Properties properties = new Properties();
    /**
     * 存放扫描路径下的所有的class文件的全路径类名。如com.example.spring.MyDispatcherServlet
     */
    private List<String> classNames = new ArrayList<>(10);
    /**
     * ioc容器。用于存放bean。
     */
    private Map<String, Object> ioc = new HashMap<>(32);
    /**
     * 存放url请求路径和处理方法的映射关系。
     */
    private Map<String, Method> handlerMapping = new HashMap<>();
    /**
     * 存放url请求路径和实例的映射关系
     */
    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件,即把application.properties读取到properties中。
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.初始化所有相关联的类,扫描用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));
        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();
        //4.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //处理请求
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    /**
     * 请求转发核心方法
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        //拼接url并把多个/替换成一个
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class returnType = method.getReturnType();
        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();

            if (requestParam.equals("HttpServletRequest")) {
                //参数类型已明确，这边强转类型
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")) {
                paramValues[i] = resp;
                continue;
            }
            if (requestParam.equals("String")) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }
        //利用反射机制来调用
        String responseResult = "";
        try {
            //obj是method所对应的实例 在ioc容器中
            Object result = method.invoke(this.controllerMap.get(url), paramValues);
            responseResult = returnType.cast(result).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        resp.getWriter().write(responseResult);
    }


    //把web.xml中的contextConfigLocation对应value值的文件加载到留里面
    private void doLoadConfig(String location) {
        if (location.startsWith("classpath:")) {
            location = location.replace("classpath:", "");
        } else if (location.contains("/")) {
            int lastSplitIndex = location.lastIndexOf('/');
            location = location.substring(lastSplitIndex + 1, location.length());
        }
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            //用Properties文件加载文件里的内容
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void doScanner(String packageName) {
        //把所有的.替换成/
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归读取包
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                //把类搞出来,反射来实例化(只有加@MyController需要实例化)
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }


    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                //处理非MyController的其他注解（requestMapping）,实际spring中有很多其他注解。
                if (!clazz.isAnnotationPresent(MyController.class)) {
                    continue;
                }
                //拼url时,是controller头的url拼上方法上的url
                String baseUrl = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();

                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    //这里应该放置实例和method
                    handlerMapping.put(url, method);
                    //避免clazz被重复实例化
                    Object tmpValue = null;
                    String ctlName = toLowerFirstWord(clazz.getSimpleName());
                    if(ioc.containsKey(ctlName)){
                        tmpValue = ioc.get(ctlName);
                    }else{
                        tmpValue = clazz.newInstance();
                    }
                    controllerMap.put(url, tmpValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 把字符串的首字母小写
     */
    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

}
