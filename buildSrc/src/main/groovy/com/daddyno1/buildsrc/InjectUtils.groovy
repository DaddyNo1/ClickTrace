package com.daddyno1.buildsrc

import org.apache.commons.io.IOUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class InjectUtils{

    public static final String OPT_FILE = ".opt"

    /**
     * 涉及 Jar 文件的修改。需要临时文件过渡。
     * 更新Jar 文件的内容。
     * cache的结构为  key: jar中class文件目录  et: com/daddyno1/test/LoginActivity.class
     *               value：进行替换的 byte[]
     */
    static def updateJarFile(File file, Map<String, byte[]> cache) {
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