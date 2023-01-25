package com.jetbrains.marco;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class EnabledIfImageMagickInstalledCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        return findAnnotation(extensionContext.getElement(), EnabledIfImageMagickIsInstalled.class) //
                .map((annotation) -> (new App.ImageMagick().detectVersion() != App.ImageMagick.Version.NA)
                        ? ConditionEvaluationResult.enabled("ImageMagick installed.")
                        : ConditionEvaluationResult.disabled("No ImageMagick installation found.")) //
                .orElse(ConditionEvaluationResult.disabled("By default, Imagemagick tests are disabled"));
    }
}
