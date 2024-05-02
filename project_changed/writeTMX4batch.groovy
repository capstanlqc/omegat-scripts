/* :name = Write PISA batch TM :description = 
 *
 *  @author:    Kos Ivantsov
 *  @version:   0.3
 *  @creation:  2024.01.12
 *  @update:    2024.05.01
 */

import java.nio.file.Files
import java.nio.file.Path
import org.omegat.util.OStrings
import org.omegat.util.StringUtil
import org.omegat.util.TMXReader2
import org.omegat.util.TMXWriter
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*

//// CLI or GUI probing
def echo
def cli
try {
    mainWindow.statusLabel.getText()
    echo = { k -> console.println(k.toString()) }
    cli = false
} catch(Exception e) {
    echo = { k -> println("\n~~~ Script output ~~~\n" + k.toString() + "\n^^^^^^^^^^^^^^^^^^^^^\n") }
    cli = true
}

//// SAVE happens during COMPILE, otherwise the script runs only after COMPILE is complete
if (eventType == SAVE) {
    // abort if a team project is not opened
    props = project.projectProperties
    if (!props || !props.repositories) {
        final def title = "Write PISA batch TM"
        final def msg   = "No project opened or not a team project."
        echo("== ${title} ==")
        echo(msg)
        // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
        return
    }

    // abort if the team project is not a pisa project
    projName = props.projectName
    // if (!projName.startsWith("pisa_2025")) {
    //     final def title = "Write PISA batch TM"
    //     final def msg   = "This is not a PISA25 project."
    //     echo("== ${title} ==")
    //     echo(msg)
    //     // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
    //     return
    // }

    echo("Generating batch TMX")
    omtVersion = OStrings.VERSION
    omtRevision = OStrings.REVISION
    projectRoot = props.projectRoot
    sourceRoot = props.sourceRoot
    sourcePath = Path.of(sourceRoot)
    targetRoot = props.targetRoot
    targetPath = Path.of(targetRoot)
    sourceLocale = props.getSourceLanguage().toString()
    targetLocale = props.getTargetLanguage().toString()
    if (props.isSentenceSegmentingEnabled()) {
        segmenting = TMXReader2.SEG_SENTENCE
    } else {
        segmenting = TMXReader2.SEG_PARAGRAPH
    }
    
    batches = []
    
    // Check if we have any subfolders
    projectFiles = project.projectFiles
    projectFileList = []
    projectFiles.each { file ->
        if (file.filePath =~ /(?i)\.(xml|html)$/) {
            projectFileList.add(file.filePath)
        }
    }
    projectFileList.each { file -> 
        dirName = file.split("[/\\\\]")
        if (dirName.size() > 1) {
            batches.add(dirName[0])
        }
    }
    uniqBatchNames = new LinkedHashSet<>(batches)
    batches = new ArrayList<>(uniqBatchNames)
    
    // If no subfolder were found, just exit
    if (batches.size() == 0) {
        echo("No batches in the source folder")
        return
    }
    
    // Create output folder target/tasks
    outputPath = Path.of("${targetRoot}tasks")
    Files.createDirectories(outputPath)
   
    // Collect all project files in each top-level subfolder
    batches.each {
        batchName = it.toString()
        outputTMXName = "${outputPath}${File.separator}${batchName}.tmx"
        outputTMXPath = Path.of(outputTMXName)
        outputTMXContents = new StringWriter()
        batchFiles = []
        translatedEntries = []
        projectFiles.each {
            if (it.filePath.startsWith(batchName)) {
                batchFiles.add(it)
            }
        }
        // Process all entries in each file of the batch
        batchFiles.each {
            it.entries.each { ste ->
                info = project.getTranslationInfo(ste)
                // Skip untranslated segments
                if (info.translation !== null) {
                    // Only some keys and values of each entry are collected to be able to drop repeated entries
                    // If entries were collected as is (ste), each would be unique because each has its unique entryNum 
                    entryMap = [:]
                    entryMap.isXAuto = info.linked.toString()
                    entryMap.creationId = info.creator
                    entryMap.changeId = info.changer
                    entryMap.creationDate = info.creationDate
                    entryMap.changeDate = info.changeDate
                    entryMap.source = StringUtil.makeValidXML(ste.srcText)
                    entryMap.target = info.translation ? StringUtil.makeValidXML(info.translation) : ""
                    if (info.hasNote()) {
                        entryMap.note = StringUtil.makeValidXML(info.note)
                    }
                    if (! info.defaultTranslation) {
                        entryMap.keyID = ste.key.id
                        entryMap.keyFile = ste.key.file
                        entryMap.keyNext = ste.key.next
                        entryMap.keyPrev = ste.key.prev
                    }
                    translatedEntries.add(entryMap)
                }
            }
        }
        // Remove repetitions in the collected entries. If a non-unique segment <+n more> has an alternative translation,
        // each alternative + one default translations are going to be stored. If a unique segent (without <+n more>) has default + alt translation,
        // only alternative translation will be stored.
        uniqueList = new HashSet<>(translatedEntries)
        uniqueEntries = uniqueList.collect()
        outputTMXContents << """\
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE tmx SYSTEM \"tmx11.dtd\">
<tmx version=\"1.1\">
  <header\
 creationtool=\"OmegaTScript\"\
 creationtoolversion=\"${omtVersion}_${omtRevision}\"\
 o-tmf=\"OmegaT TMX\"\
 adminlang=\"EN-US\"\
 datatype=\"plaintext\"\
 segtype=\"$segmenting\"\
 srclang=\"$sourceLocale"/>
  <body>"""
        uniqueEntries.each() { entry ->
            outputTMXContents << "\n    <tu>"
            if (entry.note) {
                outputTMXContents << "\n      <note>${entry.note}</note>"
            }
            if (entry.keyFile) {
                prevStr = (entry.keyPrev !== null) ? "\n      <prop type=\"prev\">${StringUtil.makeValidXML(entry.keyPrev)}</prop>" : ""
                nextStr = (entry.keyNext !== null) ? "\n      <prop type=\"next\">${StringUtil.makeValidXML(entry.keyNext)}</prop>" : ""
                idStr = (entry.keyID !== null) ? "\n      <prop type=\"id\">${entry.keyID}</prop>" : ""
                outputTMXContents << "\n      <prop type=\"file\">${entry.keyFile}</prop>${prevStr}${nextStr}${idStr}"
            }
            outputTMXContents << "\n      <tuv xml:lang=\"$sourceLocale\">\n        <seg>${entry.source}</seg>\n      </tuv>"
            createIDStr = (entry.creationId !== null) ? " creationid=\"${entry.creationId}\"" : "" 
            changeIDStr = (entry.changeId !== null) ? " changeid=\"${entry.changeId}\"" : ""
            createDateStr = (entry.creationDate) ? " creationdate=\"${new Date(entry.creationDate).format("yyyyMMdd'T'HHmmss'Z'")}\"" : ""
            changeDateStr = (entry.changeDate) ? " changedate=\"${new Date(entry.changeDate).format("yyyyMMdd'T'HHmmss'Z'")}\"" : ""
            outputTMXContents << "\n      <tuv xml:lang=\"$targetLocale\"${changeIDStr}${changeDateStr}${createIDStr}${createDateStr}>\n        <seg>${entry.target}</seg>\n      </tuv>\n    </tu>"
        }
        outputTMXContents << "\n  </body>\n</tmx>"
        // Output each batch to the respective TMX file
        new File(outputTMXName).write(outputTMXContents.toString(), "UTF-8")
    }
    echo("Batch TMX generated")
    return
}
