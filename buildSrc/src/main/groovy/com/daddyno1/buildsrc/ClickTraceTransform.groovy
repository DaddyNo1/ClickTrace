package com.daddyno1.buildsrc

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

class ClickTraceTransform extends Transform{
    Project project;
    TransformOutputProvider output
    JavassistInjector javassistInjector

    @Override
    String getName() {
        return "ClickTraceTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        javassistInjector = new JavassistInjector(project)

        //输入
        def inputs = transformInvocation.inputs;
        //输出管理类
        output = transformInvocation.getOutputProvider()

        //遍历输入
        inputs.each { TransformInput transformInput ->
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectory(directoryInput)
            }
            transformInput.jarInputs.each { JarInput jarInput ->
                handleJar(jarInput)
            }
        }
    }

    //处理 directory
    def handleDirectory(DirectoryInput directoryInput){
//        /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/javac/debug/classes

        //过滤掉 文件夹 和 非.class文件
        directoryInput.file.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
            javassistInjector.handleClassFile(classFile)
        }

        //定义输出
        def dest = output.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
    }

    // 处理 jar
    def handleJar(JarInput jarInput){
        def jarName = jarInput.name
        def md5Name =  DigestUtils.md5(jarInput.file.absolutePath)
        if(jarName.endsWith(".jar")){
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        //定义 output  重命名输出文件（同目录copyFile会冲突）
        def dest = output.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        // 将内容 复制到 输出
        FileUtils.copyFile(jarInput.file, dest)
    }
}