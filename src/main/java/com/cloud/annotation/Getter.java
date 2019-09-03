package com.cloud.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bin.yin
 * @version 1.0
 * @createTime 2019/8/30 22:29
 * @since 1.0
 * @discrib 自定义Getter注解
 */
//表示此注解用在类上或接口上面
@Target(ElementType.TYPE)
//表示此注解只在编译器存在
@Retention(value = RetentionPolicy.SOURCE)
public @interface Getter {
}
