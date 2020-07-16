package com.daddyno1.buildsrc

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ClickTracePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "ClickTracePlugin = apply"

        def android = project.extensions.findByType(AppExtension.class)
        if(android != null){
            // 保证只在 App 模块使用
            android.registerTransform(new ClickTraceTransform(project: project))
        }
    }
}