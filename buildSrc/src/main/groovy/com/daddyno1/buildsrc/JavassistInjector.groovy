package com.daddyno1.buildsrc

import com.android.build.gradle.AppExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import org.gradle.api.Project

class JavassistInjector {
    private static final String CLASS_PATH_SYMBOL = "/classes/"
    private static final String CLASS_FILE = ".class"
    private static final String FILE_SEPARATOR = "/"
    private static final String DOT = "."
    public static final String ON_CLICK_LISTENER = "android.view.View\$OnClickListener"
    public static final String ON_CLICK = "onClick"
    public static final String METHOD_SIGNATURE = "(Landroid/view/View;)V"


    Project project
    def appExtension
    ClassPool classPool = new ClassPool(ClassPool.getDefault())
    def isAndroidClassLoaded = false
    def classPath = null

    JavassistInjector(Project project) {
        this.project = project
        appExtension = project.extensions.findByType(AppExtension.class)
    }

    /**
     *  给ClassPool 添加类加载的路径。
     * @param pathName Appends a directory or a jar (or zip) file to the end of the search path.
     */
    def appendClassPath(String pathName) {
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
    def loadAndroidClass() {
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

    def getPathFromClass(String path) {
        if (classPath != null) return classPath
        if (path == null) return ""
        int index = path.indexOf(CLASS_PATH_SYMBOL)
        classPath = path.substring(0, index + 8)
        return classPath
    }

    def getFullNameFromClass(String path) {
        if (path == null) return ""
        def tmp = path - getPathFromClass(path)
        def clsName = tmp - CLASS_FILE
        return clsName.replaceAll(FILE_SEPARATOR, DOT).substring(1)
    }

    /**
     * 处理 .class 文件
     * etc: /Users/jxf/workspace/Android/valuableProject/ClickTrace/app/build/intermediates/javac/debug/classes/com/daddyno1/clicktrace/MainActivity.class
     */
    def handleClassFile(File file) {
        if (file == null || !file.absolutePath.endsWith(".class")) {
            return
        }
        println file.absolutePath

        loadAndroidClass()
        appendClassPath(getPathFromClass(file.path))
        CtClass target = classPool.get(getFullNameFromClass(file.path))
        def interfaces = target.getInterfaces();
        interfaces.each { CtClass cls ->
            if(cls.name == ON_CLICK_LISTENER){
                handleOnClickListener(target)
            }
        }
    }

    /**
     * 处理 实现了 android.view.View$OnClickListener 的CtClass。包括匿名内部类。
     * @param target  实现了android.view.View$OnClickListener 的CtClass
     */
    def handleOnClickListener(CtClass target){
        CtMethod onClickMethod = target.getMethod(ON_CLICK, METHOD_SIGNATURE)
        classPool.importPackage("android.util.Log")
        onClickMethod.insertBefore("Log.e(\"JJJJJ\", \"点击事件Hook成功\");")
        target.writeFile(classPath)
    }
}