package com.jonesoft.study.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention
annotation class AutoToString(val includeSuper: Boolean = true)
