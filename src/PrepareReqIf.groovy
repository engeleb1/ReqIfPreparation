import org.codehaus.groovy.runtime.InvokerHelper

/**
 * @author yaroslavTir
 */
class PrepareReqIf {

    def extIdToFolderId = [:]
    private String inFile
    private String outFile
    private Boolean pretty

    PrepareReqIf(String inFile, String outFile, def pretty) {
        this.inFile = inFile
        this.outFile = outFile
        this.pretty = pretty
    }

    public static void main(String[] args) {
        Boolean pretty = false;
        if (args.length > 1) pretty = args[1].toBoolean()

        def prepareReqIf = new PrepareReqIf(args[0], "out.xml", pretty)
        Node xml = prepareReqIf.open()
        prepareReqIf.execute(xml)
        prepareReqIf.save(xml)
    }

    private execute(xml) {
        def extIdToFolder = fillFolderData(xml)
        def extIdToSpec = fillSpecificationData(xml)
        def extFolderIdToExtSpecId = fillFolderSpecAssciationData(xml)
        def specFolderPairs = createSpecFolderPairs(extFolderIdToExtSpecId, extIdToFolder, extIdToSpec)
        replace(specFolderPairs)
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
            spec."@LONG-NAME" = createNewLongName(longName, desc)
            println "old: ${longName}, new:  ${spec."@LONG-NAME"}"
        }
    }

    private def createNewLongName(longName, desc) {
        def path = (desc - ~/(\w+)[^\/]*$$/)[0..-2]
        "${longName} (${path})"
    }

    private Node open() {
        def parser = new XmlParser(false, false)
        parser.setKeepIgnorableWhitespace(false) //true leads to significantly higher memory consumption
        def xml = parser.parse(inFile)
        println xml.attributes()
        xml
    }

    private save(Node xml) {
        def xmlOutput = new PrintWriter("out.xml", "UTF-8")
        xmlOutput.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        if (pretty){
            new XmlNodePrinter(xmlOutput).print(xml)
        } else {
            def nodePrinter = new XmlNodePrinter(new IndentPrinter(xmlOutput, "", true))
            nodePrinter.print(xml)
        }
        xmlOutput.close();
    }
}
