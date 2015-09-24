import groovy.xml.MarkupBuilder

/**
 * @author yaroslavTir
 */
class PrepareReqIf {

    def extIdToFolderId = [:]
    private String inFile
    private String outFile

    PrepareReqIf(String inFile, String outFile) {
        this.inFile = inFile
        this.outFile = outFile
    }

    public static void main(String[] args) {
        def prepareReqIf = new PrepareReqIf(args[0], "out.xml")

        Node xml = prepareReqIf.open()
        prepareReqIf.execute(xml)
        prepareReqIf.save(xml)
    }


    private void getFoldersHierarchy(hierarchy) {
        def folderId = hierarchy."rm:OBJECT"."rm:FOLDER-REF".text()
        def children = hierarchy."rm:CHILDREN"
        children.each {
            getFoldersChildren(children, folderId)
        }
    }

    private void getFoldersChildren(children, folderId) {
        def objects = children."rm:OBJECT"."rm:SPEC-OBJECT-REF"
        objects.each {
            extIdToFolderId.put(it.text(), folderId)
        }
        def hierarchy = children."rm:FOLDER-HIERARCHY"
        hierarchy.each {
            getFoldersHierarchy(it)
        }
    }


    private execute(xml) {
        def extIdToFolder = fillFolderData(xml)
        def extIdToSpec = fillSpecificationData(xml)
        def extFolderIdToExtSpecId = fillFolderSpecAssciationData(xml)
        def specFolderPairs = createSpecFolderPairs(extFolderIdToExtSpecId, extIdToFolder, extIdToSpec)
        replace(specFolderPairs)
//        replace2(xml, specFolderPairs)
    }

    private def fillFolderData(xml) {
        getFoldersHierarchy(xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDER-HIERARCHY")
        def folders = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDERS"."rm:FOLDER"
        def folderIdToFolder = [:];
        folders.each { folder ->
            folderIdToFolder.put(folder."@IDENTIFIER", folder)
        }

        def map = [:]
        extIdToFolderId.each { extId, folderId ->
            def folder = folderIdToFolder[folderId]
            map.put(extId, folder);
        }
        map
    }

    private def fillSpecificationData(xml) {
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"."SPECIFICATION"
        def map = [:]
        specs.each { spec ->
            spec."CHILDREN"."SPEC-HIERARCHY"."OBJECT"."SPEC-OBJECT-REF".each {
                map.put(it.text(), spec);
            }
        }
        map
    }

    private def fillFolderSpecAssciationData(xml) {
        def extens = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:ARTIFACT-EXTENSIONS"."rm:SPEC-OBJECT-EXTENSION"
        def map = [:]
        extens.each { exten ->
            def folder = exten."rm:CORE-SPEC-OBJECT-REF"?.text()
            if (folder) {
                def spec = exten."SPEC-OBJECT-REF"?.text()
                map.put(folder, spec)
            }
        }
        map
    }

    private def createSpecFolderPairs(extFolderIdToExtSpecId, extIdToFolder, extIdToSpec) {
        def specFolderPairs = [:]
        extFolderIdToExtSpecId.each { folderId, specId ->
            def folder = extIdToFolder[folderId]
            def spec = extIdToSpec[specId]
            if (folder && spec) {
                specFolderPairs.put(spec, folder)
            }
        }
        specFolderPairs
    }

    private void replace(specFolderPairs) {
        println "${specFolderPairs.size()} replaces"
        specFolderPairs.each { spec, folder ->
            def longName = spec."@LONG-NAME"
            def desc = folder.@DESC
            spec."@LONG-NAME" = "${longName} (${desc})"
            println "old: ${longName}, new:  ${spec."@LONG-NAME"}"
        }
    }


    private void replace2(xml, specFolderPairs) {
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"[0]
        def xmlOutput = new StringWriter()
        specs.name().namespaceURI = ""
        new XmlNodePrinter(new PrintWriter(xmlOutput)).print(specs)
        def specXML = xmlOutput.toString()

        def replaceMap = [:]
        specFolderPairs.each { spec, folder ->
            def oldLongName = spec."@LONG-NAME"
            def newLongName = "${oldLongName} (${folder.@DESC})"
            replaceMap.put("LONG-NAME=\"${oldLongName}\"", "LONG-NAME=\"${newLongName}\"")
        }
        boolean isSpecCection = false;
        boolean isSpecPrinted = false
        new File(outFile).withWriter { w ->
            new File(inFile).eachLine { line ->
                if (!isSpecCection) isSpecCection = line.contains("<SPECIFICATIONS>")
                if (isSpecCection && line.contains("</SPECIFICATIONS>")) {
                    isSpecCection = false
                } else {
                    if (isSpecCection && !isSpecPrinted) {
                        w.println xmlOutput.toString()
                        isSpecPrinted = true
                    }
                    if (!isSpecCection) {
                        w.println line
                    }
                }
            }
        }
    }

    private String replaceLine(def replaceMap, String line) {
        String newLine = line
        replaceMap.each { oldValue, newValue ->
            newLine = newLine.replaceAll(oldValue, newValue);
        }
        newLine
    }


    private Node open() {
        def xml = new XmlParser().parse(inFile)
        xml
    }

    private save(Node xml) {
        def xmlOutput = new StringWriter()
        new XmlNodePrinter(new PrintWriter(xmlOutput)).print(xml)
        new File(outFile).write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlOutput.toString())
    }
}
