/* :name = Set Latest Translations :description=
 *
 * @author	Thomas CORDONNIER
 * @creation	2023.11.09
 * @version	0.2.0
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
				def isTmxEntryDefault = (entry.otherProperties == null) || (entry.otherProperties.size() == 0)
				if (! isTmxEntryDefault) {
					isTmxEntryDefault = true
					[ "file", "id", "prev", "next", "path" ].each { 
						if (entry.getPropValue(it) != null) isTmxEntryDefault = false 
					}
				}				
				
				// Search which entry in the source files corresponds to the one in the tmx
				// Note: to be improved, we should use a cache as in ImportFromAutoTMX
				def inSourceFilesEntry = null
				def inProjectSaveEntry = null
				def foundSource = false
				if (isTmxEntryDefault) {
					// Search for an entry which is either untranslated or default translation
					project.allEntries.each { pe -> 
						if (pe.srcText.equals(entry.source)) {
							foundSource = true
							inProjectSaveEntry = project.getTranslationInfo(pe)
							if ((inProjectSaveEntry == null) || (inProjectSaveEntry.defaultTranslation) || (inProjectSaveEntry.translation == null)) { 
								inSourceFilesEntry = pe; return; 
							}
						}
					}
				} else {
					// Search for an entry which has the given key
					project.allEntries.each { pe -> 
						if (pe.srcText.equals(entry.source)) {
							foundSource = true
							if (pe.key.equals(new org.omegat.core.data.EntryKey(
								entry.getPropValue("file"),
								entry.source,
								entry.getPropValue("id"),
								entry.getPropValue("prev"),
								entry.getPropValue("next"),
								entry.getPropValue("path")))) {
									inSourceFilesEntry = pe; inProjectSaveEntry = project.getTranslationInfo(pe); return;
							}
						}
					}
				}
				if (! foundSource) 
					return;
				else if (inSourceFilesEntry == null) 
					// use a dummy entry for setTranslation
					inSourceFilesEntry = new org.omegat.core.data.SourceTextEntry(
						new org.omegat.core.data.EntryKey(
							entry.otherProperties.getPropValue("file"),
							entry.source,
							entry.otherProperties.getPropValue("id"),
							entry.otherProperties.getPropValue("prev"),
							entry.otherProperties.getPropValue("next"),
							entry.otherProperties.getPropValue("path")),
						-1, null, null, null, false);
				// Now search is done, if we found something we use it
				if (inSourceFilesEntry == null) return; // not found => next tmx.entry
				if (! entry.source.equals(inSourceFilesEntry.srcText)) throw new Exception("Something got wrong: " + entry.source + " found " + inSourceFilesEntry.source);
				// Still here? We have found an entry in source files, now search in project_save.tmx
				inProjectSaveEntry = project.getTranslationInfo(inSourceFilesEntry);								
				if (entry.translation.equals(inProjectSaveEntry.translation)) return;	// translation already identical : in this case you don't want to update
				// Still here? Now let's check the date
				long inProjectDate = inProjectSaveEntry.creationDate
				if (inProjectSaveEntry.changeDate > inProjectSaveEntry.creationDate) inProjectDate = inProjectSaveEntry.changeDate
				long inTmxDate = entry.creationDate
				if (entry.changeDate > entry.creationDate) inTmxDate = entry.changeDate
				console.println(entry.source + " " + inTmxDate + " / " + inProjectDate + " => " + (inTmxDate > inProjectDate));
				if (inTmxDate > inProjectDate) {
					project.setTranslation(inSourceFilesEntry, entry, isTmxEntryDefault, org.omegat.core.data.TMXEntry.ExternalLinked.xAUTO);
				}
			}
		}
	}
	project.saveProject(false);
	org.omegat.gui.main.ProjectUICommands.projectReload() 

}

return
