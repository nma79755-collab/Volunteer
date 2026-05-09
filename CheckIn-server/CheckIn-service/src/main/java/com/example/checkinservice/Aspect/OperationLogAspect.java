package com.example.checkinservice.Aspect;

import com.cyh.Utils.JwtUserDetails;
import com.cyh.entity.OperationLog;
import com.example.checkinservice.mapper.OperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Operation Log Aspect
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(operationLogAnnotation)")
    public Object recordOperationLog(ProceedingJoinPoint joinPoint,
                                     OperationLogAnnotation operationLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        int operationResult = 0;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            operationResult = 1;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            try {
                Long operatorId = getCurrentUserId();
                String operationType = operationLogAnnotation.type();
                String operationObject = operationLogAnnotation.object();
                String operationContent = getOperationContent(joinPoint, result);

                OperationLog entity = new OperationLog();
                entity.setOperatorId(operatorId);
                entity.setOperationType(operationType);
                entity.setOperationObject(operationObject);
                entity.setOperationContent(operationContent);
                entity.setOperationResult(operationResult);
                entity.setErrorMessage(errorMessage);
                entity.setCreateTime(LocalDateTime.now());
                entity.setIsDeleted(0);

                operationLogMapper.insert(entity);

                log.info("Operation logged: type={}, object={}, result={}, duration={}ms",
                        operationType, operationObject, operationResult, duration);
            } catch (Exception e) {
                log.error("Failed to record operation log", e);
            }
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtUserDetails) {
            JwtUserDetails userDetails = (JwtUserDetails) authentication.getDetails();
            return userDetails.getUserId();
        }
        return null;
    }

    private String getOperationContent(ProceedingJoinPoint joinPoint, Object result) {
        StringBuilder content = new StringBuilder();
        content.append("Method: ").append(joinPoint.getSignature().getName());

        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            content.append(", Args: ");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) content.append(", ");
                content.append(args[i]);
            }
        }

        if (result != null) {
            content.append(", Result: ").append(result);
        }

        return content.toString();
    }
}
