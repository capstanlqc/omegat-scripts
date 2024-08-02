/*:name = GoTo - Filtered note / previous :description=Jump to previous segment with a note matching the filter
 *  
 *  @author:   Kos Ivantsov
 *  @date:     2024-08-02
 *  @version:  0.1         
 */

 
import static javax.swing.JOptionPane.*
import static org.omegat.util.Platform.*

prop = project.projectProperties
filtered_note = new File(prop.getProjectRoot() + "filtered_note.txt")
if (! filtered_note.exists()) {
    filter = "XYZZZ"
} else {
    filter = filtered_note.text
}
console.println filter
exit = false
if (!prop) {
  final def title = 'Next alternative'
  final def msg   = 'Please try again after you open a project.'
  showMessageDialog null, msg, title, INFORMATION_MESSAGE
  exit = true
  return
}

lastSegmentNumber = project.allEntries.size()
jump = false
def gui() {
    if (exit)
    return
    ste = editor.getCurrentEntry()
    currentSegmentNumber = startingSegmentNumber = ste.entryNum()
    //jump = false
    while (!jump) {
        prevSegmentNumber = currentSegmentNumber == 1 ? lastSegmentNumber : currentSegmentNumber - 1
        stn = project.allEntries[prevSegmentNumber -1]
        info = project.getTranslationInfo(stn)
        note = info.note
        if (prevSegmentNumber == startingSegmentNumber) {
            return
        }
        if (note =~ filter) {
            jump = true
            editor.gotoEntry(prevSegmentNumber)
            return
        } else {
            jump = false
            currentSegmentNumber = prevSegmentNumber
        }
    }
}
return
