package com.github.delphyne.gradle.plugins.serviceloader_manifest.extensions

class ServiceLoaderManifestExtension {

	public final static String NAME = 'serviceLoader'

	List<String> serviceInterfaces = []

	ServiceLoaderManifestExtension serviceInterface(String... serviceInterfaces) {
		this.serviceInterfaces += (serviceInterfaces as List)
		this
	}
}
