## Description 

Script that set LONG_NAME of SPECIFICATION tag according with doors folder structure.

## install groovy

Windows
  
  - download http://www.groovy-lang.org/download.html
  - unzip in any place, and add to the path environment variable 

## run script 
- Extract ReqIF documnt if it is an archive (.zip or .reqifz)
- Open Command Prompt.
- Run set JAVA_OPTS="-Xmx8G"
- Run groovy PrepareReqIf.groovy YOU_FILE [PRETTY_FLAG=false]

   - YOU_FILE - doors file
   - PRETTY_FLAG - default value is "false". It means out.xml doesn't contain linebreaks or Indent, for pretty view set "true"
   - result file is "out.xml" in the same directory as script
- Replace the original .reqif file with out.xml.

## restriction 

 - aliase for "http://www.ibm.com/rm" should be xmlns:rm in input doors file.


