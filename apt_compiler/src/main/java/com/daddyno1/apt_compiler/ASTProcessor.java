package com.daddyno1.apt_compiler;

import com.github.javaparser.StaticJavaParser;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import jdk.nashorn.internal.codegen.CompileUnit;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
@AutoService(Processor.class)
public class ASTProcessor extends AbstractProcessor {

    Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        trees = Trees.instance(processingEnvironment);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("ASTProcessor - process");


        roundEnvironment.getRootElements().stream()
                .filter(it -> ((Element) it).getKind() == ElementKind.CLASS)
                .forEach(it -> {
                    ((JCTree)trees.getTree(it)).accept(new ASTVisitor());
                });
        return false;
    }
}
