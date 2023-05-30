package com.github.delphyne.gradle.plugins.serviceloader_manifest.tasks

import org.gradle.api.internal.AbstractTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.Modifier

class GenerateServiceLoaderManifestTask extends AbstractTask {

	@Input
	@SkipWhenEmpty
	List<String> serviceInterfaces

	@InputDirectory
	@SkipWhenEmpty
	File classesOutput

	@OutputDirectory
	File outputDirectory

	private final JavaPluginConvention javaConvention
	private final SourceSet main

	GenerateServiceLoaderManifestTask() {
		description = 'Generate META-INF/services manifests for use with ServiceLoaders'
		group = 'Source Generation'

		javaConvention = project.convention.getPlugin(JavaPluginConvention)
		main = javaConvention.sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
		SourceSetOutput mainOutput = main.output
		classesOutput = mainOutput.classesDir.canonicalFile
		outputDirectory = new File(mainOutput.resourcesDir, 'META-INF/services')
	}

	@TaskAction
	def run() {
		project.mkdir(outputDirectory)

		List<URL> classpath = main.runtimeClasspath.collect {
			it.toURI().toURL()
		}
		URLClassLoader classloader = URLClassLoader.newInstance(classpath as URL[])

		classloader.withCloseable {
			serviceInterfaces.each { serviceInterface ->
				logger.debug('Search for {} in {}', serviceInterface, classpath)
				Class<?> serviceClass = classloader.loadClass(serviceInterface)
				logger.debug('Found {}', serviceClass)

				List<File> classFiles = project.fileTree(classesOutput).findAll {
					it.isFile() && it.name.endsWith('.class')
				}
				logger.debug('Found {} candidates in {}', classFiles.size(), classFiles)

				List<String> classNames = classFiles.collect {
					classesOutput.toURI().relativize(it.toURI()).path.replaceAll(/\.class$/, '').replaceAll('/', '.')
				}
				logger.debug('Will consider {}', classNames)

				List<Class<?>> classes = classNames.collect {
					classloader.loadClass(it)
				}
				logger.debug('Successfully loaded {}', classes)

				List<Class<?>> implementations = classes.findAll {
					!(it.interface || Modifier.isAbstract(it.modifiers)) && Modifier.isPublic(it.modifiers) && serviceClass.isAssignableFrom(it)
				}
				logger.info('Found {} implementations of {}: {}', implementations.size(), serviceInterface, implementations)

				if (implementations.empty) {
					logger.warn('No implementations found for {}', serviceInterface)
					return
				}

				File manifest = new File(outputDirectory, serviceInterface)
				manifest.withWriter { out ->
					implementations.each {
						out.println(it.canonicalName)
					}
				}
				logger.info('Generated manifest at {}', manifest.path)
			}
		}
	}
}
