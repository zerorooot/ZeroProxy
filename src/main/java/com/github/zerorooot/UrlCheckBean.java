package com.github.zerorooot;

/**
 * @Author: zero
 * @Date: 2020/8/28 12:37
 */
public class UrlCheckBean {
    private boolean check;
    private String url;

    public UrlCheckBean(boolean check, String url) {
        this.check = check;
        this.url = url;
    }

    public UrlCheckBean() {
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "UrlCheckBean{" +
                "check=" + check +
                ", url='" + url + '\'' +
                '}';
    }
}
