package com.dreweaster.jester.example.domain.util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@JsonSerialize
@Value.Style(
    typeAbstract = {"Abstract*"}, // 'Abstract' prefix will be detected and trimmed
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    build = "create", // rename 'build' method on builder to 'create'
    visibility = ImplementationVisibility.PUBLIC) // Generated class will be always public
public @interface DomainStyle {}