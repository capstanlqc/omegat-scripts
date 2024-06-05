/* :name = Merge Creds Files :description=
 *
 * @author      Manuel Souto Pico
 * @date        2024-06-05
 * @version     0.0.1
 */

import org.omegat.util.StaticUtils
import groovy.io.FileType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

// define constants
configDir = StaticUtils.getConfigDir()
credsDir = new File(configDir + File.separator + "creds" + File.separator + "foo")
credsFile = new File(configDir + File.separator + "creds" + File.separator + "repositories.properties")

// create timestamps
def date = new Date()
def bakFormat = new SimpleDateFormat("yyyyMMdd.HHmmss")
def bakFormattedDate = bakFormat.format(date)
def headerFormat = new SimpleDateFormat("'#'EEE MMM dd HH:mm:ss z yyyy")
headerFormat.setTimeZone(TimeZone.getTimeZone("CET"))
def formattedDate = headerFormat.format(date)

// create or backup creds file
if (credsFile.exists()) {
	// make backup
	def backupFilePath = credsFile.getAbsolutePath() + '.' + bakFormattedDate + '.bak'
	def backupFile = new File(backupFilePath)
	backupFile.text = credsFile.text
} else {
	console.println("Create the file if it doesn't exist.")
	credsFile.createNewFile() // empty
	// credsFile.text = 'formattedDate'
	// credsFile.write(formattedDate)
}

// collect creds project files
def credProjFiles = []
credsDir.eachFile (FileType.FILES) { file ->
	// check that proj suffix belongs to a validated list? ["pisa", "ysc", "etc"]
	if (!file.endsWith(".done")) {
		credProjFiles << file
	}
}

credProjFiles.each{ file ->

	// merge all proj creds
	credsFile.append(file.getText()) 

	// disable the process proj creds files
	def doneFilePath = file.getAbsolutePath() + '.done'
	def doneFile = new File(doneFilePath)
	file.renameTo(doneFile)

}

// todo: dedupe lines? (duplicate lines seem harmless, though)

