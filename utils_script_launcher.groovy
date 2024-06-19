/*:name = Utils - Script Launcher :description =
 * 
 */
import org.omegat.util.Preferences

prop = project.projectProperties
scriptName = "Script Launcher"
if (!prop) {
    console.println("${scriptName}\n${"="*scriptName.size()}\nNo project open")
    return
}
def executeScript(String scriptPath) {
    scriptFile = new File(scriptPath)
    if (scriptFile.exists()) {
        evaluate(scriptFile)
    } else {
        console.println("Script file not found: ${scriptPath}")
    }
}

projectRoot = prop.projectRoot
scriptFolder = Preferences.getPreference(Preferences.SCRIPTS_DIRECTORY)
console.println(scriptFolder)
scriptPathsFile = new File(projectRoot + "script.paths")
if (! scriptPathsFile.exists()) {
    console.println("${scriptName}\n${"="*scriptName.size()}\n${scriptPathsFile} doesn't exist")
} else {
	scriptPathsFile.eachLine { line ->
	    scriptToRun = scriptFolder.toString() + File.separator + line
	    if (new File(scriptToRun).exists()) {
	        executeScript(scriptToRun)
	    }
	}
}
