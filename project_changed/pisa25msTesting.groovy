/**
 * Usage : Put this script in <ScriptsDir>/project_changed folder. Create a folder if it doesn't exists.
 * Logic : Renames target files replacing the real target language tag with the fictitious tag in the repo name.
 *
 * @authors 	Manuel Souto Pico
 * @version 	0.2.0
 * @date 		2024.07.09
 */

/*
 changes:
 0.2.0 (2024.07.09)	delete file if it exists before renaming
*/

import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE.*
import org.omegat.util.FileUtil;

if (eventType != COMPILE) {
	return
}

prop = project.projectProperties

omtDir = prop.getProjectRoot() + "omegat"
projectNameFile = new File(omtDir + File.separator + "project_name.txt")
if (!projectNameFile.exists()) {
	console.println("ERROR: File project_name.txt not found")
	return
}

projectName = projectNameFile.text ? projectNameFile.readLines().get(0) : projectName
component = projectName.contains("qq") ? "qq" : "cog"

targetDir = new File(prop.getTargetRoot())
// List of current local files
List<String> targetFiles = FileUtil.buildRelativeFilesList(targetDir, null, ['zz.txt']); /* includes, excludes */
// potential approach (not used in the end: iterate through the list of target files)

// prepare
String dir
def replacePair

switch (eventType) {
	case COMPILE:
		// dir = targetDir // new File(prop.getTargetRoot())
		dir = project.projectProperties.targetRoot
		// restores ETS language codes
		replacePair = [
            // [find: /(<(i) class="[^"]*fas fa[^"]+")\/>/, replacement: /$1><\/$2>/],
            [find: "ar-JO", replacement: ["cog": "ar-YY", "qq": "ar-YY"]],
			[find: "ar-MA", replacement: ["cog": "de-XX", "qq": "ar-YY"]],
			[find: "az-QZ", replacement: ["cog": "en-XX", "qq": "en-XX"]],
			[find: "de-DE", replacement: ["cog": "de-XX", "qq": "de-XX"]],
			[find: "en-CY", replacement: ["cog": "en-YY", "qq": "en-YY"]],
			[find: "en-ZM", replacement: ["cog": "en-XX", "qq": "en-XX"]],
			[find: "ru-MD", replacement: ["cog": "ru-XX", "qq": "ru-XX"]],
			[find: "zh-HK", replacement: ["cog": "zh-XX", "qq": "zh-XX"]],
			[find: "zh-TW", replacement: ["cog": "zh-XX", "qq": "zh-XX"]],
			[find: "it-ZZ", replacement: ["cog": "it-IT", "qq": "it-IT"]],
        ]
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
    nameFilter : ~/.*PISA_2025.*\.(xml|html)$/
]

// replacer as closure
def replacer = {file ->
    // def fixedFilePath = file.getAbsolutePath()
    // def filename = file.getName()
	def parentDirPath = file.getParentFile().getAbsolutePath()
	def fixedFilename = file.getName()

    /*
    String text = file.getText ENCODING
    String replaced = text
    */
    replacePair.each {fixedFilename = fixedFilename.replace it.find, it.replacement[component]}
    fixedFilePath = parentDirPath + File.separator + fixedFilename

    if (file.toString() != fixedFilePath.toString()) {
    	console.println("Rename ${file.getName()} as ${fixedFilename}")
    	fixedFile = new File(fixedFilePath.toString())
    	if (fixedFile.exists()) {
    		// console.println("########### FILE ${fixedFile} EXISTS, WILL BE DELETED ###########")
    		fixedFile.delete()
    	}
    	file.renameTo(fixedFilePath)
    	modifiedFiles++
    } 
}

// do replace
rootDir.traverse options, replacer

if (modifiedFiles > 0 && eventType == LOAD) {
    console.println("$modifiedFiles file(s) modified.")
}

