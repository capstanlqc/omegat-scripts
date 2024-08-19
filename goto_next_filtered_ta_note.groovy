/*:name = GoTo - Filtered T&A note / next :description = Jumps to the next segment which has a T&A note
 * 
 */

import org.omegat.core.data.SourceTextEntry
import org.omegat.core.data.TMXEntry
import org.omegat.core.data.ProjectTMX
import org.omegat.core.data.EntryKey

prop = project.projectProperties
exit = false
if (!prop) {
    final def title = 'Next filtered T&A note'
    final def msg = 'Please try again after you open a project.'
    showMessageDialog null, msg, title, INFORMATION_MESSAGE
    exit = true
    return
}

filtered_note = new File(prop.getProjectRoot() + "filtered_note.txt")
if (!filtered_note.exists()) {
    regex = 'XYZZZ'
} else {
    regex = filtered_note.text.trim()
}

checkOrphanedCallback = new ProjectTMX.CheckOrphanedCallback() {
        boolean existSourceInProject(String src) {
            return existSource.contains(src)
        }

        boolean existEntryInProject(EntryKey key) {
            return existKeys.contains(key)
        }
    }


def loadTmxFile(File tmxFile) {
    notesTmx = new ProjectTMX(srcLang, traLang,
        Core.getProject().getProjectProperties().isSentenceSegmentingEnabled(), tmxFile, checkOrphanedCallback)
}

def getComment(SourceTextEntry newEntry) {
    if (notesTmx == null) {
        return null
    }
    TMXEntry te = notesTmx.getMultipleTranslation(newEntry.getKey())
    if (te == null) {
        te = notesTmx.getDefaultTranslation(newEntry.getSrcText())
    }
    if (te != null) {
        if (te.translation == null)
            try {
            if ((! this.srcLang.equals(prop.getSourceLanguage()))
                || (! this.traLang.equals(prop.getTargetLanguage()))) {
                return null
            }
            checkTmxFileLanguages()
            loadTmxFile(tmxFile)
            return getComment(newEntry)
        } catch (Exception ex) {
            ex.printStackTrace()
        }
        return "T&A note: " + te.translation
    }
    return null
}

lastSegmentNumber = project.allEntries.size()
jump = false

def gui() {
    File dir = new File(prop.getProjectRoot() + "/notes");
    if (dir.exists()) {
        list = dir.listFiles().findAll { it.isFile() && it.name.toLowerCase().endsWith('.tmx') }
        if (list.size() == 0) {
            console.println("Extra notes : /notes directory exists but contains no files")
            exit = true
        } else {
            srcLang = prop.getSourceLanguage()
            traLang = prop.getTargetLanguage()
            loadTmxFile(tmxFile = list[0]);
        }
    } else {
        exit = true
    }

    if (exit) {
        return
    }
    ste = editor.getCurrentEntry()
    currentSegmentNumber = startingSegmentNumber = ste.entryNum()
    while (!jump) {
        nextSegmentNumber = currentSegmentNumber == lastSegmentNumber ? 1 : currentSegmentNumber + 1
        stn = project.allEntries[nextSegmentNumber - 1]
        info = project.getTranslationInfo(stn)
        src = stn.getSrcText()
        comment = getComment(stn).toString()
        if (nextSegmentNumber == startingSegmentNumber) {
            return
        }
        filtered_comment = comment.find(regex)
        //console.println(comment)
        if (filtered_comment) {
            jump = true
            console.println(comment)
            editor.gotoEntry(nextSegmentNumber)
            return
        } else {
            jump = false
            currentSegmentNumber = nextSegmentNumber
        }
    }
}
return
