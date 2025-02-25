package org.opengroup.osdu.entitlements.v2.util;

import com.google.gson.Gson;
import java.util.Map;


public abstract class TestUtils {

    protected static String token = null;
    protected static String noDataAccesstoken = null;
    protected static String dataRootToken = null;

    private static Gson gson = new Gson();

    public abstract String getToken() throws Exception;

    public abstract String getUserId() throws Exception;

    public abstract String getNoDataAccessToken() throws Exception;

    public abstract String getDataRootToken() throws Exception;

    private static void log(String method, String url, Map<String, String> headers, String body) {
        System.out.println(String.format("%s: %s", method, url));
        System.out.println(body);
    }
}
