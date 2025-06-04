package com.coraybennett.spillway.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.coraybennett.spillway.annotation.Loggable;
import com.coraybennett.spillway.annotation.Loggable.LogLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect that implements the @Loggable annotation behavior.
 * This provides consistent logging patterns throughout the application.
 */
@Aspect
@Component
@Slf4j
@Order(10)
public class LoggingAspect {

    @Around("@annotation(loggable)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        String methodName = getMethodName(joinPoint);
        String className = getClassName(joinPoint);
        String entryMessage = getEntryMessage(joinPoint, loggable, methodName);
        String paramsInfo = loggable.includeParameters() ? getParametersInfo(joinPoint) : "";
        
        logByLevel(loggable.level(), entryMessage + paramsInfo);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            String exitMessage = getExitMessage(loggable, methodName, result, endTime - startTime);
            logByLevel(loggable.level(), exitMessage);
            return result;
        } catch (Exception e) {
            if (loggable.logExceptions()) {
                log.error("{}.{} threw exception: {}", className, methodName, e.getMessage(), e);
            }
            throw e;
        }
    }
    
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        return ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
    }
    
    private String getClassName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getTarget().getClass().getSimpleName();
    }
    
    private String getEntryMessage(ProceedingJoinPoint joinPoint, Loggable loggable, String methodName) {
        if (!loggable.entryMessage().isEmpty()) {
            return loggable.entryMessage();
        }
        return "Entering " + getClassName(joinPoint) + "." + methodName + "()";
    }
    
    private String getExitMessage(Loggable loggable, String methodName, Object result, long executionTime) {
        if (!loggable.exitMessage().isEmpty()) {
            return loggable.exitMessage();
        }
        
        StringBuilder sb = new StringBuilder("Exiting ")
            .append(methodName)
            .append("() in ")
            .append(executionTime)
            .append("ms");
            
        if (loggable.includeResult() && result != null) {
            sb.append(", result: ");
            if (result instanceof String) {
                sb.append(result);
            } else {
                sb.append(result.toString());
            }
        }
        
        return sb.toString();
    }
    
    private String getParametersInfo(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        StringBuilder sb = new StringBuilder(" with parameters: [");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramNames[i]).append("=");
            
            if (args[i] == null) {
                sb.append("null");
            } else if (args[i] instanceof String) {
                sb.append(args[i]);
            } else {
                sb.append(args[i].toString());
            }
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    private void logByLevel(LogLevel level, String message) {
        switch (level) {
            case TRACE:
                log.trace(message);
                break;
            case DEBUG:
                log.debug(message);
                break;
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            default:
                log.debug(message);
        }
    }
}