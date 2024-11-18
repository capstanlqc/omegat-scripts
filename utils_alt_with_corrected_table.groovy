
/*:name = Utils - Create TMX with alternative translations from table :description = 
 * 
 *  @author:  Kos Ivantsov
 *  @date:    19-06-2024
 *  @version: 0.0.1
 *  
 *  @Usage:   The script expects a spreadsheet called 'correct' (possible extensions: tsv, xls, xlsx) in the project directory.
 *            The spreadsheet should contain:
 *            +------------+--------------------+---------------------+
 *            | Segment ID | OmegaT Source Text | Correct Target Text |
 *            +------------+--------------------+---------------------+ 
 *            Other columns will be ignored. The script outputs one or two files into <project>/script_output/:
 *            1. <project_name>_alt.tmx with alternative translations for the IDs found both in the OmegaT project
 *               and in the correct file if the source text is identical in both
 *            2. <project_name>_errors.tsv listing records in the correct file where the source text is different;
 *               and records in the correct file with IDs not found in the OmegaT project.
 */

import org.omegat.core.data.ProtectedPart
import org.omegat.util.Preferences
import org.omegat.util.StringUtil
import org.omegat.util.TMXReader2

// Set the suffix for the username to mark changes
changedSuffix = "_alt"
originalAuthor = Preferences.getPreferenceDefault(Preferences.TEAM_AUTHOR, System.getProperty("user.name"))
changesBy = originalAuthor + changedSuffix

// Set the name of the script so it's clearly visible in the console output
scriptName = "Create TMX with alternative translations"

// Check if the project is open and exit if not
prop = project.projectProperties
if (!prop) {
    console.println("${scriptName}\n${"="*scriptName.size()}\nNo project open")
    return
}

// Get the libs to read Excel and TSV files
@Grab(group='org.apache.poi', module='poi-ooxml', version='5.2.3')
@Grab(group='org.apache.commons', module='commons-csv', version='1.10.0')

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.nio.file.Files
import java.nio.file.Paths

// Function to read Excel file
def readExcelFile(String filePath) {
    FileInputStream file = new FileInputStream(new File(filePath.toString()))
    Workbook workbook = null
    
    // Determine if the file is xls or xlsx
    if (filePath.endsWith(".xlsx")) {
        workbook = new XSSFWorkbook(file)
    } else if (filePath.endsWith(".xls")) {
        workbook = new HSSFWorkbook(file)
    } else {
        throw new IllegalArgumentException("The specified file is not Excel file")
    }

    // List to contain excel data (list of arrays)
    excelData = []
    // Get the first sheet
    Sheet sheet = workbook.getSheetAt(0)
    // Iterate through rows and columns
    sheet.each { Row row ->
        // Cells of each row to be stored as elements of an array 
        rowArray = []
        row.each { Cell cell ->
            cellValue = cell.toString()
            rowArray << cellValue
        }
        // Add collected row data to the list
        excelData << rowArray
    }

    // Close the file and workbook
    file.close()
    workbook.close()
    return excelData
}

// Function to read TSV file
def readTSVFile(String filePath) {
    // List to contain tsv data
    tsvData = []
    path = Paths.get(filePath)
    reader = Files.newBufferedReader(path)
    csvParser = new CSVParser(reader, CSVFormat.TDF.withQuote('"'.charAt(0)))

    csvParser.each { csvRecord ->
        rowArray = csvRecord.toList()
        // Add collected row data to the list
        tsvData << rowArray
    }

    // Close the parser and reader
    csvParser.close()
    reader.close()
    return tsvData
}

// Determine file extension (used to check if the corrected file exists and is supported)
def getFileExtension(String filePath) {
    def path = Paths.get(filePath)
    def fileName = path.fileName.toString()
    
    if (fileName.contains('.')) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
    }
    return ""
}

// Now collect the project specific data
projectRoot = prop.projectRoot
projectName = prop.projectName
sourceLocale = prop.getSourceLanguage().toString()
targetLocale = prop.getTargetLanguage().toString()
if (prop.isSentenceSegmentingEnabled()) {
    segmenting = TMXReader2.SEG_SENTENCE
} else {
    segmenting = TMXReader2.SEG_PARAGRAPH
}

// Find the file with correct target versions and determine how it should be treated
correctBaseName = "correct"
possibleExtensions = ["txt", "tsv", "csv", "xls", "xlsx"]
correctFilePath = correctWithAnyExtension(correctBaseName, projectRoot, possibleExtensions)

if (correctFilePath == null) {
    console.println("${scriptName}\n${"="*scriptName.size()}\nRequired file not found!")
    return
}

correctFile = new File(correctFilePath)
correctExtension = getFileExtension(correctFile.toString())

if (correctExtension.equalsIgnoreCase("tsv") || correctExtension.equalsIgnoreCase("csv")) {
    correctData = readTSVFile(correctFile.toString())
} else if (correctExtension.equalsIgnoreCase("xls") || correctExtension.equalsIgnoreCase("xlsx")) {
    correctData = readExcelFile(correctFile.toString())
}

