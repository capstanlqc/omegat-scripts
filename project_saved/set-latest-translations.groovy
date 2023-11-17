/* :name = Set Latest Translations v3 :description=
 *
 * @author    Manuel Souto Pico (original idea and first attemp, final tweaks: run only for certain projects on load)
 * @author    Thomas Cordonnier (adaptation to use OmegaT internals)
 * @author    Kos Ivantsov (added trick to avoid merge dialog)
 * @creation  2023.11.06
 * @edit      2023.11.17
 * @version   0.0.3
*/

import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import groovy.util.*

//Since gui() is executed anyway, continueScript will change to false when needed
continueScript = true
changesMade = false

// path to the folder inside the TM folder
path_to_tmx_dir = "auto" + File.separator + "prev"


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
            showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
            continueScript = false
            return
        }

        // abort if the team project is not a pisa project
        projName = props.projectName
        if (!projName.contains("pisa_2025ft_translation")) {
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
        def txtFiles = new FileNameFinder().getFileNames(sourceDirPath, '**/zz.txt' /* includes */, '**/*.xml **/*.html' /* excludes */)
        dummyFile = new File(txtFiles[0]) // .absolutePath

        if (! dummyFile.exists()) {
            console.println("Dummy file doesn't exist")
            //continueScript = false
            //return
        } else {
            projectFiles = project.getProjectFiles()
            dummyFileIndex = projectFiles.findIndexOf { 
                it.filePath == sourceDir.toPath().relativize( dummyFile.toPath() ).toString() // relative path to dummy file
            }            
            dummyEntry = projectFiles[dummyFileIndex].entries[0]
            editor.gotoEntry(dummyEntry.entryNum())
        }
        
        if (!continueScript) {
            return
        }

        project.transMemories.each { name, tmx -> 
            name = name.substring(props.getTMRoot().length())
            if (name.startsWith(path_to_tmx_dir)) {
                // console.println("Importing from " + name)
                tmx.entries.each { entry ->
                    // Search which entry in the project corresponds to the one in the tmx
                    // Note: to be improved, for the moment it works only with default entries
                    // and it is not optimized, we should use a cache as in ImportFromAutoTMX
                    def inProject = null
                    project.allEntries.each { pe -> 
                        if (pe.srcText.equals(entry.source)) inProject = pe;
                    }
                    // Now search is done, if we found something we use it
                    if ((inProject != null) && (entry.source.equals(inProject.srcText))) {
                        def inProjectEntry = project.getTranslationInfo(inProject)
                        if ((inProjectEntry != null) && (entry.source.equals(inProjectEntry.source))) {
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
        // org.omegat.gui.main.ProjectUICommands.projectReload()
        if (changesMade && eventType == LOAD) {
            reloadProjectOnetime()
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





return

/*
Questions: 
- why 2 x done (91 ms elapsed)
- neverending spinning clock?

@TODO: 
- consider only tmx files that have the same name as the batch

*/ 