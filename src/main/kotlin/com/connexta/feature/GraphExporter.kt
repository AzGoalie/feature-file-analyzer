package com.connexta.feature

import com.connexta.feature.model.Bundle
import com.connexta.feature.model.Feature
import com.connexta.feature.model.FeatureFile
import java.io.FileWriter
import java.time.LocalDateTime
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

private fun XMLStreamWriter.document(init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartDocument()
    this.init()
    this.writeEndDocument()
    return this
}

private fun XMLStreamWriter.element(name: String, init: XMLStreamWriter.() -> Unit): XMLStreamWriter {
    this.writeStartElement(name)
    this.init()
    this.writeEndElement()
    return this
}

private fun XMLStreamWriter.element(name: String, content: String) {
    element(name) {
        writeCharacters(content)
    }
}

fun XMLStreamWriter.attribute(name: String, value: String) = writeAttribute(name, value)

object GraphExporter {
    fun writeGraph(featureFile: FeatureFile, path: String) {
        val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(FileWriter(path))

        writer.document {
            element("gexf") {
                attribute("xmlns", "http://www.gexf.net/1.2draft")
                attribute("version", "1.1")

                element("meta") {
                    attribute("lastmodifieddate", LocalDateTime.now().toString())

                    element("creator", "Travis McMahon")
                    element("description", "A graph file for ${featureFile.name}")
                }

                element("graph") {
                    attribute("mode", "static")
                    attribute("defaultedgetype", "directed")

                    element("attributes") {
                        attribute("class", "node")

                        element("attribute") {
                            attribute("id", "0")
                            attribute("title", "bundle?")
                            attribute("type", "boolean")
                            element("default", "false")
                        }
                    }

                    element("nodes") {
                        val featureMap = hashMapOf<Feature, String>()
                        for (feature in featureFile.features) {
                            writeFeatureNode(feature, featureMap)
                        }
                    }

                    element("edges") {
                        val edgeList = arrayListOf<String>()
                        for (feature in featureFile.features) {
                            writeEdge(feature, edgeList)
                        }
                    }
                }
            }
        }.flush()
    }

    private fun XMLStreamWriter.writeBundleNode(bundle: Bundle, parent: Feature) {
        element("node") {
            attribute("id", "${parent.name}:${bundle.groupId}/${bundle.artifactId}/${bundle.version}")
            attribute("label", bundle.artifactId)
            element("attvalues") {
                element("attvalue") {
                    attribute("for", "0")
                    attribute("value", "true")
                }
            }
        }
    }

    private fun XMLStreamWriter.writeFeatureNode(feature: Feature, featureMap: MutableMap<Feature, String>) {
        if (featureMap.containsKey(feature)) return
        featureMap.put(feature, feature.name)

        element("node") {
            attribute("id", feature.name)
            attribute("label", feature.name)
            element("attvalues") {
                element("attvalue") {
                    attribute("for", "0")
                    attribute("value", "false")
                }
            }
        }

        feature.features.forEach { writeFeatureNode(it, featureMap) }
        feature.bundles.forEach { writeBundleNode(it, feature) }
    }

    private fun XMLStreamWriter.writeEdge(feature: Feature, edgeList: MutableList<String>) {
        for (child in feature.features) {
            val id = "${feature.name}:${child.name}"
            if (edgeList.contains(id)) continue
            edgeList.add(id)
            element("edge") {
                attribute("id", id)
                attribute("source", feature.name)
                attribute("target", child.name)
            }

            writeEdge(child, edgeList)
        }

        for ((artifactId, groupId, version) in feature.bundles) {
            val id = "${feature.name}:$groupId/$artifactId/$version"
            if (edgeList.contains(id)) continue
            edgeList.add(id)
            element("edge") {
                attribute("id", "")
                attribute("source", feature.name)
                attribute("target", id)
            }
        }
    }
}