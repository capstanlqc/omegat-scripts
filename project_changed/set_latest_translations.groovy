/* :name = Set Latest Translations v3 :description=
 *
 * @author    Manuel Souto Pico, Thomas Cordonnier, Kos Ivantsov
 * @creation  2023.11.06: Manuel wrote first draft
 * @edit      2023.11.07: Thomas added OmegaT internals
 * @edit      2023.11.17: Kos added trick to avoid merge dialog
 * @edit      2023.11.19: Manuel added condition to run only for certain projects on load
 * @edit      2023.11.21: Manuel added check and warning if dummy file is missing
 * @edit      2023.11.22: Manuel removed reload (call to reloadProjectOnetime)
 * @edit      2023.11.22: Manuel added condition to discard translations in project if they are alternative
 * @edit      2023.11.23: Thomas added condition to discard TMX entries if they are alternative
 * @edit      2023.12.15: Added /tm/auto/next as update tm folder (together with /tm/auto/prev)
 * @edit      2023.12.15: Show dummy file missing error only if the project is not empty (it has batches)
 * @version   0.0.8
*/
import org.omegat.core.data.PrepareTMXEntry
import org.omegat.core.data.TMXEntry
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import groovy.util.*

//Since gui() is executed anyway, continueScript will change to false when needed
continueScript = true
changesMade = false
dummyFileException = null

// path to the folder inside the TM folder
path_to_tmx_dir = "auto" + File.separator + "prev"
path_to_prev_tmx_dir = "auto" + File.separator + "prev"
path_to_next_tmx_dir = "auto" + File.separator + "next"

def gui() {

    if (eventType == LOAD) {
        // skip
        if (skipUpdate(LOAD)) {
            LOAD.skipUpdate = false // reset the flag
            return
        }

        // abort if a team project is not opened
        props = project.projectProperties
        if (!props || !props.repositories) {
            final def title = "Set Latest Translations"
            final def msg   = "No project opened or not a team project."
            console.println("== ${title} ==")
            console.println(msg)
            // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
            continueScript = false
            return
        }

        // abort if the team project is not a pisa project
        projName = props.projectName
        if (!projName.startsWith("pisa_2025")) {
            final def title = "Set Latest Translations"
            final def msg   = "This is not a PISA25 FT project."
            console.println("== ${title} ==")
            console.println(msg)
            // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
            continueScript = false
            return
        }

        curEntry = editor.getCurrentEntryNumber()
        //Jump to the dummy file to avoid conflict resolution dialog
        dummyFileName = "zz.txt"
        sourceDirPath = props.getSourceRoot()
        sourceDir = new File(sourceDirPath)
        def allFiles = new FileNameFinder().getFileNames(sourceDirPath, '**/*.xml **/*.html' /* includes */, '' /* excludes */)
        numberOfFilesInProject = allFiles.size()
        def txtFiles = new FileNameFinder().getFileNames(sourceDirPath, '**/' + dummyFileName /* includes */, '**/*.xml **/*.html' /* excludes */)
        try {
            dummyFile = new File(txtFiles[0]) // .absolutePath
            assert dummyFile.getClass() == java.io.File
            if (dummyFile.exists()) {
                projectFiles = project.getProjectFiles()
                dummyFileIndex = projectFiles.findIndexOf {
                    it.filePath == sourceDir.toPath().relativize( dummyFile.toPath() ).toString() // relative path to dummy file
                }
                dummyEntry = projectFiles[dummyFileIndex].entries[0]
                editor.gotoEntry(dummyEntry.entryNum())
            }
        } catch (Exception e) {
            editor.gotoEntry(1)
            dummyFileException = e
        }

        


        if (!continueScript) {
            return
        }

        project.transMemories.each { name, tmx -> 
            name = name.substring(props.getTMRoot().length())
            if (name.startsWith(path_to_prev_tmx_dir) || name.startsWith(path_to_next_tmx_dir)) {
                // console.println("Importing from " + name)
                tmx.entries.each { entry ->
                    // see if the entry is alternative (= if it has id)
                    def isTmxEntryAlternative = entry.otherProperties.findAll  {  it -> it.type == 'id' } // or prev/next
                    // Search which entry in the project corresponds to the one in the tmx
                    // Note: to be improved, for the moment it works only with default entries
                    // and it is not optimized, we should use a cache as in ImportFromAutoTMX
                    def inProject = null
                    project.allEntries.each { pe -> 
                        def translation = project.getTranslationInfo(pe)
                        if ((pe.srcText.equals(entry.source))  && (!isTmxEntryAlternative) && (translation == null || translation.defaultTranslation)) inProject = pe; 
                    }
                    // Now search is done, if we found something we use it
                    if ((inProject != null) && (entry.source.equals(inProject.srcText))) {
                        def inProjectEntry = project.getTranslationInfo(inProject)
                        if ((inProjectEntry != null) && (!isTmxEntryAlternative) && (entry.source.equals(inProjectEntry.source))) {
                            long inProjectDate = inProjectEntry.creationDate
                            if (inProjectEntry.changeDate > inProjectEntry.creationDate) {
                                inProjectDate = inProjectEntry.changeDate
                            }
                            long inTmxDate = entry.creationDate
                            if (entry.changeDate > entry.creationDate) {
                                inTmxDate = entry.changeDate
                            }
                            // console.println(entry.source + " " + inTmxDate + " / " + inProjectDate + " => " + (inTmxDate > inProjectDate));
                            if (inTmxDate > inProjectDate) {
                                project.setTranslation(inProject, entry, true, null) // org.omegat.core.data.TMXEntry.ExternalLinked.xAUTO);
                                changesMade = true
                                console.println("Changes made!")
                            }
                        }
                    }
                }
            }
        }

        editor.gotoEntry(curEntry)
        if ( dummyFileException != null && numberOfFilesInProject > 0) { FlagDummyFileMissing(dummyFileException) }
        // org.omegat.gui.main.ProjectUICommands.projectReload()
        if (changesMade && eventType == LOAD) {
            console.println("No need to reload?")
            // reloadProjectOnetime()
            // org.omegat.gui.main.ProjectUICommands.projectReload();
        }   
    }
}




// functions

def skipUpdate(eventType) {
    if (!eventType.metaClass.hasProperty(eventType, 'skipUpdate')) {
        eventType.metaClass.skipUpdate = false
    }
    eventType.skipUpdate
}


def reloadProjectOnetime() {
    LOAD.skipUpdate = true    // avoid potentially infinity reloading loop
    javax.swing.SwingUtilities.invokeLater({
        org.omegat.gui.main.ProjectUICommands.projectReload()
    } as Runnable)
}

def FlagDummyFileMissing(e) {

    // @todo: create a generic function for all message dialogs
    
    console.println("There was a problem: ${e}")
    final def title = "Batch transition"
    final def msg   = "File missing error (merge dialog might appear): \n\n${e}.\n\nPlease report this message to cApStAn support."
    console.println("== ${title} ==")
    console.println(msg)
    showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
    // continueScript = false
}


return

/*
@TODO: 
- consider only tmx files that have the same name as the batch
*/ 