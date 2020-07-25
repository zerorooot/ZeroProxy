package com.github.zerorooot;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

/**
 * @Author: zero
 * @Date: 2020/6/17 20:54
 */
public class UrlList {
    private final String path;

    public UrlList(String path) {
        this.path = path;
    }

    public HashMap<String, LinkedList<String>> getWebUrlList() {
        HashMap<String, LinkedList<String>> hashMap = new HashMap<>(16);
        try {
            for (String s : getFileContent()) {
                LinkedList<String> strings = new LinkedList<>();
                String domain = s.replaceAll("/.*", "");
                if (Objects.nonNull(hashMap.get(domain))) {
                    strings = hashMap.get(domain);
                }
                strings.add(s);
                hashMap.put(domain, strings);
            }
        } catch (Exception ignored) {
        }

        return hashMap;
    }

    public LinkedList<String> getFileContent() throws IOException {
        LinkedList<String> content = new LinkedList<>();
        if (!new File(path).exists()) {
            new File(path).createNewFile();
            return content;
        }
        FileInputStream fileInputStream = new FileInputStream(path);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.equals("") && !line.startsWith("#")) {
                content.add(line);
            }
        }

        return content;
    }

}

