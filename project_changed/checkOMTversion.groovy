/* :name =   Check OmegaT version :description=
 * 
 * @author:  Kos Ivantsov
 * @date:    2024-02-12
 * @version: 0.2.5
 * @changed: Manuel 2024-02-20 -- fixed matching regex, changed order of actions (close, then prompt)
 * @changed: Manuel 2024-04.24 -- remove open project / project name and event type checks to run in any case
 * @changed: Manuel 2024-06-27 -- add a list of allowed revisions (rather than unique version)
 * @changed: Kos    2025-01-14 -- close the project only after the user has been informed
 * @changed: Kos    2025-02-12 -- place the information message in the center of the active screen
 * @changed: Kos    2025-05-29 -- disable running on non-cApStAn builds of OmegaT
 */

import java.awt.Desktop
import java.awt.Dimension
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Window
import javax.swing.FocusManager
import javax.swing.JFrame
import org.omegat.util.OStrings
import org.omegat.util.Preferences

import static javax.swing.JOptionPane.*
import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import static org.omegat.gui.main.ProjectUICommands.*
import static org.omegat.util.Platform.*

omtVendor = Preferences.getPreference("omegat_vendor")
if ((!omtVendor) || (omtVendor != "cApStAn")) {
    message = "cApStAn customization cannot be installed for this version of OmegaT"
    console.print(message)
    return
}

reqVersion = "5.7.3"
// 57b1bb571 was used for Windows 5.7.3, e363cb094 was used for Mac 5.7.3
// 911305e31 is for 5.7.4_0_Beta (both Windows and Mac) -- internal version for testing
allowedRevisions = ["57b1bb571", "e363cb094", "911305e31"]

winURL="https://cat.capstan.be/OmegaT/exe/OmegaT_${reqVersion}_Windows_64_Signed.exe"
macURL="https://cat.capstan.be/OmegaT/exe/OmegaT_${reqVersion}_Mac.zip"

if (eventType == LOAD) {
    title = "Check OmegaT version"
    openURL = false
    closeProject = false

    if (!(allowedRevisions.contains(OStrings.REVISION))) {

        // Get the active window
        Window activeWindow = FocusManager.getCurrentManager().getActiveWindow()
    
        if (activeWindow == null) {
            println "No active window found!"
        }
        
        // Get the bounds of the active window
        Rectangle windowBounds = activeWindow.bounds
    
        // Get all screen devices
        def ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        def screens = ge.getScreenDevices()
    
        // Find the screen containing the largest part of the window
        GraphicsDevice activeScreen = null
        Rectangle activeBounds = null
        int maxIntersectionArea = 0
    
        screens.each { screen ->
            Rectangle screenBounds = screen.defaultConfiguration.bounds
            Rectangle intersection = windowBounds.intersection(screenBounds)
    
            int intersectionArea = intersection.width * intersection.height
            if (intersectionArea > maxIntersectionArea) {
                maxIntersectionArea = intersectionArea
                activeScreen = screen
                activeBounds = screenBounds
            }
        }
    
        if (activeScreen) {
            console.println "Active Screen: ${activeScreen}"
            console.println "Active Screen Bounds: ${activeBounds}"
        } else {
            console.println "No active screen found for the active window."
        }
    
        // Define the rectangle
        def rectangle = activeBounds
        // Calculate the center point of the rectangle
        int centerX = rectangle.x + (rectangle.width / 2)
        int centerY = rectangle.y + (rectangle.height / 2)
    
        // Create a dummy JFrame as the parent for the dialog
        def parentFrame = new JFrame()
        parentFrame.setUndecorated(true) // Make the frame invisible
        parentFrame.setSize(1, 1)        // Minimal size to make it functional
        parentFrame.setLocation(centerX, centerY)
        parentFrame.setVisible(true)

        // inform the user 
        msg="OmegaT ${reqVersion} (built by cApStAn) is required."
        console.println("== ${title} ==")
        console.println(msg)
        showMessageDialog parentFrame, msg, title, INFORMATION_MESSAGE
        parentFrame.dispose()
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
