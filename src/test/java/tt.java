import com.github.zerorooot.Serve;

import java.io.IOException;

/**
 * @Author: zero
 * @Date: 2020/7/23 19:29
 */
public class tt {
    public static void main(String[] args) throws IOException {
        String ip = "192.168.123.34";
        int port = 9999;
        String path = "/home/ubuntu/Code/Java/ZeroProxy/src/test/config";
        String account = "admin";
        String password = "12345";
        System.out.println("http://"+ip+":"+port+"/index.html");
        Serve serve = new Serve(ip, port, path);
        serve.getHttpProxyServer(account, password).start(ip, port);

    }
}
