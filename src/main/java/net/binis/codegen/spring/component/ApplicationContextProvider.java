package net.binis.codegen.spring.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static void setAppContext(ApplicationContext ctx) {
        context = ctx;
    }

    public void setApplicationContext(ApplicationContext ctx) {
        setAppContext(ctx);
    }
}