package com.jetbrains.marco;


import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfImageMagickInstalledCondition.class)
public @interface EnabledIfImageMagickInstalled {

}
