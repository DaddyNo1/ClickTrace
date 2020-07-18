Hook  android.view.View.OnClickListener#onClick

##### 实现原理：
自定义一个Gradle Plugin，然后注册一个Transform对象，然后再transform方法里，分别遍历
目录 和 jar包，然后我们就可以遍历所有的.class文件。然后在利用Javassist的相关API，
加载相应 .class文件、解析.class文件，就可以找到满足一定特定条件的.class和相关方法，
最后去修改相应的方法以动态插入埋点字节码，从而达到自动埋点的效果。
