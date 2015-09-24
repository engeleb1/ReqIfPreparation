/**
 * Created by YMolodkov on 23.09.2015.
 */
class PrepareReqIf_backup2 {
    def static extIdToSpec = [:]
    def static extIdToFolderId = [:]
    def static extIdToFolder = [:]

    def static extFolderIdToExtSpecId = [:]
    def static extSpecIdToExtFolderId = [:]

    public static void main(String[] args) {
        String inFile = args[0];
        String outFile = "out.xml";
        Node xml = open(inFile)
        replace(xml)
        save(xml, outFile)
    }

    private static Node open(String inFile) {
        def xml = new XmlParser().parse(inFile)
        xml
    }

    private static void getFoldersHierarchy(hierarchy) {
        def folderId = hierarchy."rm:OBJECT"."rm:FOLDER-REF".text()
        def children = hierarchy."rm:CHILDREN"
        children.each{
            getFoldersChildren(children, folderId)
        }
    }

    private static void getFoldersChildren(children, folderId) {
        def objects = children."rm:OBJECT"."rm:SPEC-OBJECT-REF"
        objects.each {
            extIdToFolderId.put(it.text(), folderId)
        }
        def hierarchy = children."rm:FOLDER-HIERARCHY"
        hierarchy.each{
            getFoldersHierarchy(it)
        }
    }


    private static replace(xml) {
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"."SPECIFICATION"
        specs.each { spec ->
            spec."CHILDREN"."SPEC-HIERARCHY"."OBJECT"."SPEC-OBJECT-REF".each{
                extIdToSpec.put(it.text(), spec);
            }
        }

        getFoldersHierarchy(xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDER-HIERARCHY")

        def folders = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDERS"."rm:FOLDER"
        def folderIdToFolder = [:];
        folders.each { folder ->
            folderIdToFolder.put(folder."@IDENTIFIER", folder)
        }

        extIdToFolderId.each { extId, folderId ->
            def folder = folderIdToFolder[folderId]
            extIdToFolder.put(extId, folder);
        }


        def extens = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:ARTIFACT-EXTENSIONS"."rm:SPEC-OBJECT-EXTENSION"
        extens.each { exten ->
            def folder = exten."rm:CORE-SPEC-OBJECT-REF"?.text()
            if (folder) {
                def spec = exten."SPEC-OBJECT-REF"?.text()
                extFolderIdToExtSpecId.put(folder, spec)
                extSpecIdToExtFolderId.put(spec, folder)
            }
        }
        def specToFolder = [:]
        extFolderIdToExtSpecId.each { folderId, specId ->
            def folder = extIdToFolder[folderId]
            def spec = extIdToSpec[specId]
            if (folder && spec) {
                specToFolder.put(spec, folder)
            }
        }
        specToFolder.each{spec, folder ->
            def longName = spec."@LONG-NAME"
            def desc = folder.@DESC
            spec."@LONG-NAME" = "${longName} (${desc})"
        }
    }

    private static save(Node xml, outFile) {
        new XmlNodePrinter(new PrintWriter(new FileWriter(outFile))).print(xml)
    }
}
