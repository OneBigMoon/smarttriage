package com.wcjk.triage.http;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class ApiManagerTest {
    @Test
    public void resetApiClearsCachedServerApi() throws Exception {
        ApiManager manager = ApiManager.getInstance();
        Field apiField = ApiManager.class.getDeclaredField("api");
        apiField.setAccessible(true);
        Object apiProxy = Proxy.newProxyInstance(
                ServerApi.class.getClassLoader(),
                new Class[]{ServerApi.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        return null;
                    }
                });

        apiField.set(manager, apiProxy);
        assertNotNull(apiField.get(manager));

        manager.resetApi();

        assertNull(apiField.get(manager));
    }
}
