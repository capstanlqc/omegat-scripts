/* :name = Copy Source :description=
 *
 * @author      Manuel Souto Pico
 * @date        2020-03-03
 * @version     0.0.1
 */
 
 import org.omegat.core.data.PrepareTMXEntry
 import java.util.ArrayList
 
def gui(){
	def segList = new ArrayList<Integer>();

	project.allEntries.each { currSegm ->
		def source = currSegm.getSrcText();
		def target = project.getTranslationInfo(currSegm) ? project.getTranslationInfo(currSegm).translation : null;
          if (target == null) {
          	segList.add(currSegm.entryNum());
          	if (currSegm.entryNum() == editor.getCurrentEntryNumber()) {
				// avoid conflict between current editor contents and new entry
				editor.replaceEditText(source)
				editor.commitAndDeactivate(); editor.activateEntry();
          	} else {
				// update without using the editor, it is faster
				PrepareTMXEntry addEntry = new PrepareTMXEntry()
				addEntry.source = source; addEntry.translation = source
				project.setTranslation(currSegm, addEntry, true, null);
			}
          }
	}
	editor.refreshViewAfterFix(segList);
	console.println("Source text copied to the target segment in " + segList.size() + " segments.")
	editor.gotoEntry(savedEntry);
}
