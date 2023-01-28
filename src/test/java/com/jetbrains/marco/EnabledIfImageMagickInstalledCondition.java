package com.jetbrains.marco;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

public class EnabledIfImageMagickInstalledCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findAnnotation(context.getElement(), EnabledIfImageMagickInstalled.class) //
                .map((annotation) -> (ImageMagick.detectVersion() != null)
                        ? ConditionEvaluationResult.enabled("ImageMagick installed.")
                        : ConditionEvaluationResult.disabled("No ImageMagick installation found.")) //
                .orElseGet(() -> ConditionEvaluationResult.disabled("By default, Imagemagick tests are disabled"));
    }
}
