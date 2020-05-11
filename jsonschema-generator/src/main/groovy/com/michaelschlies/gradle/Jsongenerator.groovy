package com.michaelschlies.gradle

import groovy.transform.Canonical
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

import java.util.regex.Pattern

@Canonical
class Jsongenerator {
    List<String> search_packages = []
    List<Pattern> excludes = []

    Jsongenerator(Project project) {
        search_packages = project.objects.listProperty(String).getOrElse([])
        excludes = project.objects.listProperty(Pattern).getOrElse([])
    }
}
