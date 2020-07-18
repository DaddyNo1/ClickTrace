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

import javax.annotation.processing.Processor

class ClickTraceTransform extends ClassFullProjectTransform {

    //Project
    Project project
    // 字节码处理工具类
    JavassistInjector javassistInjector

    @Override
    String getName() {
        return "ClickTraceTransform"
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //先调用父类的transform，此时所有 directory 和 jar 的输出都已经就绪，可直接使用
        super.transform(transformInvocation)

        //初始化字节码工具类
        javassistInjector = new JavassistInjector(project)

        /**
         * 先把所有要处理的jar文件都作为javassist classpath，因为这些jar之间可能有相互
         * 依赖关系，如果不添加到classPath，可能在处理某个Jar的时候报错
         */
        outputDirs.each { File dest ->
            javassistInjector.handleDependencies(dest.path)
        }
        outputJars.each { File dest ->
            javassistInjector.handleDependencies(dest.path)
        }

        //处理dir
        outputDirs.each { File dest ->
            dest.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
                // dest.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28
                // classFile.path:  /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28/com/daddyno1/clicktrace/MainActivity.class
                javassistInjector.handleClassFile(dest.path, classFile)
            }
        }
        //处理输出jar文件
        outputJars.each { File dest ->
            javassistInjector.handleJarFile(dest)
        }
    }
}