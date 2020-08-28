package com.github.zerorooot;

import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle;
import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.intercept.common.FullResponseIntercept;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import com.github.monkeywie.proxyee.util.ProtoUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.util.*;

/**
 * @Author: zero
 * @Date: 2020/7/23 18:56
 */
public class Serve {
    private String path;
    private String ip;
    private int port;

    public Serve(String ip, int port, String path) {
        this.path = path;
        this.ip = ip;
        this.port = port;
    }

    /**
     * 拦截网站
     *
     * @param
     * @return
     */
    public HttpProxyServer getHttpProxyServer() {
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        config.setHandleSsl(true);

        HttpProxyServer httpProxyServer = new HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        //ban url
                        pipeline.addLast(getHttpProxyIntercept());
                        //change json
                        pipeline.addLast(getFullResponseIntercept());
                    }

                }).httpProxyExceptionHandle(getHttpProxyExceptionHandle());

        return httpProxyServer;
    }

    public HttpProxyServer getHttpProxyServer(String account, String password) {
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        config.setHandleSsl(true);
        WebIntercept webIntercept = new WebIntercept(ip, port, path, account, password);
        HttpProxyServer httpProxyServer = new HttpProxyServer()
                .serverConfig(config)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline) {
                        //ban url
                        pipeline.addLast(getHttpProxyIntercept());
                        //change json
                        pipeline.addLast(getFullResponseIntercept());
                        //web
                        pipeline.addLast(webIntercept);
                    }

                }).httpProxyExceptionHandle(getHttpProxyExceptionHandle());

        return httpProxyServer;
    }


    private HttpProxyIntercept getHttpProxyIntercept() {
        HttpProxyIntercept httpProxyIntercept = new HttpProxyIntercept() {
            @Override
            public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
                                      HttpProxyInterceptPipeline pipeline) throws Exception {
                if (check(httpRequest, "")) {
                    blockUrl(clientChannel);
                }
                //转到下一个拦截器处理
                pipeline.beforeRequest(clientChannel, httpRequest);
            }
        };
        return httpProxyIntercept;
    }


    /**
     * 检测网站是否在config文件内
     *
     * @param httpRequest
     * @param replace     用于替换返回值
     * @return
     */
    private boolean check(HttpRequest httpRequest, String replace) {
//        System.out.println(url);
        String originalUrl = getOriginalUrl(httpRequest);
        return check(originalUrl, replace).isCheck();
    }

    public UrlCheckBean check(String urls, String replace) {
        UrlCheckBean urlCheckBean = new UrlCheckBean();
        String url = replace + urls.replaceAll("\\?.*", "");
        HashMap<String, LinkedList<String>> webUrlListMap =
                new UrlList(path + File.separator + "config.txt").getWebUrlList();
        String domain;
        if (url.contains("/")) {
            domain = url.substring(0, url.indexOf("/"));
        } else {
            domain = url;
        }


        if (Objects.nonNull(webUrlListMap.get(domain))) {
            for (String s : webUrlListMap.get(domain)) {
                if (url.matches(s)) {
                    //要替换的url不输出，仅输出被ban的
                    if (!"@".equals(replace)) {
                        System.out.println(LocalTime.now().toString() + " ban : " + urls);
                    }
                    urlCheckBean.setCheck(true);
                    urlCheckBean.setUrl(s);
                    return urlCheckBean;
                }
            }
        }
        urlCheckBean.setCheck(false);
        urlCheckBean.setUrl(url);
        return urlCheckBean;
    }

    private String getOriginalUrl(HttpRequest httpRequest) {
        ProtoUtil.RequestProto requestProto = ProtoUtil.getRequestProto(httpRequest);
        String webPort = "";
        assert requestProto != null;
        if (!(requestProto.getPort() == 80 || requestProto.getPort() == 433)) {
            webPort = ":" + requestProto.getPort();
        }
        String url = requestProto.getHost() + webPort + httpRequest.uri();
        return url;
    }

    /**
     * 替换返回内容
     *
     * @return 替换后的内容
     */
    private FullResponseIntercept getFullResponseIntercept() {
        FullResponseIntercept fullResponseIntercept = new FullResponseIntercept() {
            @Override
            public boolean match(HttpRequest httpRequest, HttpResponse httpResponse, HttpProxyInterceptPipeline httpProxyInterceptPipeline) {
                return check(httpRequest, "@");
            }

            @Override
            public void handelResponse(HttpRequest httpRequest, FullHttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) {
                String origin = httpResponse.content().toString(Charset.defaultCharset());
                String orginalUrl = getOriginalUrl(httpRequest);
                String url = check(orginalUrl, "@").getUrl().replace("@", "");

                String fileName = path + File.separator + url.replace(".", "").replace("/", "").replace(
                        ":", "") + ".txt";

                LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
                try {
                    if (!new File(fileName).exists()) {
                        System.out.println("没找到 " + fileName + " 创建中。。。。");
                        new File(fileName).createNewFile();
                    }
                    //转成utf-8
                    InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            linkedHashMap.put(line.split("=")[0], line.split("=")[1]);
                        }
                    }
                    bufferedReader.close();
                    inputStreamReader.close();
                    System.out.println(linkedHashMap.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }

                String replace = replaceContent(new Gson().fromJson(origin, JsonElement.class),
                        linkedHashMap);

                httpResponse.content().clear();
                httpResponse.content().writeBytes(replace.getBytes());

                System.gc();
            }
        };
        return fullResponseIntercept;
    }

    /**
     * 替换json里的内容
     *
     * @param jsonElement   json
     * @param linkedHashMap 要替换的东西
     * @return 替换后的json
     */
    public String replaceContent(JsonElement jsonElement, LinkedHashMap<String, String> linkedHashMap) {
        for (String name : linkedHashMap.keySet()) {
            String value = linkedHashMap.get(name);
            if (name.contains(".")) {
                jsonElement = singleReplaceContent(jsonElement, name, value);
            } else {
                //不含 "." ，证明是单个
                if (!name.contains("@")) {
                    //不含 @，证明不是if删除模式
                    try {
                        int key = Integer.parseInt(name);
                        System.out.println(LocalTime.now().toString() + "   " + jsonElement.getAsJsonArray().get(key) + " ->" + value);
                        jsonElement.getAsJsonArray().set(key, new Gson().fromJson(value, JsonElement.class));
                    } catch (Exception e) {
//                        e.printStackTrace();
                        if (jsonElement.getAsJsonObject().get(name) != null) {
                            //判断类型
                            JsonElement typeJson = jsonElement.getAsJsonObject().get(name);
                            System.out.println(LocalTime.now().toString() + "   " + name + " : " + jsonElement.getAsJsonObject().get(name) + " ->" + value);

                            //判断value的类型
                            try {
                                typeJson.getAsJsonPrimitive();

                                if (typeJson.getAsJsonPrimitive().isString()) {
                                    jsonElement.getAsJsonObject().addProperty(name, value);
                                }

                                if (typeJson.getAsJsonPrimitive().isBoolean()) {
                                    boolean b = Boolean.parseBoolean(value);
                                    jsonElement.getAsJsonObject().addProperty(name, b);
                                }

                                if (typeJson.getAsJsonPrimitive().isNumber()) {
                                    Number number = NumberFormat.getInstance().parse(value);
                                    jsonElement.getAsJsonObject().addProperty(name, number);
                                }

                            } catch (Exception ignored) {
                                //替換整个json
                                jsonElement.getAsJsonObject().addProperty(name, value);
                            }
                        }
                    }
                } else {
                    //含 @ ，if删除模式
                    name = name.substring(0, name.lastIndexOf("@"));
                    String checkValue = value.substring(0, value.lastIndexOf("@"));
                    //读取@之前的name和value
                    try {
                        int key = Integer.parseInt(name);
                        //检测
                        if (jsonElement.getAsJsonArray().get(key) != null && jsonElement.getAsJsonArray().get(key).toString().matches(checkValue)) {
                            jsonElement = singleReplaceContent(jsonElement, value.substring(value.lastIndexOf("@") + 1),
                                    null);
                        }
                    } catch (Exception e) {
                        if (jsonElement.getAsJsonObject().get(name) != null && jsonElement.getAsJsonObject().get(name).toString().matches(checkValue)) {
                            jsonElement = singleReplaceContent(jsonElement, value.substring(value.lastIndexOf("@") + 1),
                                    null);
                        }
                    }

                }
            }
        }
        return jsonElement.toString();
    }

    /**
     * 替换单个json里的内容
     *
     * @param jsonElement json
     * @param name        要替换json的名字
     * @param value       要替换的内容,为null表示删除name字段
     * @return 替换后的json
     */
    public JsonElement singleReplaceContent(JsonElement jsonElement, String name, String value) {
        String[] keys = name.split("\\.");
        JsonElement j = jsonElement;
        for (int i = 0; i < keys.length - 1; i++) {
            try {
                int key = Integer.parseInt(keys[i]);
                jsonElement = jsonElement.getAsJsonArray().get(key);
            } catch (Exception e) {
                try {
                    jsonElement = jsonElement.getAsJsonObject().get(keys[i]);
                } catch (Exception e1) {
                    System.err.println(LocalTime.now().toString() + "   " + "找不到 " + name + " 中的 " + keys[i] + " ,不进行替换⊙﹏⊙∥");
                    return j;
                }
            }

            //遍历到倒数第二个，再下一个就是要替换的内容
            if (i == keys.length - 2) {
                String keyString = keys[i + 1];
                //匹配到 @ ，证明要删除某内容
                if (keyString.contains("@")) {
                    keyString = keyString.substring(0, keyString.lastIndexOf("@"));
                    String checkValue = value.substring(0, value.lastIndexOf("@"));
                    try {
                        int keyInt = Integer.parseInt(keyString);
                        JsonElement keyArray = jsonElement.getAsJsonArray().get(keyInt);
                        //找到符合的key
                        if (Objects.nonNull(keyArray) && keyArray.toString().matches(checkValue)) {
                            return singleReplaceContent(j, value.substring(value.lastIndexOf("@") + 1), null);
                        } else {
                            System.err.println(LocalTime.now().toString() + "   没找到 " + keyString + "跳过，不删除");
                            return j;
                        }

                    } catch (Exception e) {
                        //蜜汁错误，输出看看效果
                        if (!e.getMessage().contains("For input string")) {
                            System.err.println(LocalTime.now().toString() + "   " + "出错啦~，联系开发者吧ヽ（≧□≦）ノ");
                            e.printStackTrace();
                        }
                        //找到符合的key
                        JsonElement keyObject = jsonElement.getAsJsonObject().get(keyString);
                        if (Objects.nonNull(keyObject) && keyObject.toString().replace("\"", "").matches(checkValue)) {
                            return singleReplaceContent(j, value.substring(value.lastIndexOf("@") + 1), null);
                        } else {
                            //没找到符合的key
                            System.err.println(LocalTime.now().toString() + "   没找到 " + keyString + "跳过，不删除");
                            return j;
                        }

//                        if (jsonElement.getAsJsonObject().get(keyString).toString().matches(checkValue)) {}

                    }
                }

                if (name.contains("@") && Objects.nonNull(value) && value.contains("@")) {
                    System.err.println(LocalTime.now().toString() + "   " + "(+_+)? 找不到匹配的 " + value + ",不进行更改");
                    return j;
                }

                //替换
                try {
                    //替换JsonArray
                    int key = Integer.parseInt(keyString);
                    //替换
                    if (Objects.nonNull(value)) {
                        //仅原字段存在时才更改
                        if (jsonElement.getAsJsonArray().get(key) != null) {
                            System.out.println(LocalTime.now().toString() + "   " + key + " : " + jsonElement.getAsJsonArray().get(key) + " ->" + value);
                            jsonElement.getAsJsonArray().set(key, new Gson().fromJson(value, JsonElement.class));
                        } else {
                            System.err.println(LocalTime.now().toString() + "   " + keyString + " 不存在，跳过~");
                        }
                    } else {
                        //删除
                        System.out.println(LocalTime.now().toString() + "   " + "remove " + jsonElement.getAsJsonArray().get(key));
                        jsonElement.getAsJsonArray().remove(key);
                    }

                } catch (Exception e) {
                    //替换JsonObject
                    if (Objects.nonNull(value)) {
                        //仅原字段存在时才更改
                        try{
                            JsonElement typeJson = jsonElement.getAsJsonObject().get(keyString);
                            System.out.println(LocalTime.now().toString() + "   " + keyString + " : " + typeJson + " ->" + value);

                            //判断value的类型
                            try {
                                typeJson.getAsJsonPrimitive();

                                if (typeJson.getAsJsonPrimitive().isString()) {
                                    jsonElement.getAsJsonObject().addProperty(keyString, value);
                                }

                                if (typeJson.getAsJsonPrimitive().isBoolean()) {
                                    boolean b = Boolean.parseBoolean(value);
                                    jsonElement.getAsJsonObject().addProperty(keyString, b);
                                }

                                if (typeJson.getAsJsonPrimitive().isNumber()) {
                                    Number number = NumberFormat.getInstance().parse(value);
                                    jsonElement.getAsJsonObject().addProperty(keyString, number);
                                }

                            } catch (Exception ignored) {
                                //替換整个json
                                jsonElement.getAsJsonObject().addProperty(keyString, value);
                            }

                        } catch (Exception e1){
                            System.err.println(LocalTime.now().toString() + "   " + keyString + "  不存在，跳过~");
                        }
                    } else {
                        //删除
                        assert jsonElement != null;
                        System.out.println(LocalTime.now().toString() + "   " + "remove  " + jsonElement.getAsJsonObject().get(keyString));
                        jsonElement.getAsJsonObject().remove(keyString);
                    }
                }


            }
        }
        /*
        执行到这里，说明用了关键词@的删除模式，且要删除的内容是顶层元素
        如 json是 {"f":[{"a":{"pic":"123"},"b":"c","d":"e"},{"a":{"pic":"a"},"b":"c","d":"e"}],"test":"true"}
        规则是 f.0.a.pic@="123"@test
         */
        if (!name.contains(".")) {
            try {
                int key = Integer.parseInt(name);
                System.out.println(LocalTime.now().toString() + "   " + "remove  " + j.getAsJsonArray().get(key));
                j.getAsJsonArray().remove(key);
            } catch (Exception e) {
                try {
                    System.out.println(LocalTime.now().toString() + "   " + "remove  " + j.getAsJsonObject().get(name));
                    j.getAsJsonObject().remove(name);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }


        return j;
    }


    /**
     * 重定向到127.0.0.1，从而实现拦截的目的
     *
     * @param clientChannel
     */
    private void blockUrl(Channel clientChannel) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, "http://127.0.0.1");
        HttpContent content = new DefaultLastHttpContent();
        clientChannel.writeAndFlush(response);
        clientChannel.writeAndFlush(content);
        clientChannel.close();
    }

    //除去报错
    private HttpProxyExceptionHandle getHttpProxyExceptionHandle() {
        HttpProxyExceptionHandle httpProxyExceptionHandle = new HttpProxyExceptionHandle() {
            @Override
            public void beforeCatch(Channel clientChannel, Throwable cause) throws Exception {
//                if (debug) {
//                    cause.printStackTrace();
//                }
                cause.getMessage();
            }

            @Override
            public void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause) throws Exception {
//                if (debug) {
//                    cause.printStackTrace();
//                }
                cause.getMessage();

            }
        };

        return httpProxyExceptionHandle;
    }

}
