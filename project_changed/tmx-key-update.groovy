/**
 * Usage : Put this script in <ScriptsDir>/project_changed folder. Create a folder if it doesn't exists.
 *
 * @authors 	Manuel Souto Pico
 * @version 	0.1.0
 * @date 		2024.08.09
 */

import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*

/*
// check that this is PISA
in omegat/project_name.txt: pisa_2025ms_translation... 
prop = project.getProjectProperties()
proj_name = prop.projectName
// The container is the first entity in the project name (before the first underscore)
container = (proj_name =~ /^[^_]+/)[0]

/* == rather than contains? */
/*
if (container.contains("pisa")) { 
  console.println("This script runs on PISA XLIFF files, let's continue!")
} else {
  console.println("This is not PISA, let's stop here. Good bye!")
  return
}
*/

// prepare
String dir
def replacePair

/*def skipTraverse(eventType) {
    if (!eventType.metaClass.hasProperty(eventType, 'skipTraverse')) {
        eventType.metaClass.skipTraverse = false
    }
    eventType.skipTraverse
}*/

switch (eventType) {
    case LOAD:
        // Skip traverse
/*        if (skipTraverse(LOAD)) {
			LOAD.skipTraverse = false // reset the flag
			return
        }*/

        dir = project.projectProperties.TMRoot
        replacePair = [
            [find: /(?<=<prop type="id">)foo(?=<\/prop>)/, replacement: /bar/],
        ]
		break
	case COMPILE:
		dir = project.projectProperties.targetRoot
        replacePair = []
        break
    default:
        return null // No output
}

String ENCODING = 'UTF-8'
File rootDir = new File(dir)
int modifiedFiles = 0

// options as map
def options = [
    type       : groovy.io.FileType.FILES,
    nameFilter : ~/.*PISA_2025FT_.+\.tmx$/
]

// replacer as closure
def replacer = {file ->
    String text = file.getText ENCODING
    String replaced = text
    replacePair.each {replaced = replaced.replaceAll it.find, it.replacement}
    if (text != replaced) {
        file.setText replaced, ENCODING
        console.println "modified: $file"
        modifiedFiles++
    }
}

/*def reloadProjectOnetime = {
    LOAD.skipTraverse = true    // avoid potentially infinity reloading loop
    javax.swing.SwingUtilities.invokeLater({
        org.omegat.gui.main.ProjectUICommands.projectReload()
    } as Runnable)
}
*/
// do replace
rootDir.traverse options, replacer

/*if (modifiedFiles > 0 && eventType == LOAD) {
    console.println "$modifiedFiles file(s) modified."
    reloadProjectOnetime()
}
*/
