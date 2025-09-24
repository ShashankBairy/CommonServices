package com.common.config;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ExceptionLoggerAspect {

	@AfterThrowing(pointcut = "execution(* com.aop.aopController..*(..))", throwing = "ex")
    public void logAfterThrowing(Exception ex) {
		
		StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("***Back Trace is  ::: "+element.toString()+"**** Your Error is : "+ex.getMessage()).append("\n");
        }
        log.error(sb.toString());
		
    }
}
