package com.connexta.feature

import com.connexta.feature.model.FeatureFile
import java.io.File

private val excludePattern = Regex("\\.\\w*|target|node|node_modules|java|webapp|schemas|test")

fun main(args: Array<String>) {
    val path = args.asList().getOrElse(0) { "C:/Users/TravisMcMahon/Development/ddf" }
    val resultPath = args.asList().getOrElse(1) { "C:/Users/TravisMcMahon/Desktop/test" }

    val files = File(path).walkTopDown()
            .onEnter { !it.name.matches(excludePattern) }
            .filter { it.name == "features.xml" }

    val featureFiles = files.map(FeatureFileParser::parseFeatureFile).toList()
    FeatureFileParser.linkFeatures(featureFiles)

    featureFiles.map { GraphExporter.writeGraph(it, "$resultPath/${it.name}.gexf") }

    val features = featureFiles.flatMap { it.features }.toMutableList()
    GraphExporter.writeGraph(FeatureFile("DDF", features), "$resultPath/CombinedProject.gexf")
}
