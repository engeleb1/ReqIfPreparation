/**
 * Created by YMolodkov on 23.09.2015.
 */
class PrepareReqIf_backup {
    def static extIdToSpec = [:]
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

    private static def getFolders(folderRefs, id) {

        def folderIdToSpecObjId = [:]
        folderRefs.each { folder ->
            def folderId = folder."rm:OBJECT"."rm:FOLDER-REF".text()
            def specObjId = folder."rm:CHILDREN"."rm:OBJECT"."rm:SPEC-OBJECT-REF".text()
            folderIdToSpecObjId.put(folderId, specObjId);
        }
        folderIdToSpecObjId
    }

    private static replace(xml) {
        def specs = xml."CORE-CONTENT"."REQ-IF-CONTENT"."SPECIFICATIONS"."SPECIFICATION"
        specs.each { spec ->
            def specObjId = spec."CHILDREN"."SPEC-HIERARCHY"."OBJECT"."SPEC-OBJECT-REF".text()
            extIdToSpec.put(specObjId, spec);
        }

        def folderRefs = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDER-HIERARCHY"."rm:CHILDREN"."rm:FOLDER-HIERARCHY"
        Object folderIdToSpecObjId = getFolders(xml, folderRefs)

        def folders = xml."TOOL-EXTENSIONS"."REQ-IF-TOOL-EXTENSION"."rm:FOLDERS"."rm:FOLDER"
        folders.each { folder ->
            def extId = folderIdToSpecObjId[folder.@IDENTIFIER]
            if (extId) extIdToFolder.put(extId, folder);
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

        extFolderIdToExtSpecId.each { folderId, specId ->
            def folder = extIdToFolder[folderId]
            def spec = extIdToSpec[specId]
            if (folder && spec) {
                def longName = spec."@LONG-NAME"
                def desc = folder.@DESC
                spec."@LONG-NAME" = "${longName} (${desc})"
            }
        }
    }

    private static save(Node xml, outFile) {
        new XmlNodePrinter(new PrintWriter(new FileWriter(outFile))).print(xml)
    }
}