def correctWithAnyExtension(correctBaseName, projectRoot, possibleExtensions) {
    def foundCorrectFile = possibleExtensions.findResult { extension -> 
        filePath = "${projectRoot}${correctBaseName}.${extension.toLowerCase()}"
        if (new File(filePath.toString()).exists()) {
            return filePath
        }
    }
    return foundCorrectFile ?: null
}
correctFile = new File(correctWithAnyExtension(correctBaseName, projectRoot, possibleExtensions))
if (! correctFile.exists()) {
    console.println("${scriptName}\n${"="*scriptName.size()}\nRequired file not found!")
    return
}
console.println correctFile
correctExtension = getFileExtension(correctFile.toString())
if (correctExtension.equalsIgnoreCase("tsv") || correctExtension.equalsIgnoreCase("csv")) {
    correctData = readTSVFile(correctFile.toString())
} else if (correctExtension.equalsIgnoreCase("xls") || correctExtension.equalsIgnoreCase("xlsx")) {
    correctData = readExcelFile(correctFile.toString())
}

// Collect segments' IDs in the correct data
correctIDs = correctData.collect { it[0] }

outputFolder = projectRoot + "script_output"
outputTMXFile = outputFolder + File.separator + "${projectName}_alt.tmx"
outputErrorFile = outputFolder + File.separator + "${projectName}_errors.tsv"
outputTMXContents = new StringWriter()
outputTMXContents << """<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE tmx SYSTEM \"tmx11.dtd\">
<tmx version=\"1.1\">
  <header\
 creationtool=\"OmegaTScript\"\
 o-tmf=\"OmegaT TMX\"\
 adminlang=\"EN-US\"\
 datatype=\"plaintext\"\
 segtype=\"${segmenting}\"\
 srclang=\"${sourceLocale}"/>
  <body>
"""
outputErrorContents = new StringWriter()
outputErrorContents << "Segment number" + "\t" + "Segment ID" + "\t" + "OmegaT Source" + "\t" + "Expected source" + "\t" + "OmegaT target" + "\t" + "Requested target" + "\n"
correctedCount = 0
errorCount = 0
projectIDs = []
project.allEntries.each { ste ->
    info = project.getTranslationInfo(ste)
    source = ste.srcText
    target = info.translation
    creationId = info.creator
    changeId = info.changer
    creationDate = info.creationDate
    changeDate = info.changeDate
    keyID = ste.key.id
    projectIDs.add(keyID)
    keyFile = ste.key.file
    keyNext = ste.key.next
    keyPrev = ste.key.prev
    note = info.hasNote() ? StringUtil.makeValidXML(info.note) : null
    // Collect tags to be able to skip segments with tags only
    sourceTags = []
    for (ProtectedPart pp : ste.getProtectedParts()) {
        sourceTags.add(pp.getTextInSourceSegment())
    }
    // Use only translated segments and only the ones that don't contain only tags
    if ((!(info.translation == null)) && (!(source.replaceAll('\u200c', '') == sourceTags.join("")))) {
        correctData.each { array ->
            correctID = array[0]     //Segment's ID from the correction file
            correctSource = array[1] //Segment's source from the correction file
            correctTarget = array[2] //Segment's requested target from the correction file
            if (keyID == correctID) {
                //console.println(ste.key.id)
                if (ste.srcText == correctSource) {
                    source = StringUtil.makeValidXML(source)
                    target = StringUtil.makeValidXML(correctTarget)
                    outputTMXContents << "    <tu>"
                    if (note) {
                        outputTMXContents << "\n      <note>${note}</note>"
                    }
                    prevStr = (keyPrev) ? "\n      <prop type=\"prev\">${StringUtil.makeValidXML(keyPrev)}</prop>" : ""
                    nextStr = (keyNext) ? "\n      <prop type=\"next\">${StringUtil.makeValidXML(keyNext)}</prop>" : ""
                    idStr = (keyID) ? "\n      <prop type=\"id\">${keyID}</prop>" : ""
                    outputTMXContents << "\n      <prop type=\"file\">${keyFile}</prop>${prevStr}${nextStr}${idStr}"
                    outputTMXContents << "\n      <tuv xml:lang=\"$sourceLocale\">\n        <seg>${source}</seg>\n      </tuv>"
                    createIDStr = (creationId) ? " creationid=\"${creationId}\"" : "" 
                    changeIDStr = " changeid=\"${changesBy}\""
                    createDateStr = (creationDate) ? " creationdate=\"${new Date(creationDate).format("yyyyMMdd'T'HHmmss'Z'")}\"" : ""
                    changeDateStr = " changedate=\"${new Date().format("yyyyMMdd'T'HHmmss'Z'")}\""
                    outputTMXContents << "\n      <tuv xml:lang=\"$targetLocale\"${changeIDStr}${changeDateStr}${createIDStr}${createDateStr}>\n        <seg>${target}</seg>\n      </tuv>\n    </tu>\n"
                    correctedCount++
                } else {
                    outputErrorContents << ste.entryNum() + "\t" + keyID + "\t" + source + "\t" + correctSource + "\t" + target + "\t" + correctTarget + "\n" 
                    errorCount++
                }
            }
        }
    }
}

if ((correctedCount > 0 || errorCount > 0) && (!(new File(outputFolder).exists()))) {
    new File(outputFolder).mkdir()
}
if (correctedCount > 0) {
    outputTMXContents << "  </body>\n</tmx>"
    new File(outputTMXFile).write(outputTMXContents.toString(), "UTF-8")
    
}
if (errorCount > 0) {
    correctIDs.each { item ->
        if (!(projectIDs.contains(item))) {
            outputErrorContents << "\t" + item + "\t" + "Not found in OmegaT project"
        }
    }
    new File(outputErrorFile).write(outputErrorContents.toString(), "UTF-8")
}
