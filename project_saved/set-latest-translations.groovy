/* :name = Set Latest Translations :description=
 *
 * @author	Thomas CORDONNIER
 * @creation	2023.11.07
 * @version	0.1.1
*/


// path to the folder inside the TM folder
path_to_tmx_dir = "auto/"



def gui() {

	def props = project.projectProperties
	if (!props) {
		final def msg   = 'No project opened.'
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
		return
	}

	project.transMemories.each { name, tmx -> 
		name = name.substring(props.getTMRoot().length())
		if (name.startsWith(path_to_tmx_dir)) {
			console.println("Importing from " + name);
			tmx.entries.each { entry ->
				// Search which entry in the source files corresponds to the one in the tmx
				// Note: to be improved, for the moment it works only with default entries
				// and it is not optimized, we should use a cache as in ImportFromAutoTMX
				def inSourceFilesEntry = null
				project.allEntries.each { pe -> 
					if (pe.srcText.equals(entry.source)) inSourceFilesEntry = pe;
				}
				// Now search is done, if we found something we use it
				if (inSourceFilesEntry == null) return; // not found => next tmx.entry
				if (! entry.source.equals(inSourceFilesEntry.srcText)) throw new Exception("Something got wrong: " + entry.source + " found " + inSourceFilesEntry.source);
				// Still here? We have found an entry in source files, now search in project_save.tmx
				def inProjectSaveEntry = project.getTranslationInfo(inSourceFilesEntry)
				if (inProjectSaveEntry == null) return;	// entry is in source but not translated
				if (! entry.source.equals(inProjectSaveEntry.source)) return;	// same but OmegaT returns a dummy entry
				if (entry.translation.equals(inProjectSaveEntry.translation)) return;	// in this case you don't want to update
				// Still here? Now let's check the date
				long inProjectDate = inProjectSaveEntry.creationDate
				if (inProjectSaveEntry.changeDate > inProjectSaveEntry.creationDate) inProjectDate = inProjectSaveEntry.changeDate
				long inTmxDate = entry.creationDate
				if (entry.changeDate > entry.creationDate) inTmxDate = entry.changeDate
				console.println(entry.source + " " + inTmxDate + " / " + inProjectDate + " => " + (inTmxDate > inProjectDate));
				if (inTmxDate > inProjectDate) {
					project.setTranslation(inSourceFilesEntry, entry, true, org.omegat.core.data.TMXEntry.ExternalLinked.xAUTO);
				}
			}
		}
	}
	project.saveProject(false);
	org.omegat.gui.main.ProjectUICommands.projectReload() 

}

return
