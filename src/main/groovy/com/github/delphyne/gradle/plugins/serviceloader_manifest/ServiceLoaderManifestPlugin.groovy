package com.github.delphyne.gradle.plugins.serviceloader_manifest

import com.github.delphyne.gradle.plugins.serviceloader_manifest.extensions.ServiceLoaderManifestExtension
import com.github.delphyne.gradle.plugins.serviceloader_manifest.tasks.GenerateServiceLoaderManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class ServiceLoaderManifestPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		ServiceLoaderManifestExtension extension = project.extensions.create(ServiceLoaderManifestExtension.NAME, ServiceLoaderManifestExtension)
		project.plugins.apply(JavaPlugin)
		GenerateServiceLoaderManifestTask task = project.tasks.create('generateServiceLoaderManifest', GenerateServiceLoaderManifestTask)
		task.dependsOn(project.tasks.findByName('classes'))
		project.tasks.findByName('jar').dependsOn(task)
		project.afterEvaluate {
			task.serviceInterfaces = extension.serviceInterfaces
		}
	}
}
