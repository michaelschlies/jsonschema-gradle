package com.michaelschlies.gradle

import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import groovy.lang.GroovyObject
import groovy.io.FileType

//import org.apache.commons.lang3.StringUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils

class JSGPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        GroovyObject x = {}
        Jsongenerator jsgExt = project.extensions.create('jsonSchema', Jsongenerator, project)
        project.task('jsonSchema') {
            doLast {
                List<URL> directories = []
                new File("build/classes").listFiles().each {
                    if (!it.directory)
                        return
                    println 'before dir = '
                    URL dir = (new File(it.absolutePath.concat(File.separator).concat('main')).toURI().toURL())
                    println 'after dir = '
                    if (!directories.contains(dir)) {
                        directories << dir
                        println "directory added: ${dir.toString()}"
                    }
                }
                List<String> classNames = []
                List<Class<?>> classes = []
                println "buildDir: ${project.buildDir}"
                project.buildDir.eachFileRecurse(FileType.FILES) {
                    if (it.name.endsWith('.class')) {
                        String relativePath = project.projectDir.relativePath(it)
                        relativePath = relativePath.replaceAll("build/classes/([A-Za-z]+)/main/", '')
                        String absoluteClassName = relativePath.replaceAll('.class$', '').replaceAll(File.separator, '.')
                        classNames << absoluteClassName
                        println "relative-ey path ${relativePath}"
//                    c.loadClass(absoluteClassName)
                    }
                }
                URLClassLoader c = new URLClassLoader(directories.toArray() as URL[], Thread.currentThread().contextClassLoader)
                println c.URLs
                classNames.forEach { String className ->
                    try {
                        println "trying to load ${className}"
                        classes << c.loadClass(className)
                    } catch (Throwable ex) {
                        ex.printStackTrace()
                    }
                }
                OptionPreset OP = new OptionPreset(
                        Option.SCHEMA_VERSION_INDICATOR,
                        Option.ADDITIONAL_FIXED_TYPES,
                        Option.FLATTENED_ENUMS,
                        Option.FLATTENED_OPTIONALS,
                        Option.DEFINITIONS_FOR_ALL_OBJECTS,
                        Option.VALUES_FROM_CONSTANT_FIELDS,
                        Option.PUBLIC_NONSTATIC_FIELDS,
                        Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
                        Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
                        Option.ALLOF_CLEANUP_AT_THE_END
                )
                SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OP)
                SchemaGeneratorConfig config = configBuilder.build()
                SchemaGenerator generator = new SchemaGenerator(config)
                FileTreeBuilder treeBuilder = new FileTreeBuilder(new File("${project.buildDir.absolutePath}${File.separator}"))
                File outDir = treeBuilder.dir('json')

                classes.forEach { Class cls ->
                    println "class loaded: ${cls.name}, package: ${cls.package.name}"
                    if (jsgExt.search_packages.contains(cls.package.name)) {
                        JsonNode jsonSchema = generator.generateSchema(cls)
                        String f = jsonSchema.toPrettyString()
                        File outFile = new File("${outDir.absolutePath}${File.separator}${cls.package.name}.${cls.name}.json")
                        println "Trying to write file: ${outFile.absolutePath}"
                        outFile.write(f)
                    }
                }


                println project.buildDir.absolutePath
            }
        }.dependsOn('build').setGroup('json')
        project.task('schemaZip', type: Zip) {
            description = 'Bundles the json schema files in a zip file'
//            archiveBaseName
//            archiveFileName = project.name + "-" + project.version + "-jsonSchemas.zip"
//            destinationDir = project.file(project.buildDir.path + "/libs")

            from (project.tasks.getByName('jsonSchema')){
                include "**/*.json"
            }
        }.dependsOn('jsonSchema').setGroup('json')
    }

    private static String getJsonFileName(String qualifiedName) {
        return StringUtils.substringAfterLast(qualifiedName, ".") + ".json"
    }
}
