package com.github.zerorooot;

/**
 * @Author: zero
 * @Date: 2020/7/23 19:05
 */
public class Start {
    public static void main(String[] args) {
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String path = args[2];
        Serve serve = new Serve(ip, port, path);

        if (args.length == 5) {
            String account = args[3];
            String password = args[4];
            System.out.println("开启 web 控制");
            System.out.println("http://" + ip + ":" + port + "/index.html");
            serve.getHttpProxyServer(account, password).start(ip, port);
        }


        System.out.println("关闭 web 控制");
        serve.getHttpProxyServer().start(ip, port);

    }
}
