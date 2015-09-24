/**
 * Created by YMolodkov on 23.09.2015.
 */
class PrepareReqIf {

    def extIdToFolderId = [:]

    public static void main(String[] args) {
        String inFile = args[0];
        String outFile = "out.xml";
        def prepareReqIf = new PrepareReqIf()

        Node xml = prepareReqIf.open(inFile)
        prepareReqIf.execute(xml)
        prepareReqIf.save(xml, outFile)
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
        replace(extIdToFolder, extIdToSpec, extFolderIdToExtSpecId)
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

    private void replace(extIdToFolder, extIdToSpec, extFolderIdToExtSpecId) {
        def specToFolder = [:]
        extFolderIdToExtSpecId.each { folderId, specId ->
            def folder = extIdToFolder[folderId]
            def spec = extIdToSpec[specId]
            if (folder && spec) {
                specToFolder.put(spec, folder)
            }
        }
        println "${specToFolder.size()} replaces"
        specToFolder.each { spec, folder ->
            def longName = spec."@LONG-NAME"
            def desc = folder.@DESC
            spec."@LONG-NAME" = "${longName} (${desc})"
            println "old: ${longName}, new:  ${spec."@LONG-NAME"}"
        }
    }


    private Node open(String inFile) {
        def xml = new XmlParser().parse(inFile)
        xml
    }

    private save(Node xml, outFile) {
        new XmlNodePrinter(new PrintWriter(new FileWriter(outFile))).print(xml)
    }
}
