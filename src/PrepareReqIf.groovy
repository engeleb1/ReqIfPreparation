import org.codehaus.groovy.runtime.InvokerHelper

/**
 * @author yaroslavTir
 */
class PrepareReqIf {

    private String inFile
    private String outFile
    private Boolean pretty
    private specObjectToSpec = [:]
    

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
        def idToSpec = loadSpecificationData(xml)
        processFolders(xml, idToSpec)
        removeLinkTypes(xml)
    }

    private def processFolders(xml, idToSpec) {
        getFoldersHierarchy(xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDER-HIERARCHY", loadIdToFolder(xml), idToSpec)
    }
    
    private loadIdToFolder(xml) {
        def folderIdToFolder = [:];
        def folders = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDERS"."rm:FOLDER"
        folders.each { folder ->
            folderIdToFolder.put(folder."@IDENTIFIER", folder)
        }
        folderIdToFolder
    }

    private void getFoldersHierarchy(hierarchy, folderIdToFolder, idToSpec) {
        def folderId = hierarchy."rm:OBJECT"."rm:FOLDER-REF".text()
        def folder = folderIdToFolder[folderId]
        if(folder) {
            def children = hierarchy."rm:CHILDREN"
            children.each {
                processFoldersChildren(folder, children, folderIdToFolder, idToSpec)
            }
        } else {
            println "Folder with ID ${folderId} is unknown."
        }
    }

    private void processFoldersChildren(folder, children, folderIdToFolder, idToSpec) {
        def objects = children."rm:OBJECT"."rm:SPEC-OBJECT-REF"
        def specifications = objects.collect {
            //If SPEC-OBJECT-REF points to a specification child represents a module
            idToSpec[it.text()]
        }.grep().each {
            def longName = it."@LONG-NAME"
            def desc = folder.@DESC
            it."@LONG-NAME" = "$longName ($desc)"
            println "old: ${longName}, new:  ${it."@LONG-NAME"}"
        }
        def hierarchy = children."rm:FOLDER-HIERARCHY"
        hierarchy.each {
            getFoldersHierarchy(it, folderIdToFolder, idToSpec)
        }
    }

    private def loadSpecificationData(xml) {
        def idToSpec = [:]
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"."SPECIFICATION"
        println "Found ${specs.size()} specification(s)."
        specs.each { spec ->
           // println "${spec."@IDENTIFIER"} - ${spec."@LONG-NAME"}"
            idToSpec.put(spec."@IDENTIFIER", spec)
        }
        idToSpec
    }

    private void removeLinkTypes(xml) {
        def relationTypes = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPEC-TYPES"."SPEC-RELATION-TYPE"
        println "Found ${relationTypes.size()} relation type(s)"
        if(!relationTypes.isEmpty()) {
            def first = relationTypes.get(0)
            first."@LONG-NAME" = "Generic Relation"

            def others = relationTypes.takeRight(relationTypes.size() - 1).each({
                it.parent().remove(it)
            })

            def relations = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPEC-RELATIONS"."SPEC-RELATION".each({
                it."TYPE"."SPEC-RELATION-TYPE-REF".get(0).setValue(first."@IDENTIFIER")
            })


        }
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
