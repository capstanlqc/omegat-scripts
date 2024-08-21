/* :name = Write PISA batch TM :description = 
 *
 *  @author:    Manuel Souto Pico
 *  @version:   0.1
 *  @creation:  2024.08.20
 */

import java.nio.file.Files
import java.nio.file.Path
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

// stop if event is not load
if (eventType != LOAD) {
    // console.println("This script only runs when the project is loading.")
    return
}

// get project properties / internals
props = project.projectProperties
projName = props.projectName
projectRoot = props.projectRoot
sourceRoot = props.sourceRoot
sourcePath = Path.of(sourceRoot)
targetRoot = props.targetRoot
targetPath = Path.of(targetRoot)


// abort if a team project is not opened
if (!props) {
    final def title = "Create my TMs"
    final def msg   = "No project opened or not a team project."
    echo("== ${title} ==")
    echo(msg)
    // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
    return
}

// abort if the team project is not a pisa project
// if (!projName.startsWith("pisa_2025")) {
//     final def title = "Write PISA batch TM"
//     final def msg   = "This is not a PISA25 project."
//     echo("== ${title} ==")
//     echo(msg)
//     // showMessageDialog(null, msg, title, INFORMATION_MESSAGE)
//     return
// }

echo("Copying batch TMX")

tmRoot = props.TMRoot // dir = project.projectProperties.TMRoot

def mineTmxDir = new File(props.getTMRoot(), "mine")
def tasksDir = new File(targetRoot, "tasks")


// Check if the source directory exists and is a directory
if (tasksDir.exists() && tasksDir.isDirectory()) {
    // Ensure the destination directory exists, create it if it doesn't
    if (!mineTmxDir.exists()) {
        mineTmxDir.mkdirs()
    }

    // Iterate over each file in the source directory
    tasksDir.eachFile { file ->

        // continue to next iteration if file corresponds to an existing batch?
    
        if (file.isFile()) { // Only process files, not subdirectories
            def destFile = new File(mineTmxDir, file.name) // Create a File object for the destination file

            // Copy the file
            file.withInputStream { input ->
                destFile.withOutputStream { output ->
                    output << input
                }
            }
            console.println("Copied ${file.name} to ${mineTmxDir}")
        }
    }
    console.println("All files copied successfully.")
} else {
    console.println("Source directory does not exist or is not a directory.")
}

return
