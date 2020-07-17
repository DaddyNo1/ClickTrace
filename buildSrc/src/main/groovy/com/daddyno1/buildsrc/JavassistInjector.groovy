package com.daddyno1.buildsrc

import com.android.build.gradle.AppExtension
import com.sun.istack.Nullable
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class JavassistInjector {
    private static final String CLASS_PATH_SYMBOL = "/classes/"
    private static final String CLASS_FILE = ".class"
    private static final String FILE_SEPARATOR = "/"
    private static final String DOT = "."
    public static final String ON_CLICK_LISTENER = "android.view.View\$OnClickListener"
    public static final String ON_CLICK = "onClick"
    public static final String METHOD_SIGNATURE = "(Landroid/view/View;)V"
    public static final String TAG = "==ClickTrace==";
    public static final String JAR_FILE = ".jar";
    public static final String OPT_FILE = ".opt";
    public static final String ANDROIDX = "androidx";


    Project project
    def appExtension
    ClassPool classPool = new ClassPool(ClassPool.getDefault())
    def isAndroidClassLoaded = false

    JavassistInjector(Project project) {
        this.project = project
        appExtension = project.extensions.findByType(AppExtension.class)
    }

    /**
     *  给ClassPool 添加类加载的路径。
     * @param pathName Appends a directory or a jar (or zip) file to the end of the search path.
     */
    def private appendClassPath(String pathName) {
//        println("class pool append classPath: " + pathName)

        try {
            classPool.appendClassPath(pathName)
        } catch (NotFoundException e) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化 ClassPool，从而可以访问原始的  Android sdk
     */
    def private loadAndroidClass() {
        if (!isAndroidClassLoaded) {
            // [ /Users/jxf/workspace/Android/sdk/platforms/android-29/android.jar,
            // /Users/jxf/workspace/Android/sdk/build-tools/29.0.3/core-lambda-stubs.jar ]
            List<File> bootClassPaths = appExtension.getBootClasspath();
            if (!bootClassPaths.isEmpty()) {
                for (Object bootClassPath : bootClassPaths) {
                    appendClassPath(bootClassPath.toString());
                }
            }
        }
    }

    /**
     * 添加依赖
     * @param classPath
     */
    def handleDependencies(String classPath){
        loadAndroidClass()
        appendClassPath(classPath)
    }

    /**
     * 获取class  File 的全类名
     * @param file
     * @param classPath file对应的path
     */
    def getClassFullName(String classPath, File file) {
        if (file == null || classPath == null) return
        def classNameTmp = file.path - (classPath + "/") - CLASS_FILE
        return classNameTmp.replaceAll(FILE_SEPARATOR, DOT)
    }

    def getClassFullNameFromJarEntry(JarEntry jarEntry) {
        if (jarEntry == null) return ''
        def tmp = jarEntry.name - CLASS_FILE
        tmp.replaceAll(FILE_SEPARATOR, DOT)
    }

    /**
     * 处理 .class 文件
     * @param classPath /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/transforms/ClickTraceTransform/debug/28
     * @param file file
     */
    def handleClassFile(String classPath, File file) {
        if (file == null || !file.absolutePath.endsWith(".class")) {
            return
        }

        handleDependencies(classPath)

        CtClass target = classPool.get(getClassFullName(classPath, file))
        def interfaces = target.getInterfaces();
        interfaces.each { CtClass cls ->
            // 如果 target 实现了 OnClickListener 则会进行处理
            if (cls.name == ON_CLICK_LISTENER) {
                println(">>>>>>>>处理${file.path}<<<<<<<<<")
                handleOnClickListener(target, classPath)
                return
            }
        }
    }

    /**
     * 处理 实现了 android.view.View$OnClickListener 的CtClass。包括匿名内部类。
     * @param target 实现了android.view.View$OnClickListener 的CtClass
     */
    def handleOnClickListener(CtClass target, String classPath) {
        CtMethod onClickMethod = target.getMethod(ON_CLICK, METHOD_SIGNATURE)
        classPool.importPackage("android.util.Log")
        onClickMethod.insertBefore("Log.e(\"${TAG}\", \"点击事件Hook成功\");")
        target.writeFile(classPath)
    }

    /**
     * 处理 实现了 android.view.View$OnClickListener 的CtClass。包括匿名内部类。
     * @param target 实现了android.view.View$OnClickListener 的CtClass
     */
    def handleOnClickListener(CtClass target) {
        CtMethod onClickMethod = target.getMethod(ON_CLICK, METHOD_SIGNATURE)
        classPool.importPackage("android.util.Log")
        onClickMethod.insertBefore("Log.e(\"${TAG}\", \"点击事件Hook成功\");")
        target.toBytecode()
    }


    /**
     * 处理 .jar 文件
     * @param jarFile File
     */
    def handleJarFile(File file) {
        if (file == null || !file.path.endsWith(JAR_FILE)) return

        //遍历jar文件内容
        Map<String, byte[]> caches = new HashMap<>()
        JarFile jarFile = new JarFile(file)
        def enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            // jarEntry.name        com/daddyno1/test/LoginActivity.class
            JarEntry jarEntry = enumeration.nextElement()

            //遍历target的所有实现的interfaces
            def className = getClassFullNameFromJarEntry(jarEntry)

            /**
             * 过滤androidx 及其相关
             */
//            if(className.startsWith(ANDROIDX)){
//                return
//            }

            CtClass target = classPool.get(className)   //获取目标Class
            def interfaces = target.getInterfaces()
            interfaces.each { CtClass cls ->
                // 如果 target 实现了 OnClickListener 则会进行处理
                if (cls.name == ON_CLICK_LISTENER) {
                    println(">>>>>>>>处理${jarEntry.name}<<<<<<<<<")
                    def bytes = handleOnClickListener(target)
                    caches.put(jarEntry.name, bytes)
                    //缓存class文件路径和修改过后的byte[]     com/daddyno1/test/LoginActivity.class  ->  byte[]
                    target.detach() //释放资源
                    return //阻止each循环
                }
            }
        }

        //遍历完Jar，进行jar替换操作
        if(!caches.isEmpty()){
            updateJarFile(file, caches)
        }
    }

    /**
     * 涉及 Jar 文件的修改。需要临时文件过渡。
     */
    def updateJarFile(File file, Map<String, byte[]> cache) {
        try {
            /**
             * 第一步：创建过渡文件
             */
            File optJar = new File(file.parent, file.name + OPT_FILE)
            def jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))

            /**
             * 遍历file，填充新的File
             */
            JarFile jarFile = new JarFile(file)
            def enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement()
                ZipEntry zipEntry = new ZipEntry(jarEntry.name)
                def inputStream = jarFile.getInputStream(zipEntry)
                jarOutputStream.putNextEntry(zipEntry)

                if (cache.containsKey(jarEntry.name)) {
                    //被处理过
                    jarOutputStream.write(cache.get(jarEntry.name))
                } else {
                    //没被处理
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
                inputStream.close()
            }
            jarOutputStream.close()
            jarFile.close()

            //删除源文件
            if (file.exists()) {
                file.delete()
            }
            //重命名
            optJar.renameTo(file)

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}