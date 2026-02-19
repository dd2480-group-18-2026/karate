package com.intuit.karate.core;

import com.intuit.karate.TestUtils;
import com.intuit.karate.driver.Driver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioEngineStopTest {

    @Test
    void stop_shouldQuitDriver_whenOptionsStopTrue() throws Exception {
        ScenarioEngine engine = TestUtils.engine();
        engine.init();

        Object options = optionsWithStop(true);
        AtomicBoolean quit = new AtomicBoolean(false);

        set(engine, "driver", driverWith(options, quit));

        assertDoesNotThrow(() -> engine.stop(null));
        assertTrue(quit.get());
    }

    @Test
    void stop_shouldHitTargetStopBranch_whenTargetPresent() throws Exception {
        ScenarioEngine engine = TestUtils.engine();
        engine.init();

        Object options = optionsWithStop(false);
        setTargetToReturnVideo(options, "x.mp4");

        set(engine, "driver", driverWith(options, new AtomicBoolean(false)));

        assertDoesNotThrow(() -> engine.stop(null));
    }


    private static Object optionsWithStop(boolean stop) throws Exception {
        Object options = alloc(Class.forName("com.intuit.karate.driver.DriverOptions"));
        set(options, "stop", stop);
        return options;
    }

    private static void setTargetToReturnVideo(Object options, String video) throws Exception {
        Field tf = findField(options.getClass(), "target");
        tf.setAccessible(true);
        Class<?> targetType = tf.getType();

        Object target = Proxy.newProxyInstance(
                targetType.getClassLoader(),
                new Class[]{targetType},
                (p, m, a) -> {
                    if ("stop".equals(m.getName())) {
                        Map<String, Object> out = new HashMap<>();
                        out.put("video", video);
                        return out;
                    }
                    return null;
                }
        );

        tf.set(options, target);
    }

    private static Driver driverWith(Object options, AtomicBoolean quitCalled) {
        return (Driver) Proxy.newProxyInstance(
                Driver.class.getClassLoader(),
                new Class[]{Driver.class},
                (p, m, a) -> {
                    if ("getOptions".equals(m.getName())) return options;
                    if ("quit".equals(m.getName())) { quitCalled.set(true); return null; }
                    return null;
                }
        );
    }

    private static void set(Object obj, String field, Object val) throws Exception {
        Field f = findField(obj.getClass(), field);
        f.setAccessible(true);
        f.set(obj, val);
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> x = c; x != null; x = x.getSuperclass()) {
            try { return x.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T alloc(Class<T> clazz) throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method m = u.getMethod("allocateInstance", Class.class);
        return (T) m.invoke(unsafe, clazz);
    }
}
