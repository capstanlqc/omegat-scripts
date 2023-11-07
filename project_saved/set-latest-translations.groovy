/* :name = Set Latest Translations :description=
 *
 * @author	Manuel Souto Pico
 * @creation	2023.11.06
 * @version	0.0.1
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
				// Search which entry in the project corresponds to the one in the tmx
				// Note: to be improved, for the moment it works only with default entries
				// and it is not optimized, we should use a cache as in ImportFromAutoTMX
				def inProject = null
				project.allEntries.each { pe -> 
					if (pe.srcText.equals(entry.source)) inProject = pe;
				}
				// Now search is done, if we found something we use it
				if ((inProject != null) && (entry.source.equals(inProject.srcText))) {
					def inProjectEntry = project.getTranslationInfo(inProject)
					if ((inProjectEntry != null) && (entry.source.equals(inProjectEntry.source))) {
						long inProjectDate = inProjectEntry.creationDate
						if (inProjectEntry.changeDate > inProjectEntry.creationDate) inProjectDate = inProjectEntry.changeDate
						long inTmxDate = entry.creationDate
						if (entry.changeDate > entry.creationDate) inTmxDate = entry.changeDate
						console.println(entry.source + " " + inTmxDate + " / " + inProjectDate + " => " + (inTmxDate > inProjectDate));
						if (inTmxDate > inProjectDate) {
							project.setTranslation(inProject, entry, true, null);// org.omegat.core.data.TMXEntry.ExternalLinked.xAUTO);
						}
					}
				}
			}
		}
	}
	project.saveProject(false);
	org.omegat.gui.main.ProjectUICommands.projectReload() 

}

return