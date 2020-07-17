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
import com.android.build.api.variant.VariantInfo
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

class ClickTraceTransform extends Transform {
    Project project;
    TransformOutputProvider output
    JavassistInjector javassistInjector

    def listOfJar = new ArrayList()
    def listOfDir = new ArrayList();

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
    boolean applyToVariant(VariantInfo variant) {
        return super.applyToVariant(variant)
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

        /**
         * 先把所有要处理的jar文件都作为javassist classpath，因为这些jar之间可能有相互
         * 依赖关系，如果不添加到classPath，可能在处理某个Jar的时候报错
         */
        listOfDir.each { File dest ->
            javassistInjector.handleDependencies(dest.path)
        }
        listOfJar.each { File dest ->
            javassistInjector.handleDependencies(dest.path)
        }

        //处理dir
        listOfDir.each { File dest ->
            dest.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
                // dest.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28
                // classFile.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28/com/daddyno1/clicktrace/MainActivity.class
                javassistInjector.handleClassFile(dest.path, classFile)
            }
        }
        //处理输出jar文件
        listOfJar.each {
            javassistInjector.handleJarFile(it)
        }
    }

    /**
     * 处理Dir的输入
     * 每一个任务都会会有 输入、输出。此时是默认输入是：/......../app/build/intermediates/javac/debug/classes
     * 这里有一个问题就是，如果我们的任务依赖了其它的任务，也许输入就不是这个路径了。如何从其中解析出 classPath 和 className，这是一个问题。
     *
     * 另外一个问题是：我们对字节码的处理应该在上一个任务的输出上直接修改字节码，处理之后拷贝一份作为自己的输出；还是应该先从上一个任务
     * 的输出拷贝一份 作为自己的输出，然后修改自己的输出内容（通常是字节码处理）。按理说后一种方式是对的，这样不会影响上一个任务的输出，
     * 否则会不会出现这种问题：很多任务都依赖同一个任务进行执行，假如某一个任务改了 那个任务的输出，那么其它任务有可能会被污染。所以根据以上
     * 思路修改自己处理逻辑。
     */
    def handleDirectory(DirectoryInput directoryInput) {
        /**
         * 第一步：首先指定自己的输出
         */
        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-DirInput: ${directoryInput.file.path}"
        //定义输出
        def dest = output.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
        //打印 ClickTraceTransform 这个任务最终的输出
        println "ClickTraceTransform-DirOutput: ${dest.path}"

        listOfDir.add(dest)

//        /**
//         * 第二步：筛选输入内容，按照规则处理自己的输出内容。
//         */
//        //过滤掉 文件夹 和 非.class文件
//        dest.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
//            // dest.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28
//            // classFile.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28/com/daddyno1/clicktrace/MainActivity.class
//
//            javassistInjector.handleClassFile(dest.path, classFile)
//        }
    }

    // 处理 jar
    def handleJar(JarInput jarInput) {
        /**
         * 第一步：首先指定自己输出
         */
        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-JarInput: ${jarInput.file.path}"

        def jarName = jarInput.name
        def md5Name = DigestUtils.md5(jarInput.file.absolutePath)
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        //定义 output  重命名输出文件（同目录copyFile会冲突）
        def dest = output.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        // 将内容 复制到 输出
        FileUtils.copyFile(jarInput.file, dest)

        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-JarOutput: ${dest.path}"

        listOfJar.add(dest);

//        javassistInjector.handleJarFile(dest)
    }
}