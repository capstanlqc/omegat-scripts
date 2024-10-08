# omegat-scripts
Groovy scripts that can run in OmegaT


## `utils_report_auto.groovy` 
<details>
  Reports how many segments are populated from <code>tm/auto</code> and <code>tm/enforce</code>. Data is output to the console, can be automatically copied as tab separated values to the clipboard, and exported to a tsv file inside the current project. 
</details>

## `utils_import_creds.groovy`
<details>
  This scripts adds credentials data from a plain text file to <code>credential.properties</code> in OmegaT config folder. The user selects the file via a file chooser dialog. Once the selected file is imported, its extension changes to <code>.done</code> and such processed file cannot be used again.
  
  The script also checks if the selected file is a binary file, and if it actually contains the expected credentials data. In case a wrong file is selected, the file chooser dialog appears again.  
  To simplify the check for the above conditions, selecting only one file at a time is possible.
</details>

## `goto_next_filtered_note.groovy` and `goto_prev_filtered_note.groovy`
<details>
  The scripts will activate the next/previous segment where the note contains text either defined in the file <code>&lt;project_folder&gt;/filtered_note.txt</code>, or, if the file is not found, in the scripts themselves (currently <code>XYZZZ</code>).
</details>

## `goto_next_ta_note.groovy`
<details>
  The script will activate the next segment which has a match in the first TMX file found in <code>&lt;project_folder&gt;/notes</code>. Works even without the plugin that shows T&A notes.
</details>
