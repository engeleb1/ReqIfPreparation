import org.codehaus.groovy.runtime.InvokerHelper

/**
 * @author yaroslavTir
 */
class PrepareReqIf {

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
        def coreToSpecObject = fillCoreToSpecObjectMap(xml)
        def specObjectIdToFolder = fillFolderData(xml, coreToSpecObject)
        def specObjectToSpec = fillSpecificationData(xml)
        def specFolderPairs = createSpecFolderPairs(specObjectIdToFolder, specObjectToSpec)
        replace(specFolderPairs)
    }

    private def fillFolderData(xml, coreToSpecObject) {
        def specObjectIdToFolder = [:] 
        getFoldersHierarchy(xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDER-HIERARCHY", loadIdToFolder(xml), coreToSpecObject, specObjectIdToFolder)
        specObjectIdToFolder
    }
    
    private loadIdToFolder(xml) {
        def folderIdToFolder = [:];
        def folders = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDERS"."rm:FOLDER"
        folders.each { folder ->
            folderIdToFolder.put(folder."@IDENTIFIER", folder)
        }
        folderIdToFolder
    }


    private void getFoldersHierarchy(hierarchy, folderIdToFolder, coreToSpecObject, specObjectIdToFolder) {
        def folderId = hierarchy."rm:OBJECT"."rm:FOLDER-REF".text()
        def folder = folderIdToFolder[folderId]
        if(folder) {
            def children = hierarchy."rm:CHILDREN"
            println "Processing ${children.size()} children of ${folder."@DESC"}"
            children.each {
                processFoldersChildren(children, folderIdToFolder, folder, coreToSpecObject, specObjectIdToFolder)
            }
        } else {
            println "Folder with ID ${folderId} is unknown."
        }
    }

    private void processFoldersChildren(children, folderIdToFolder, folder, coreToSpecObject, specObjectIdToFolder) {
        def objects = children."rm:OBJECT"."rm:SPEC-OBJECT-REF"
        objects.take(1).each {
            def specObjectId = coreToSpecObject[it.text()]
            if(specObjectId) {
                specObjectIdToFolder.put(specObjectId, folder)
            } else {
                println "ERROR: Failed to load ID of Spec Object with core ID ${it.text()}."
            }
        }
        def hierarchy = children."rm:FOLDER-HIERARCHY"
        hierarchy.each {
            getFoldersHierarchy(it, folderIdToFolder, coreToSpecObject, specObjectIdToFolder)
        }
    }

    private def fillCoreToSpecObjectMap(xml) {
        def extens = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:ARTIFACT-EXTENSIONS"."rm:SPEC-OBJECT-EXTENSION"
        def map = [:]
        extens.each { exten ->
            def coreId = exten."rm:CORE-SPEC-OBJECT-REF"?.text()
            if (coreId) {
                def specObjectId = exten."SPEC-OBJECT-REF"?.text()
                map.put(coreId, specObjectId)
            }
        }
        map
    }

    private def fillSpecificationData(xml) {
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"."SPECIFICATION"
        println "Found ${specs.size()} specification(s)."
        def map = [:]
        specs.each { spec ->
            spec."CHILDREN"."SPEC-HIERARCHY"."OBJECT"."SPEC-OBJECT-REF".each {
                map.put(it.text(), spec);
            }
        }
        map
    }

    private def createSpecFolderPairs(specObjectIdToFolder, specObjectToSpec) {
        def specFolderPairs = [:]
        println "Processing ${specObjectIdToFolder.size()} folder(s)."
        specObjectIdToFolder.each { specObjectId, folder ->
            def spec = specObjectToSpec[specObjectId]
            if (folder && spec) {
                specFolderPairs.put(spec, folder)
            } else if(folder) {
                println "No specification found for folder ${folder.@DESC} and Spec Object ID ${specObjectId}."
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
