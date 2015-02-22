package com.example.spring.aop.monitor;

import com.google.common.base.Stopwatch;
import org.springframework.stereotype.Component;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by pzhong1 on 2/22/15.
 */

@Aspect
@Component
public class HelloWorldControllerAspect {
    @Before("helloWorldPointcut(name)")
    public void helloWorldPointcutBefore(String name){
        System.out.println("in aop before");
    }

    @After("helloWorldPointcut(name)")
    public void helloWorldPointcutAfter(String name){
        System.out.println("in aop after");
    }

    @Pointcut("execution(* com.example.spring.controller.*.sayHello(*)) && args(name)")
    private void helloWorldPointcut(String name){}

    @Around("helloWorldPointcut(name)")
    public Object profile(ProceedingJoinPoint call, String name) throws Throwable {
        Stopwatch clock = Stopwatch.createStarted();
        try {
            return call.proceed();
        } finally {
            clock.stop();
            long millis = clock.elapsed(MILLISECONDS);
            String message = String.format("[apiResponseTime=>%s] ms for name: %s", millis, name);
            System.out.println(message);
        }
    }
}
