/* :name =   Check OmegaT version :description=
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-02-12
 * @version: 0.2.3
 * @changed: Manuel 2024-02-20 -- fixed matching regex, changed order of actions (close, then prompt)
 * @changed: Manuel 2024-04.24 -- remove open project / project name and event type checks to run in any case
 * @changed: Manuel 2024-06-27 -- add a list of allowed revisions (rather than unique version)
 * @changed: Kos    2025-01014 -- close the project only after the user has been informed
 */

import java.awt.Desktop
import org.omegat.util.OStrings

import static javax.swing.JOptionPane.*
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import static org.omegat.gui.main.ProjectUICommands.*
import static org.omegat.util.Platform.*

reqVersion = "5.7.3"
// reqRevision = "a978d82ee"
allowedRevisions = ["a978d82ee", "57b1bb571", "e363cb094"] // 57b1bb571 was used for Windows, e363cb094 was used for Mac
winURL="https://cat.capstan.be/OmegaT/exe/OmegaT_${reqVersion}_Windows_64_Signed.exe"
macURL="https://cat.capstan.be/OmegaT/exe/OmegaT_${reqVersion}_Mac.zip"

if (eventType == LOAD) {
    title = "Check OmegaT version"
    openURL = false
    closeProject = false

    if (!(allowedRevisions.contains(OStrings.REVISION))) {
        
        // close the project, first of all
        //org.omegat.util.gui.UIThreadsUtil.executeInSwingThread { projectClose() }

        // inform the user 
        msg="OmegaT ${reqVersion} (built by cApStAn) is required."
        console.println("== ${title} ==")
        console.println(msg)
        showMessageDialog null, msg, title, INFORMATION_MESSAGE
        openURL = true
        // close the project
        projectClose()
    } else {
        console.println("== ${title} ==")
        console.println("OmegaT version ${OStrings.VERSION} (${OStrings.REVISION})")
    }
    
    if (openURL) {
        os = System.getProperty("os.name").toLowerCase()
        switch (os) {
            case ~/(?i).*win.*/:
                url = winURL
                break
            case ~/(?i).*mac.*/:
                url = macURL
                break
            default:
                openURL = false
        }
    }
    if (openURL) {
        title = "Download OmegaT"
        msg = "Do you want to download the installation file?"
        userChoice = showConfirmDialog(null, msg, title, OK_CANCEL_OPTION)
        if (userChoice == OK_OPTION) {
            console.println("Opening the download link...")
            Desktop.getDesktop().browse(new URI(url))
        } else {
            return
        }
    }
}
