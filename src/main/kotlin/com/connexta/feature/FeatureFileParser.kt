package com.connexta.feature

import com.connexta.feature.model.Bundle
import com.connexta.feature.model.Feature
import com.connexta.feature.model.FeatureFile
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
private val namePattern = Regex("(\\w+-)+app")

object FeatureFileParser {
    fun parseFeatureFile(featureFile: File): FeatureFile {
        val featureFileDocument = documentBuilder.parse(featureFile)
        val features = featureFileDocument.getElementsByTagName("features").item(0)

        val featureArray = ArrayList<Feature>()

        (0 until features.childNodes.length)
                .map { features.childNodes.item(it) }
                .filterIsInstance<Element>()
                .forEach {
                    if (it.tagName == "feature") {
                        featureArray.add(parseFeature(it))
                    } else {
                        println("non feature found: '${it.tagName}' in ${featureFile.canonicalPath}")
                    }
                }

        val name = namePattern.find(featureFile.canonicalPath)?.value ?: "UNKNOWN-APP"

        if (name == "UNKNOWN-APP") {
            println("Unknown app: ${featureFile.canonicalPath}")
        }
        return FeatureFile(name, featureArray)
    }


    fun linkFeatures(featureFiles: List<FeatureFile>) {
        val features = featureFiles.flatMap { it.features }
        println("linking ${features.size} features")

        for (feature in features) {
            for (index in 0 until feature.features.size) {
                val current = feature.features[index]
                feature.features[index] = features.find { it.name == current.name } ?: current
            }
        }

        for (featureFile in featureFiles) {
            for (index in 0 until featureFile.features.size) {
                val current = featureFile.features[index]
                featureFile.features[index] = features.find { it.name == current.name } ?: current
            }
        }
    }

    private fun parseFeature(feature: Element): Feature {
        val bundles = (0 until feature.childNodes.length)
                .map { feature.childNodes.item(it) }
                .filterIsInstance<Element>()
                .filter { it.tagName == "bundle" }
                .map {
                    it.textContent.trim()
                            .removePrefix("wrap:")
                            .removePrefix("mvn:")
                            .removePrefix("file:")
                }.map { it.split("/") }
                .map { Bundle(it[0], it[1], it[2]) }

        val features = (0 until feature.childNodes.length)
                .map { feature.childNodes.item(it) }
                .filterIsInstance<Element>()
                .filter { it.tagName == "feature" }
                .map { Feature(it.textContent.trim(), "", "") }
                .toMutableList()

        return Feature(
                feature.getAttribute("name").trim(),
                feature.getAttribute("install").trim(),
                feature.getAttribute("version").trim(),
                features,
                bundles)
    }
}