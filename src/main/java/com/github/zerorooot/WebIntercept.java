package com.github.zerorooot;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.util.ProtoUtil;
import com.github.monkeywie.proxyee.util.ProtoUtil.RequestProto;
import com.google.gson.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

import com.github.zerorooot.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @Author: zero
 * @Date: 2020/7/3 10:00
 */
public class WebIntercept extends HttpProxyIntercept {
    private final String path;
    private final String ip;
    private final int port;
    private final String account;
    private final String password;
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024 * 8;


    public WebIntercept(String ip, int port, String path, String account, String password) {
        this.path = path;
        this.ip = ip;
        this.port = port;
        this.account = account;
        this.password = password;
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception {
        RequestProto requestProto = ProtoUtil.getRequestProto(httpRequest);
        if (requestProto == null) {
            //bad request
            clientChannel.close();
            return;
        }

        InetSocketAddress inetSocketAddress = (InetSocketAddress) clientChannel.localAddress();

        if (requestProto.getHost().equals(inetSocketAddress.getHostString()) &&
                requestProto.getPort() == inetSocketAddress.getPort()) {

            if ("/favicon.ico".equals(httpRequest.uri())) {
                return;
            }

            if ("/index.html".equals(httpRequest.uri()) || "/".equals(httpRequest.uri())) {
                String html;
                if (Objects.nonNull(httpRequest.headers().get("cookie")) && checkCookie(httpRequest.headers().get("cookie"))) {
                    html = "http://" + ip + ":" + port + "/ban/config.html";
                    redirect(clientChannel, html);
                } else {
                    html = new String(WebIntercept.class.getResourceAsStream("/html/index.html").readAllBytes());
                    response(clientChannel, html);
                }
                return;
            }

            //登录验证
            if ("/login".equals(httpRequest.uri())) {
                if (httpRequest instanceof FullHttpRequest) {
                    FullHttpRequest fullHttpRequest = (FullHttpRequest) httpRequest;
                    checkLogin(fullHttpRequest, clientChannel);
                } else if (match(httpRequest)) {
                    //重置拦截器
                    pipeline.resetBeforeHead();
                    //添加gzip解压处理
                    clientChannel.pipeline().addAfter("httpCodec", "decompress", new HttpContentDecompressor());
                    //添加Full request解码器
                    clientChannel.pipeline().addAfter("decompress", "aggregator", new HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH));
                    //重新过一遍处理器链
                    clientChannel.pipeline().fireChannelRead(httpRequest);
                }
                return;
            }

            if (Objects.nonNull(httpRequest.headers().get("cookie")) && checkCookie(httpRequest.headers().get("cookie")) && httpRequest.uri().startsWith("/ban")) {
                //输出check
                if ("/ban/check.html".equals(httpRequest.uri())) {
                    String html = new String(WebIntercept.class.getResourceAsStream("/html/check.html").readAllBytes());
                    response(clientChannel, html);
                }
                //config页面
                if ("/ban/config.html".equals(httpRequest.uri())) {
                    String html =
                            new String(WebIntercept.class.getResourceAsStream("/html/config.html").readAllBytes());
                    response(clientChannel, html);
                }

                if ("/ban/changeFile".equals(httpRequest.uri())) {
                    if (httpRequest instanceof FullHttpRequest) {
                        FullHttpRequest fullHttpRequest = (FullHttpRequest) httpRequest;
                        changeFile(fullHttpRequest, clientChannel);
                    } else if (match(httpRequest)) {
                        //重置拦截器
                        pipeline.resetBeforeHead();
                        //添加gzip解压处理
                        clientChannel.pipeline().addAfter("httpCodec", "decompress", new HttpContentDecompressor());
                        //添加Full request解码器
                        clientChannel.pipeline().addAfter("decompress", "aggregator", new HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH));
                        //重新过一遍处理器链
                        clientChannel.pipeline().fireChannelRead(httpRequest);
                    }

                }
                if ("/ban/getUrlFile".equals(httpRequest.uri())) {
                    if (httpRequest instanceof FullHttpRequest) {
                        FullHttpRequest fullHttpRequest = (FullHttpRequest) httpRequest;
                        getUrlFile(fullHttpRequest, clientChannel);
                    } else if (match(httpRequest)) {
                        //重置拦截器
                        pipeline.resetBeforeHead();
                        //添加gzip解压处理
                        clientChannel.pipeline().addAfter("httpCodec", "decompress", new HttpContentDecompressor());
                        //添加Full request解码器
                        clientChannel.pipeline().addAfter("decompress", "aggregator", new HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH));
                        //重新过一遍处理器链
                        clientChannel.pipeline().fireChannelRead(httpRequest);
                    }
                }
                //验证传入的值
                if ("/ban/CheckTest".equals(httpRequest.uri())) {
                    if (httpRequest instanceof FullHttpRequest) {
                        FullHttpRequest fullHttpRequest = (FullHttpRequest) httpRequest;
                        checkValue(fullHttpRequest, clientChannel);
                    } else if (match(httpRequest)) {
                        //重置拦截器
                        pipeline.resetBeforeHead();
                        //添加gzip解压处理
                        clientChannel.pipeline().addAfter("httpCodec", "decompress", new HttpContentDecompressor());
                        //添加Full request解码器
                        clientChannel.pipeline().addAfter("decompress", "aggregator", new HttpObjectAggregator(DEFAULT_MAX_CONTENT_LENGTH));
                        //重新过一遍处理器链
                        clientChannel.pipeline().fireChannelRead(httpRequest);
                    }
                }
                //获取开头为@的config文件内容
                if ("/ban/getConfig".equals(httpRequest.uri())) {
                    UrlList urlList = new UrlList(path + File.separator + "config.txt");
                    List<String> linkedList =
                            urlList.getFileContent().stream().filter(s -> s.startsWith("@")).map(s -> s.substring(1)).collect(Collectors.toList());

                    String data = new Gson().toJson(linkedList);

                    JsonArray jsonArray = new Gson().fromJson(data, JsonArray.class);
                    response(clientChannel, jsonArray.toString());
                }
            } else {
                redirect(clientChannel, "http://" + ip + ":" + port + "/index.html");
            }


        } else {
            pipeline.beforeRequest(clientChannel, httpRequest);

        }


    }

    /**
     * 拦截并处理响应
     */
    public void checkValue(FullHttpRequest httpRequest, Channel clientChannel) {
        String jsonContent = httpRequest.content().toString(StandardCharsets.UTF_8);
        JsonObject jsonObject = new Gson().fromJson(jsonContent, JsonObject.class);

        if (!jsonObject.has("method")) {
            response(clientChannel, "error");
            return;
        }

        if ("ban".equals(jsonObject.get("method").getAsString())) {
            String url = URLDecoder.decode(jsonObject.get("url").getAsString(), StandardCharsets.UTF_8);
            url = url.replace("http://", "").replace("https://", "");
            String checkUrl = new Serve(ip, port, path).check(url, "") + "";

            response(clientChannel, checkUrl);
        }


        if ("check".equals(jsonObject.get("method").getAsString())) {
            String json = URLDecoder.decode(jsonObject.get("json").getAsString(), StandardCharsets.UTF_8);
            String rule = URLDecoder.decode(jsonObject.get("rule").getAsString(), StandardCharsets.UTF_8);
            String[] rules = rule.split("\n");
            Properties properties = new Properties();
            for (String s : rules) {
                if (s.contains("=")) {
                    String key = s.split("=")[0];
                    String value = s.split("=")[1];
                    properties.put(key, value);
                }
            }

            String changeJson =
                    new Serve(ip, port, path).replaceContent(new Gson().fromJson(json, JsonElement.class), properties);

            response(clientChannel, changeJson);
        }


    }

    public void getUrlFile(FullHttpRequest httpRequest, Channel clientChannel) throws Exception {
        String url = httpRequest.content().toString(StandardCharsets.UTF_8);
//        url = url.replaceAll(".*\\?", "");
        String fileName = path + File.separator + url.replace(".", "").replace("/", "").replace(
                ":", "") + ".txt";
        if (!new File(fileName).exists()) {
            new File(fileName).createNewFile();
            response(clientChannel, "");
            return;
        }

        FileInputStream fileInputStream = new FileInputStream(fileName);
        String content = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        response(clientChannel, content);
    }

    public void changeFile(FullHttpRequest httpRequest, Channel clientChannel) throws Exception {
        String[] contents = httpRequest.content().toString(StandardCharsets.UTF_8).split("&");
        HashMap<String, String> hashMap = new HashMap<>(2);
        for (String s : contents) {
            hashMap.put(URLDecoder.decode(s.split("=")[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8));
        }

        String url = hashMap.get("url");
        String fileName = path + File.separator + url.replace(".", "").replace("/", "").replace(
                ":", "") + ".txt";
        String content = hashMap.get("content");
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(content.getBytes());
        response(clientChannel, "ok");
    }

    //返回
    private void response(Channel clientChannel, String html) {
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);

//        httpResponse.headers().set(HttpHeaderNames.SET_COOKIE, System.currentTimeMillis());

        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, html.getBytes().length);
        httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        HttpContent httpContent = new DefaultLastHttpContent();
        httpContent.content().writeBytes(html.getBytes());
        clientChannel.writeAndFlush(httpResponse);
        clientChannel.writeAndFlush(httpContent);
        clientChannel.close();
    }

    private void checkLogin(FullHttpRequest httpRequest, Channel clientChannel) throws Exception {
        //u=123&p=456
        String contents = URLDecoder.decode(httpRequest.content().toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        String token = contents.replace("u=", "").replace("&p", "");
//        Cookie cookie = new DefaultCookie(token[0], token[1]);

        if (token.replace("=", "").equals(account + password)) {
            String html = "http://" + ip + ":" + port + "/ban/check.html";
            redirect(clientChannel, token, html);
        } else {
            String html = "http://" + ip + ":" + port + "/index.html";
            redirect(clientChannel, html);
        }


    }

    private boolean checkCookie(String cookie) {
        if (cookie.contains(";")) {
            cookie = cookie.substring(cookie.lastIndexOf(";") + 1).strip();
        }
        return cookie.equals(account + "=" + password);

    }

    private static void redirect(Channel clientChannel, String cookie, String path) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);

        response.headers().set(HttpHeaderNames.SET_COOKIE, cookie);
        response.headers().set(HttpHeaderNames.LOCATION, path);
        HttpContent content = new DefaultLastHttpContent();
        clientChannel.writeAndFlush(response);
        clientChannel.writeAndFlush(content);
        clientChannel.close();
    }

    private static void redirect(Channel clientChannel, String path) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, path);
        HttpContent content = new DefaultLastHttpContent();
        clientChannel.writeAndFlush(response);
        clientChannel.writeAndFlush(content);
        clientChannel.close();
    }

    /**
     * 匹配到的请求会解码成FullRequest
     */
    public boolean match(HttpRequest httpRequest) {
        RequestProto requestProto = ProtoUtil.getRequestProto(httpRequest);
        return ip.equals(requestProto.getHost()) && port == requestProto.getPort();
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpContent httpContent,
                              HttpProxyInterceptPipeline pipeline) throws Exception {
            pipeline.beforeRequest(clientChannel, httpContent);
    }
}
